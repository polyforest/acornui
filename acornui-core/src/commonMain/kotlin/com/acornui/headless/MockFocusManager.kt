package com.acornui.headless

import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.focus.FocusChangedEventRo
import com.acornui.focus.FocusChangingEventRo
import com.acornui.focus.FocusManager
import com.acornui.focus.FocusOptions
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

object MockFocusManager : FocusManager {
	override fun init(root: ElementContainer<UiComponent>) {
	}

	override val focusedChanging: Signal<(FocusChangingEventRo) -> Unit> = emptySignal()
	override val focusedChanged: Signal<(FocusChangedEventRo) -> Unit> = emptySignal()

	override fun invalidateFocusableOrder(value: UiComponentRo) {
	}

	override val focused: UiComponentRo? = null

	override fun focus(value: UiComponentRo?, options: FocusOptions) {
	}

	override fun nextFocusable(): UiComponentRo = throw UnsupportedOperationException()

	override fun previousFocusable(): UiComponentRo = throw UnsupportedOperationException()

	override val focusables: List<UiComponentRo> = emptyList()

	override fun dispose() {
	}
}
