package com.acornui.filter

import com.acornui.component.ComponentInit
import com.acornui.component.UiComponentRo
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.math.MinMaxRo

class BlurFilter(owner: Owned) : FramebufferFilter(owner) {

	var blurX by bindable(1f)
	var blurY by bindable(1f)

	var quality = BlurQuality.NORMAL

	private val gl = inject(Gl20)
	private val glState = inject(GlState)
	private val blurShader = BlurShader(gl)
	private val blurFramebuffer = resizeableFramebuffer()

	init {
		clearColor = Color(0.5f, 0.5f, 0.5f, 0f)
	}

	override fun beforeRender(target: UiComponentRo, clip: MinMaxRo) {
		val hPad = blurX * 4f * quality.passes
		val vPad = blurY * 4f * quality.passes
		padding.set(left = hPad, top = vPad, right = hPad, bottom = vPad)
		super.beforeRender(target, clip)
	}

	override fun afterRender(target: UiComponentRo, clip: MinMaxRo) {
		framebuffer.end()

		val textureToBlur = framebuffer.texture
		textureToBlur.filterMin = TextureMinFilter.LINEAR
		textureToBlur.filterMag = TextureMagFilter.LINEAR
		blurFramebuffer.setSize(textureToBlur.width, textureToBlur.height)
		blurFramebuffer.texture.filterMin = TextureMinFilter.LINEAR
		blurFramebuffer.texture.filterMag = TextureMagFilter.LINEAR

		glState.setTexture(textureToBlur)

		glState.setShader(blurShader) {
			gl.uniform2f(blurShader.getRequiredUniformLocation("u_resolutionInv"), 1f / textureToBlur.width.toFloat(), 1f / textureToBlur.height.toFloat())
			glState.setTexture(framebuffer.texture)
			glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
			val passes = quality.passes
			for (i in 1 .. passes) {
				val p = i.toFloat() / passes.toFloat()
				blurFramebuffer.begin()
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), 0f, blurY * p)
				glState.batch.putIdt()
				blurFramebuffer.end()
				glState.setTexture(blurFramebuffer.texture)
				blurFramebuffer.begin()
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), blurX * p, 0f)
				glState.batch.putIdt()
				blurFramebuffer.end()
			}
		}
		draw(target, blurFramebuffer.texture)
	}

	private fun ShaderBatch.putIdt() {
		putVertex(-1f, -1f, 0f)
		putVertex(1f, -1f, 0f)
		putVertex(1f, 1f, 0f)
		putVertex(-1f, 1f, 0f)
		putQuadIndices()
	}

	override fun dispose() {
		super.dispose()
		blurFramebuffer.dispose()
		blurShader.dispose()
	}
}

internal class BlurShader(gl: Gl20) : ShaderProgramBase(gl,
		vertexShaderSrc = """
$DEFAULT_SHADER_HEADER

attribute vec4 a_position;

void main() {
	gl_Position =  a_position;
}

""", fragmentShaderSrc = """

$DEFAULT_SHADER_HEADER

uniform vec2 u_dir;
uniform vec2 u_resolutionInv;
uniform sampler2D u_texture;

void main() {
	vec2 coord = gl_FragCoord.xy * u_resolutionInv;
	vec2 m = u_dir * u_resolutionInv;
	vec4 sum = vec4(0.0);

	sum += texture2D(u_texture, coord - m * 4.0) * 0.05;
	sum += texture2D(u_texture, coord - m * 3.0) * 0.09;
	sum += texture2D(u_texture, coord - m * 2.0) * 0.12;
	sum += texture2D(u_texture, coord - m * 1.0) * 0.15;

	sum += texture2D(u_texture, coord) * 0.18;

	sum += texture2D(u_texture, coord + m * 1.0) * 0.15;
	sum += texture2D(u_texture, coord + m * 2.0) * 0.12;
	sum += texture2D(u_texture, coord + m * 3.0) * 0.09;
	sum += texture2D(u_texture, coord + m * 4.0) * 0.05;

	gl_FragColor = sum;
}

""")

fun Owned.blurFilter(init: ComponentInit<BlurFilter> = {}): BlurFilter {
	val b = BlurFilter(this)
	b.init()
	return b
}

enum class BlurQuality(val passes: Int) {
		LOW(1),
		NORMAL(2),
		HIGH(3),
}