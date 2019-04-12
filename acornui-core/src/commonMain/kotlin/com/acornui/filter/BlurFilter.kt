package com.acornui.filter

import com.acornui.component.ComponentInit
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Window
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.math.*
import kotlin.math.ceil
import kotlin.math.floor

open class BlurFilter(owner: Owned) : RenderFilterBase(owner) {

	var blurX by bindable(1f)
	var blurY by bindable(1f)

	var quality by bindable(BlurQuality.NORMAL)
	var colorTint by bindable(Color.WHITE)

	private val gl = inject(Gl20)
	private val glState = inject(GlState)
	private val window = inject(Window)
	private val blurShader = own(BlurShader(gl))
	private val blurFramebufferA = own(resizeableFramebuffer())
	private val blurFramebufferB = own(resizeableFramebuffer())

	private val _padding = Pad()
	val padding: PadRo
		get() {
			val hPad = blurX * 4f * quality.passes
			val vPad = blurY * 4f * quality.passes
			return _padding.set(left = hPad, top = vPad, right = hPad, bottom = vPad)
		}

	override fun canvasDrawRegion(out: MinMax): MinMax {
		super.canvasDrawRegion(out).inflate(padding)
		out.xMin = floor(out.xMin)
		out.yMin = floor(out.yMin)
		out.xMax = ceil(out.xMax)
		out.yMax = ceil(out.yMax)
		return out
	}

	override val shouldSkipFilter: Boolean
		get() = super.shouldSkipFilter || (blurX <= 0f && blurY <= 0f)

	private val framebufferUtil = RenderableToFramebuffer(this)

	/**
	 * The region (in canvas coordinates) where the contents have been drawn.
	 */
	val canvasRegion: MinMaxRo
		get() = framebufferUtil.canvasRegion

	init {
		framebufferUtil.clearColor = Color(0.5f, 0.5f, 0.5f, 0f)
	}

	override fun draw(clip: MinMaxRo) {
		drawToPingPongBuffers(clip)
		drawBlurToScreen()
	}

	fun drawToPingPongBuffers(clip: MinMaxRo) {
		val contents = contents ?: return
		framebufferUtil.drawToFramebuffer(clip, contents, padding)
		val textureToBlur = framebufferUtil.texture

		blurFramebufferA.setSize(textureToBlur.width, textureToBlur.height)
		blurFramebufferA.texture.filterMin = TextureMinFilter.LINEAR
		blurFramebufferA.texture.filterMag = TextureMagFilter.LINEAR
		blurFramebufferB.setSize(textureToBlur.width, textureToBlur.height)
		blurFramebufferB.texture.filterMin = TextureMinFilter.LINEAR
		blurFramebufferB.texture.filterMag = TextureMagFilter.LINEAR

		glState.setTexture(textureToBlur)

		glState.useShader(blurShader) {
			gl.uniform2f(blurShader.getRequiredUniformLocation("u_resolutionInv"), 1f / textureToBlur.width.toFloat(), 1f / textureToBlur.height.toFloat())
			glState.setTexture(framebufferUtil.texture)
			glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
			val passes = quality.passes
			for (i in 1 .. passes) {
				val p = i.toFloat() / passes.toFloat()
				blurFramebufferA.begin()
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), 0f, blurY * p)
				glState.batch.putIdt()
				blurFramebufferA.end()
				blurFramebufferB.begin()
				glState.setTexture(blurFramebufferA.texture)
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), blurX * p, 0f)
				glState.batch.putIdt()
				blurFramebufferB.end()
				glState.setTexture(blurFramebufferB.texture)
			}
		}
	}

	fun drawBlurToScreen(canvasX: Float = canvasRegion.xMin, canvasY: Float = canvasRegion.yMin) {
		val texture = blurFramebufferB.texture
		val batch = glState.batch
		batch.begin()
		glState.viewProjection = Matrix4.IDENTITY
		glState.model = Matrix4.IDENTITY
		glState.setTexture(texture)
		glState.blendMode(BlendMode.NORMAL, false)

		val w = canvasRegion.width
		val h = canvasRegion.height

		val x1 = canvasX / window.width * 2f - 1f
		val x2 = (canvasX + w) / window.width * 2f - 1f
		val y1 = -(canvasY / window.height * 2f - 1f)
		val y2 = -((canvasY + h) / window.height * 2f - 1f)

		val u = canvasRegion.width / texture.width
		val v = 1f - (canvasRegion.height / texture.height)

		// Top left
		batch.putVertex(x1, y1, 0f, u = 0f, v = 1f, colorTint = colorTint)
		// Top right
		batch.putVertex(x2, y1, 0f, u = u, v = 1f, colorTint = colorTint)
		// Bottom right
		batch.putVertex(x2, y2, 0f, u = u, v = v, colorTint = colorTint)
		// Bottom left
		batch.putVertex(x1, y2, 0f, u = 0f, v = v, colorTint = colorTint)
		batch.putQuadIndices()
	}

	fun drawOriginalToScreen() {
		framebufferUtil.drawToScreen()
	}

	private fun ShaderBatch.putIdt() {
		putVertex(-1f, -1f, 0f)
		putVertex(1f, -1f, 0f)
		putVertex(1f, 1f, 0f)
		putVertex(-1f, 1f, 0f)
		putQuadIndices()
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