package com.acornui.core.input.interaction

import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.Stage
import com.acornui.component.UiComponentRo
import com.acornui.core.Disposable
import com.acornui.core.ancestry
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.input.*
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.timer


/**
 * Watches the stage for mouse and touch events that constitute a 'click', then dispatches the click event on the
 * interactivity manager.
 *
 * This will respond to mouse and touch, but not both.
 */
abstract class ClickDispatcher(
		override val injector: Injector
) : Scoped, Disposable {

	protected val stage = inject(Stage)
	private val timeDriver = inject(TimeDriver)
	private val interactivityManager = inject(InteractivityManager)

	var multiClickSpeed: Int = 400

	private val downButtons: Array<MutableList<UiComponentRo>?> = Array(6, { null }) // TODO: Kotlin bug: Enum.values.size not working.
	private val clickEvent = ClickInteraction()

	private var lastTarget: UiComponentRo? = null
	private var currentCount = 0
	private var previousTimestamp = 0L

	private var touchMode = false

	private var pendingClick = false

	private val rootMouseDownHandler = {
		event: MouseInteraction ->
		if (!touchMode) {
			val downElement = stage.getChildUnderPoint(event.canvasX, event.canvasY, onlyInteractive = true)
			if (downElement != null) {
				setDownElement(downElement, event.button.ordinal)
			}
		}
	}

	private val rootTouchStartHandler = {
		event: TouchInteraction ->
		val first = event.changedTouches.first()
		val downElement = stage.getChildUnderPoint(first.canvasX, first.canvasY, onlyInteractive = true)
		if (downElement != null) {
			setDownElement(downElement, WhichButton.LEFT.ordinal)
		}
	}

	private fun setDownElement(downElement: UiComponentRo, ordinal: Int) {
		if (lastTarget != downElement) {
			lastTarget = downElement
			currentCount = 1
		}
		@Suppress("UNCHECKED_CAST")
		val ancestry = arrayListObtain<UiComponentRo>()
		downElement.ancestry(ancestry)
		downButtons[ordinal] = ancestry
	}

	private var preventMouseTimer: Disposable? = null

	private val rootTouchEndHandler =  {
		event: TouchInteraction ->
		val first = event.changedTouches.first()
		release(WhichButton.LEFT, first.canvasX, first.canvasY, event.timestamp)
		touchMode = true
		preventMouseTimer?.dispose()
		preventMouseTimer = timer(timeDriver, 2.5f, 1) {
			touchMode = false
			preventMouseTimer = null
		}
	}

	private val rootTouchCancelHandler = {
		event: TouchInteraction ->
		val downElements = downButtons[WhichButton.LEFT.ordinal]
		if (downElements != null) {
			downButtons[WhichButton.LEFT.ordinal] = null
			arrayListPool.free(downElements)
		}
	}

	private val rootMouseUpHandler = {
		event: MouseInteraction ->
		if (!touchMode && !event.isFabricated) {
			release(event.button, event.canvasX, event.canvasY, event.timestamp)
		}
	}

	fun release(button: WhichButton, canvasX: Float, canvasY: Float, timestamp: Long) {
		val downElements = downButtons[button.ordinal]
		if (downElements != null) {
			downButtons[button.ordinal] = null
			for (i in 0..downElements.lastIndex) {
				val downElement = downElements[i]
				if (downElement.containsCanvasPoint(canvasX, canvasY)) {
					if (timestamp - previousTimestamp <= multiClickSpeed) {
						currentCount++
					} else {
						// Not fast enough to be considered a multi-click
						currentCount = 1
					}
					previousTimestamp = timestamp
					clickEvent.clear()
					clickEvent.type = getClickType(button)
					clickEvent.target = downElement
					clickEvent.button = button
					clickEvent.timestamp = timestamp
					clickEvent.count = currentCount
					clickEvent.canvasX = canvasX
					clickEvent.canvasY = canvasY
					pendingClick = true
					break
				}
			}
			arrayListPool.free(downElements)
		}
	}

	private fun getClickType(button: WhichButton): InteractionType<ClickInteraction> {
		return when (button) {
			WhichButton.LEFT -> ClickInteraction.LEFT_CLICK
			WhichButton.RIGHT -> ClickInteraction.RIGHT_CLICK
			WhichButton.MIDDLE -> ClickInteraction.MIDDLE_CLICK
			WhichButton.BACK -> ClickInteraction.BACK_CLICK
			WhichButton.FORWARD -> ClickInteraction.FORWARD_CLICK
			else -> throw Exception("Unknown click type.")
		}
	}

	init {
		stage.mouseDown(isCapture = true).add(rootMouseDownHandler)
		stage.touchStart(isCapture = true).add(rootTouchStartHandler)
		stage.mouseUp().add(rootMouseUpHandler)
		stage.touchEnd().add(rootTouchEndHandler)
		stage.touchCancel(isCapture = true).add(rootTouchCancelHandler)
	}

	protected val fireHandler = {
		_: Any ->
		if (pendingClick) {
			pendingClick = false
			interactivityManager.dispatch(clickEvent.target!!, clickEvent)
		}
	}

	override fun dispose() {
		stage.mouseDown(isCapture = true).remove(rootMouseDownHandler)
		stage.touchStart(isCapture = true).remove(rootTouchStartHandler)
		stage.mouseUp().remove(rootMouseUpHandler)
		stage.touchEnd().remove(rootTouchEndHandler)
		stage.touchCancel(isCapture = true).remove(rootTouchCancelHandler)
		for (i in 0..downButtons.lastIndex) {
			val list = downButtons[i]
			if (list != null) {
				list.clear()
				arrayListPool.free(list)
			}
		}
	}
}

class JvmClickDispatcher(injector: Injector) : ClickDispatcher(injector) {

	init {
		stage.mouseUp().add(fireHandler)
		stage.touchEnd().add(fireHandler)
	}

	override fun dispose() {
		super.dispose()
		stage.mouseUp().remove(fireHandler)
		stage.touchEnd().remove(fireHandler)
	}
}