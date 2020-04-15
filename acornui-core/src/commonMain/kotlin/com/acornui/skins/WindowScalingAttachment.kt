package com.acornui.skins

import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.di.ContextImpl
import com.acornui.function.as2
import com.acornui.graphic.Window
import com.acornui.graphic.dpiStyle

/**
 * Watches the window for scale changes, updating the dpi scaling style.
 */
class WindowScalingAttachment(val target: UiComponent) : ContextImpl(target) {

	private val dpiScaling = dpiStyle()
	private val window = inject(Window)

	init {
		window.scaleChanged.add(::updateWindowScaling.as2)
		updateWindowScaling()
	}

	private fun updateWindowScaling() {
		dpiScaling.apply {
			scaleX = window.scaleX
			scaleY = window.scaleY
		}
	}

	fun apply() {
		if (!target.styleRules.contains(dpiScaling))
			target.styleRules.add(dpiScaling)
	}

	override fun dispose() {
		super.dispose()
		window.scaleChanged.remove(::updateWindowScaling.as2)
		target.styleRules.remove(dpiScaling)
	}

	companion object {

		fun attach(target: UiComponent) {
			val scalingAttachment = target.createOrReuseAttachment(WindowScalingAttachment) { WindowScalingAttachment(target) }
			scalingAttachment.apply()
		}
	}
}