/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.component

import com.acornui.collection.ActiveList
import com.acornui.component.layout.algorithm.CanvasLayoutData
import com.acornui.component.layout.algorithm.canvasLayoutData
import com.acornui.component.layout.setSize
import com.acornui.component.style.StyleTag
import com.acornui.component.text.text
import com.acornui.AppConfig
import com.acornui.Disposable
import com.acornui.di.*
import com.acornui.input.interaction.MouseInteractionRo
import com.acornui.input.interaction.rollOut
import com.acornui.input.interaction.rollOver
import com.acornui.input.mouseDown
import com.acornui.input.touchStart
import com.acornui.popup.PopUpInfo
import com.acornui.popup.PopUpManager
import com.acornui.time.tick
import com.acornui.function.as1
import com.acornui.math.Bounds
import com.acornui.math.Easing
import com.acornui.math.Vector2
import com.acornui.signal.bind

class TooltipAttachment(val target: UiComponentRo) : Disposable {

	private var isOver: Boolean = false
	private val tooltipManager = target.inject(TooltipManager)
	private var timerHandle: Disposable? = null
	private var showCountdown = showDelay
	private var hideCountdown = hideDelay
	private val dT = target.inject(AppConfig).frameTime

	init {
		target.rollOver(isCapture = true).add(::targetRollOverHandler)
		target.rollOut(isCapture = true).add(::targetRollOutHandler)
		target.mouseDown(isCapture = true).add(::resetShowCountdown.as1)
		target.touchStart(isCapture = true).add(::resetShowCountdown.as1)
	}

	private fun targetRollOverHandler(event: MouseInteractionRo) {
		hideCountdown = hideDelay
		isOver = true
		if (timerHandle == null) {
			timerHandle = target.tick {
				if (isOver) {
					showCountdown -= dT
					if (showCountdown <= 0f) {
						if (activeTip == null)
							activeTip = tooltip
						activeTip?.view?.alpha = minOf(1f, fadeInEasing.apply(-showCountdown / fadeInDuration))
					}
				} else {
					hideCountdown -= dT
					if (hideCountdown <= 0f) {
						activeTip?.view?.alpha = maxOf(0f, 1f - fadeOutEasing.apply(-hideCountdown / fadeOutDuration))
						if (hideCountdown <= -fadeOutDuration) {
							activeTip = null
							timerHandle?.dispose()
							timerHandle = null
							showCountdown = showDelay
						}
					}
				}
			}
		}
	}

	private fun targetRollOutHandler(event: MouseInteractionRo) {
		isOver = false
	}

	private fun resetShowCountdown() {
		showCountdown = showDelay
	}

	private var _activeTip: Tooltip<*>? = null
	private var activeTip: Tooltip<*>?
		get() = _activeTip
		set(value) {
			val oldTip = _activeTip
			if (oldTip != null)
				tooltipManager.tooltips.remove(oldTip)
			_activeTip = value
			if (value != null)
				tooltipManager.tooltips.add(value)
		}

	var tooltip: Tooltip<*>? = null
		set(value) {
			val oldValue = field
			if (oldValue != value) {
				field = value
				if (activeTip != null)
					activeTip = value
			}
		}

	override fun dispose() {
		target.rollOver(isCapture = true).remove(::targetRollOverHandler)
		target.rollOut(isCapture = true).remove(::targetRollOutHandler)
		target.mouseDown(isCapture = true).remove(::resetShowCountdown.as1)
		target.touchStart(isCapture = true).remove(::resetShowCountdown.as1)

		activeTip = null
		timerHandle?.dispose()
		timerHandle = null
	}

	companion object {
		var showDelay = 1f
		var hideDelay = 0.5f
		var fadeInDuration = 0.2f
		var fadeOutDuration = 0.3f
		var fadeInEasing = Easing.pow2In
		var fadeOutEasing = Easing.pow2Out
	}

}

interface TooltipManager {

	val tooltips: MutableList<Tooltip<*>>

	fun createTooltip(value: String): Tooltip<String>

	companion object : DKey<TooltipManager> {
		override fun factory(injector: Injector): TooltipManager? {
			return TooltipManagerImpl(injector)
		}
	}
}

class TooltipManagerImpl(override val injector: Injector) : TooltipManager, Scoped, Disposable {

	private val _tooltips = ActiveList<Tooltip<*>>()
	override val tooltips: MutableList<Tooltip<*>> = _tooltips

	private val toolTipLayoutData: CanvasLayoutData = canvasLayoutData()

	private var enterFrameHandle: Disposable? = null
	private val popUpManager = inject(PopUpManager)

	private val defaultTooltipView: ItemRenderer<String> = TooltipView(stage)

	init {
		_tooltips.bind {
			@Suppress("UNCHECKED_CAST")
			activeTooltip = _tooltips.lastOrNull() as Tooltip<Any?>?
		}
	}

	override fun createTooltip(value: String): Tooltip<String> = Tooltip(value, defaultTooltipView)

	private var activeTooltip: Tooltip<Any?>? = null
		set(value) {
			if (field != value) {
				val previousView = field?.view
				field = value
				val newView = value?.view
				newView?.data = value?.data
				if (previousView != newView) {
					if (previousView != null && !previousView.isDisposed) {
						popUpManager.removePopUp(previousView)
					}
					if (newView != null && !newView.isDisposed) {
						popUpManager.addPopUp(PopUpInfo(newView, isModal = false, focus = false, dispose = false, priority = value.priority, layoutData = toolTipLayoutData))
					}
				}
				followMouse = value != null
			}
		}

	private var followMouse: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				if (value) {
					enterFrameHandle = tick(callback = ::frameHandler)
				} else {
					enterFrameHandle?.dispose()
					enterFrameHandle = null
				}
			}
		}

	private val mousePosition = Vector2()
	private val cursorWidth = 13f
	private val cursorHeight = 12f
	private val gap = 5f

	private fun frameHandler() {
		val view = activeTooltip?.view ?: return
		view.parent?.mousePosition(mousePosition)
		toolTipLayoutData.left = mousePosition.x + cursorWidth + gap
		toolTipLayoutData.top = mousePosition.y + cursorHeight + gap
	}

	override fun dispose() {
		activeTooltip = null
		_tooltips.clear()
	}
}

data class Tooltip<E>(
		val data: E,
		val view: ItemRenderer<E>,
		val priority: Float = 0f
)

class TooltipView(owner: Owned) : ContainerImpl(owner), ItemRenderer<String> {

	val style = bind(PanelStyle())

	private var background: UiComponent? = null
	private val textField = addChild(text())

	init {
		interactivityMode = InteractivityMode.NONE
		styleTags.add(Companion)

		watch(style) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))
		}
	}

	override var data: String?
		get() = textField.text
		set(value) {
			textField.text = value ?: ""
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val padding = style.padding
		val childWidth = padding.reduceWidth(explicitWidth)
		val childHeight = padding.reduceHeight(explicitHeight)
		textField.setSize(childWidth, childHeight)
		textField.moveTo(padding.left, padding.top)
		out.set(padding.expandWidth2(textField.width), padding.expandHeight2(textField.height))
		background?.setSize(out)
	}

	companion object : StyleTag
}


fun UiComponentRo.tooltipAttachment(): TooltipAttachment {
	return createOrReuseAttachment(TooltipAttachment) { TooltipAttachment(this) }
}

fun UiComponentRo.tooltip(value: String?) {
	tooltipAttachment().tooltip = if (value == null) null else inject(TooltipManager).createTooltip(value)
}
