package com.acornui.headless

import com.acornui.RedrawRegions
import com.acornui.component.RenderContextRo
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo
import com.acornui.math.RectangleRo

object MockRenderContext : RenderContextRo {

	override val parentContext: RenderContextRo? = null
	override val clipRegion: MinMaxRo = MinMaxRo.NEGATIVE_INFINITY
	override val colorTint: ColorRo = Color.WHITE
	override val viewProjectionTransformInv: Matrix4Ro = Matrix4.IDENTITY
	override val viewProjectionTransform: Matrix4Ro  = Matrix4.IDENTITY
	override val viewTransform: Matrix4Ro = Matrix4.IDENTITY
	override val projectionTransform: Matrix4Ro = Matrix4.IDENTITY
	override val canvasTransform: RectangleRo = RectangleRo.EMPTY
	override val modelTransform: Matrix4Ro = Matrix4.IDENTITY
	override val modelTransformInv: Matrix4Ro = Matrix4.IDENTITY
	override val redraw: RedrawRegions = RedrawRegions.ALWAYS
}
