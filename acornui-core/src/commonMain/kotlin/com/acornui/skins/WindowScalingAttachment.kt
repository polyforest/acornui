package com.acornui.skins

import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.style.addStyleRule
import com.acornui.component.text.charStyle
import com.acornui.core.di.inject
import com.acornui.core.graphic.Window

/**
 * Watches the window for scale changes, updating the character style.
 */
class WindowScalingAttachment(val target: UiComponent) {

	private val textScaling = charStyle()
	private val window = target.inject(Window)

	init {
		window.scaleChanged.add { _, _ ->
			updateWindowScaling()
		}
		updateWindowScaling()
		target.addStyleRule(textScaling)
	}

	private fun updateWindowScaling() {
		textScaling.apply {
			val window = target.inject(Window)
			scaleX = window.scaleX
			scaleY = window.scaleY
		}
	}

	companion object {
		fun attach(target: UiComponent) {
			target.createOrReuseAttachment(WindowScalingAttachment) { WindowScalingAttachment(target) }
		}
	}
}