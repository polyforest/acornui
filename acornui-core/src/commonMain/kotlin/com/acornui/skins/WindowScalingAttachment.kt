package com.acornui.skins

import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.style.AlwaysFilter
import com.acornui.component.style.StyleRule
import com.acornui.component.text.charStyle
import com.acornui.Disposable
import com.acornui.di.inject
import com.acornui.graphic.Window
import com.acornui.function.as2

/**
 * Watches the window for scale changes, updating the character style.
 */
class WindowScalingAttachment(val target: UiComponent) : Disposable {

	private val textScaling = charStyle()
	private val textScalingRule = StyleRule(textScaling, AlwaysFilter)
	private val window = target.inject(Window)

	init {
		window.scaleChanged.add(::updateWindowScaling.as2)
		updateWindowScaling()
	}

	private fun updateWindowScaling() {
		textScaling.apply {
			val window = target.inject(Window)
			scaleX = window.scaleX
			scaleY = window.scaleY
		}
	}

	fun apply() {
		if (!target.styleRules.contains(textScalingRule))
			target.styleRules.add(textScalingRule)
	}

	override fun dispose() {
		window.scaleChanged.remove(::updateWindowScaling.as2)
		target.styleRules.remove(textScalingRule)

	}

	companion object {

		fun attach(target: UiComponent) {
			val scalingAttachment = target.createOrReuseAttachment(WindowScalingAttachment) { WindowScalingAttachment(target) }
			scalingAttachment.apply()
		}
	}
}