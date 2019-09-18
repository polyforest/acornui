package com.acornui.headless

import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.focus.FocusManager
import com.acornui.signal.Cancel
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

object MockFocusManager : FocusManager {
	override fun init(root: ElementContainer<UiComponent>) {
	}

	override val focusedChanging: Signal<(UiComponentRo?, UiComponentRo?, Cancel) -> Unit> = emptySignal()
	override val focusedChanged: Signal<(UiComponentRo?, UiComponentRo?) -> Unit> = emptySignal()

	override fun invalidateFocusableOrder(value: UiComponentRo) {
	}

	override val focused: UiComponentRo? = null

	override fun focused(value: UiComponentRo?) {
	}

	override fun nextFocusable(): UiComponentRo = throw UnsupportedOperationException()

	override fun previousFocusable(): UiComponentRo = throw UnsupportedOperationException()

	override val focusables: List<UiComponentRo> = emptyList()

	override fun unhighlightFocused() {
	}

	override fun highlightFocused() {
	}

	override fun dispose() {
	}
}
