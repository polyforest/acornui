package com.acornui.headless

import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.focus.FocusInitiator
import com.acornui.focus.FocusManager
import com.acornui.focus.FocusOptions

object MockFocusManager : FocusManager {
	override fun init(root: ElementContainer<UiComponent>) {
	}

	override fun invalidateFocusableOrder(value: UiComponentRo) {
	}

	override fun focus(value: UiComponentRo?, options: FocusOptions, initiator: FocusInitiator) {
	}

	override fun nextFocusable(): UiComponentRo = throw UnsupportedOperationException()

	override fun previousFocusable(): UiComponentRo = throw UnsupportedOperationException()

	override val focusables: List<UiComponentRo> = emptyList()

	override fun dispose() {
	}
}
