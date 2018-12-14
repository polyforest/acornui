package com.acornui.component.layout

import com.acornui.component.ComponentInit
import com.acornui.component.ElementContainerImpl
import com.acornui.component.UiComponent
import com.acornui.core.di.Owned
import com.acornui.core.focus.Focusable
import com.acornui.math.Bounds

/**
 * A container with a custom one-off layout.
 * For reusable layouts see [LayoutContainer].
 *
 * Example
 * ```
 *   +customLayout {
 *      val myLabel = +text("Hello")
 *
 *      updateSizeConstraintsCallback = { out ->
 *          out.width.min = 300f + 40f
 *          out.height.min = 40f + 30f
 *      }
 *
 *      updateLayoutCallback = { explicitWidth, explicitHeight, out ->
 *          myLabel.setSize(300f, 40f)
 *          myLabel.moveTo(40f, 30f)
 *          out.set(myLabel.right, myLabel.bottom)
 *      }
 *   }
 * ```
 */
open class CustomLayoutContainer(
		owner: Owned
) : ElementContainerImpl<UiComponent>(owner), Focusable {

	var updateSizeConstraintsCallback: (out: SizeConstraints) -> Unit = { _ -> }
	var updateLayoutCallback: (explicitWidth: Float?, explicitHeight: Float?, out: Bounds) -> Unit = { _, _, _ -> }

	override fun updateSizeConstraints(out: SizeConstraints) {
		updateSizeConstraintsCallback(out)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		updateLayoutCallback(explicitWidth, explicitHeight, out)
		if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
		if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight
	}
}

fun Owned.customLayout(init: ComponentInit<CustomLayoutContainer> = {}): CustomLayoutContainer {
	val c = CustomLayoutContainer(this)
	c.init()
	return c
}