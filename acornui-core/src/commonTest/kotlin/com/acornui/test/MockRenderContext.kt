package com.acornui.test

import com.acornui.component.RenderContextRo
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*

object MockRenderContext : RenderContextRo {

	override val parentContext: RenderContextRo? = null
	override val clipRegion: MinMaxRo = MinMaxRo.NEGATIVE_INFINITY
	override val colorTint: ColorRo = Color.CLEAR
	override val viewProjectionTransformInv: Matrix4Ro = Matrix4.IDENTITY
	override val viewProjectionTransform: Matrix4Ro  = Matrix4.IDENTITY
	override val viewTransform: Matrix4Ro = Matrix4.IDENTITY
	override val projectionTransform: Matrix4Ro = Matrix4.IDENTITY
	override val canvasTransform: RectangleRo = RectangleRo.EMPTY
	override val modelTransform: Matrix4Ro = Matrix4.IDENTITY
	override val modelTransformInv: Matrix4Ro = Matrix4.IDENTITY
}
