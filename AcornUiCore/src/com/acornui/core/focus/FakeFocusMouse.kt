package com.acornui.core.focus

import com.acornui.component.UiComponentRo
import com.acornui.component.stage
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.input.*
import com.acornui.core.input.interaction.*
import com.acornui.core.time.time

/**
 * Dispatches mouse events when using SPACE or ENTER key presses on the focused element.
 */
class FakeFocusMouse(
		override val injector: Injector
) : Scoped, Disposable {

	private val focus = inject(FocusManager)
	private val interactivity = inject(InteractivityManager)

	private val fakeMouseEvent = MouseInteraction()
	private var downKey: Int? = null
	private var downElement: UiComponentRo? = null

	private val keyDownHandler = {
		event: KeyInteractionRo ->
		if (!event.handled) {
			val f = focus.focused()
			if (f != null) {
				val isRepeat = event.isRepeat && downKey == event.keyCode && f.downRepeatEnabled()
				if ((downKey == null || isRepeat) && event.keyCode == Ascii.SPACE || event.keyCode == Ascii.RETURN) {
					event.handled = true
					downKey = event.keyCode
					downElement = focus.focused()
					dispatchFakeMouseEvent(MouseInteractionRo.MOUSE_DOWN)
				}
			}
		}
	}

	private val keyUpHandler = {
		event: KeyInteractionRo ->
		if (!event.handled && event.keyCode == downKey) {
			event.handled = true
			dispatchFakeMouseEvent(MouseInteractionRo.MOUSE_UP)
			downKey = null
	 		if (downElement == focus.focused()) {
				downElement!!.dispatchClick()
				downElement = null
			}
		}
	}

	private fun dispatchFakeMouseEvent(type: InteractionType<MouseInteractionRo>) {
		val f = focus.focused()
		if (f != null) {
			fakeMouseEvent.clear()
			fakeMouseEvent.isFabricated = true
			fakeMouseEvent.type = type
			fakeMouseEvent.button = WhichButton.LEFT
			fakeMouseEvent.timestamp = time.nowMs()
			interactivity.dispatch(f, fakeMouseEvent)
		}
	}

	init {
		stage.keyDown().add(keyDownHandler)
		stage.keyUp().add(keyUpHandler)
	}

	override fun dispose() {
		stage.keyDown().remove(keyDownHandler)
		stage.keyUp().remove(keyUpHandler)
	}
}

fun Scoped.fakeFocusMouse(): FakeFocusMouse {
	return FakeFocusMouse(injector)
}