package com.acornui.filter

import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.ShaderProgram
import com.acornui.gl.core.useShader
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo

/**
 * A filter that sets a custom shader.
 */
class ShaderFilter(
		owner: Owned,

		/**
		 * The shader to apply to the component.
		 */
		var shader: ShaderProgram,

		/**
		 * Before rendering, this will be called, providing a time to set uniforms.
		 */
		var configure: (gl: Gl20, shader: ShaderProgram) -> Unit = { _, _ -> }
) : RenderFilterBase(owner) {

	private val glState = inject(GlState)
	private val gl = inject(Gl20)

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		glState.useShader(shader) {
			configure(gl, shader)
			contents?.render()
		}
	}

}