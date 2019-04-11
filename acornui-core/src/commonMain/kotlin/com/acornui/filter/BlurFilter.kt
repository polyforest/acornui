package com.acornui.filter

import com.acornui.component.ComponentInit
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.math.MinMaxRo
import com.acornui.math.Pad
import com.acornui.math.PadRo

open class BlurFilter(owner: Owned) : FramebufferFilter(owner) {

	var blurX by bindable(1f)
	var blurY by bindable(1f)

	var quality by bindable(BlurQuality.NORMAL)

	private val gl = inject(Gl20)
	private val glState = inject(GlState)
	private val blurShader = BlurShader(gl)
	private val blurFramebufferA = resizeableFramebuffer()

	private val _padding = Pad()
	override val padding: PadRo
		get() {
			val hPad = blurX * 4f * quality.passes
			val vPad = blurY * 4f * quality.passes
			return _padding.set(left = hPad, top = vPad, right = hPad, bottom = vPad)
		}

	override val shouldSkipFilter: Boolean
		get() = super.shouldSkipFilter || (blurX <= 0f && blurY <= 0f)

	init {
		clearColor = Color(0.5f, 0.5f, 0.5f, 0f)
	}

	override fun draw(clip: MinMaxRo) {
		renderToFramebuffer(clip)
		drawToScreen()
	}

	protected fun renderToFramebuffer(clip: MinMaxRo) {
		beginFramebuffer(clip)
		renderContents(clip)
		framebuffer.end()

		val textureToBlur = framebuffer.texture
		textureToBlur.filterMin = TextureMinFilter.LINEAR
		textureToBlur.filterMag = TextureMagFilter.LINEAR
		blurFramebufferA.setSize(textureToBlur.width, textureToBlur.height)
		blurFramebufferA.texture.filterMin = TextureMinFilter.LINEAR
		blurFramebufferA.texture.filterMag = TextureMagFilter.LINEAR
		framebuffer.setSize(textureToBlur.width, textureToBlur.height)
		framebuffer.texture.filterMin = TextureMinFilter.LINEAR
		framebuffer.texture.filterMag = TextureMagFilter.LINEAR

		glState.setTexture(textureToBlur)

		glState.useShader(blurShader) {
			gl.uniform2f(blurShader.getRequiredUniformLocation("u_resolutionInv"), 1f / textureToBlur.width.toFloat(), 1f / textureToBlur.height.toFloat())
			glState.setTexture(framebuffer.texture)
			glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
			val passes = quality.passes
			for (i in 1 .. passes) {
				val p = i.toFloat() / passes.toFloat()
				blurFramebufferA.begin()
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), 0f, blurY * p)
				glState.batch.putIdt()
				blurFramebufferA.end()
				framebuffer.begin()
				glState.setTexture(blurFramebufferA.texture)
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), blurX * p, 0f)
				glState.batch.putIdt()
				framebuffer.end()
				glState.setTexture(framebuffer.texture)
			}
		}
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
		blurFramebufferA.dispose()
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
		BEST(4),
}