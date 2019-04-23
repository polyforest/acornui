package com.acornui.component

import com.acornui.graphic.ColorRo
import com.acornui.math.*

class PaddedDrawable(
		private val wrapped: BasicDrawable
) : BasicDrawable {

	val padding = Pad()

	override val naturalWidth: Float
		get() = padding.expandWidth2(wrapped.naturalWidth)

	override val naturalHeight: Float
		get() = padding.expandHeight2(wrapped.naturalHeight)

	override fun updateVertices(width: Float, height: Float, x: Float, y: Float, z: Float, rotation: Float, originX: Float, originY: Float) {
		wrapped.updateVertices(padding.reduceWidth2(width), padding.reduceHeight2(height), x, y, z, rotation, originX - padding.left, originY - padding.top)
	}

	override fun render(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		wrapped.render(clip, transform, tint)
	}
}