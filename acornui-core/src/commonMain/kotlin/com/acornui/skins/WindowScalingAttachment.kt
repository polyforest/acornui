package com.acornui.skins

import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.style.AlwaysFilter
import com.acornui.component.style.StyleRule
import com.acornui.component.text.charStyle
import com.acornui.di.ContextImpl
import com.acornui.function.as2
import com.acornui.graphic.Window

/**
 * Watches the window for scale changes, updating the character style.
 */
class WindowScalingAttachment(val target: UiComponent) : ContextImpl(target) {

	private val textScaling = charStyle()
	private val textScalingRule = StyleRule(textScaling, AlwaysFilter)
	private val window = inject(Window)

	init {
		window.scaleChanged.add(::updateWindowScaling.as2)
		updateWindowScaling()
	}

	private fun updateWindowScaling() {
		textScaling.apply {
			val window = inject(Window)
			scaleX = window.scaleX
			scaleY = window.scaleY
		}
	}

	fun apply() {
		if (!target.styleRules.contains(textScalingRule))
			target.styleRules.add(textScalingRule)
	}

	override fun dispose() {
		super.dispose()
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