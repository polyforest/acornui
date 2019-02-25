package com.acornui.component.text

import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import com.acornui.core.floor
import com.acornui.core.graphic.BlendMode
import com.acornui.gl.core.GlState
import com.acornui.gl.core.putQuadIndices
import com.acornui.gl.core.putVertex
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.Vector3
import com.acornui.string.isBreaking

/**
 * Represents a single character in a [TextField], typically within a [TextSpanElement].
 */
class CharElement private constructor() : TextElement, Clearable {

	override var char: Char = CHAR_PLACEHOLDER
	private var style: CharElementStyle? = null

	override var textParent: TextSpanElementRo<TextElementRo>? = null

	val glyph: Glyph?
		get() {
			return style?.font?.getGlyphSafe(char)
		}

	override var x: Float = 0f
	override var y: Float = 0f

	override val advanceX: Float
		get() = (glyph?.advanceX?.toFloat() ?: 0f)

	override var explicitWidth: Float? = null

	override var kerning: Float = 0f

	override fun getKerning(next: TextElementRo): Float {
		val d = glyph?.data ?: return 0f
		val c = next.char ?: return 0f
		return d.getKerning(c).toFloat()
	}

	private var u = 0f
	private var v = 0f
	private var u2 = 0f
	private var v2 = 0f

	private var visible = false

	/**
	 * A cache of the vertex positions in world space.
	 */
	private val charVertices: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())
	private val normal = Vector3()

	private val backgroundVertices: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())

	private val lineVertices: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())

	private var fontColor: ColorRo = Color.BLACK
	private var backgroundColor: ColorRo = Color.CLEAR

	override val clearsLine: Boolean
		get() = char == '\n'
	override val clearsTabstop: Boolean
		get() = char == '\t'
	override val isBreaking: Boolean
		get() = char.isBreaking()
	override val overhangs: Boolean
		get() = char == ' '

	override fun setSelected(value: Boolean) {
		val style = style ?: return
		if (value) {
			fontColor = style.selectedTextColorTint
			backgroundColor = style.selectedBackgroundColor
		} else {
			fontColor = style.textColorTint
			backgroundColor = style.backgroundColor
		}
	}

	override fun validateVertices(transform: Matrix4Ro, leftClip: Float, topClip: Float, rightClip: Float, bottomClip: Float) {
		val style = style ?: return
		val x = x
		val y = y
		val glyph = glyph ?: return

		var charL = glyph.offsetX + x
		var charT = glyph.offsetY + y
		var charR = charL + glyph.width
		var charB = charT + glyph.height

		val lineHeight = textParent?.lineHeight ?: 0f
		val bgL = maxOf(leftClip, x)
		val bgT = maxOf(topClip, y)
		val bgR = minOf(rightClip, x + width + kerning)
		val bgB = minOf(bottomClip, y + lineHeight)

		visible = bgL < rightClip && bgT < bottomClip && bgR > leftClip && bgB > topClip
		if (!visible)
			return

		val region = glyph.region
		val textureW = glyph.texture.width.toFloat()
		val textureH = glyph.texture.height.toFloat()

		var regionX = region.x.toFloat()
		var regionY = region.y.toFloat()
		var regionR = region.right.toFloat()
		var regionB = region.bottom.toFloat()

		if (charL < leftClip) {
			if (glyph.isRotated) regionY += leftClip - charL
			else regionX += leftClip - charL
			charL = leftClip
		}
		if (charT < topClip) {
			if (glyph.isRotated) regionX += topClip - charT
			else regionY += topClip - charT
			charT = topClip
		}
		if (charR > rightClip) {
			if (glyph.isRotated) regionB -= charR - rightClip
			else regionR -= charR - rightClip
			charR = rightClip
		}
		if (charB > bottomClip) {
			if (glyph.isRotated) regionR -= charB - bottomClip
			else regionB -= charB - bottomClip
			charB = bottomClip
		}

		u = regionX / textureW
		v = regionY / textureH
		u2 = regionR / textureW
		v2 = regionB / textureH

		// Transform vertex coordinates from local to global
		transform.prj(charVertices[0].set(charL, charT, 0f))
		transform.prj(charVertices[1].set(charR, charT, 0f))
		transform.prj(charVertices[2].set(charR, charB, 0f))
		transform.prj(charVertices[3].set(charL, charB, 0f))

		// Background vertices
		transform.prj(backgroundVertices[0].set(bgL, bgT, 0f))
		transform.prj(backgroundVertices[1].set(bgR, bgT, 0f))
		transform.prj(backgroundVertices[2].set(bgR, bgB, 0f))
		transform.prj(backgroundVertices[3].set(bgL, bgB, 0f))

		if (style.underlined || style.strikeThrough) {
			var lineL = x
			if (lineL < leftClip) lineL = leftClip
			var lineR = x + glyph.advanceX
			if (lineL > rightClip) lineR = rightClip
			var lineT = y + if (style.strikeThrough) {
				(baseline / 2f).floor()
			} else {
				baseline + 1f
			}
			if (lineT < topClip) lineT = topClip
			var lineB = lineT + style.lineThickness
			if (lineB > bottomClip) lineB = bottomClip

			transform.prj(lineVertices[0].set(lineL, lineT, 0f))
			transform.prj(lineVertices[1].set(lineR, lineT, 0f))
			transform.prj(lineVertices[2].set(lineR, lineB, 0f))
			transform.prj(lineVertices[3].set(lineL, lineB, 0f))
		}

		transform.rot(normal.set(Vector3.NEG_Z)).nor()
	}

	override fun render(glState: GlState) {
		if (!visible) return
		val style = style ?: return
		val glyph = glyph ?: return
		val batch = glState.batch

		if (backgroundColor.a > 0f) {
			batch.begin()
			glState.setTexture(glState.whitePixel)
			glState.blendMode(BlendMode.NORMAL, false)
			// Top left
			batch.putVertex(backgroundVertices[0], normal, backgroundColor, 0f, 0f)
			// Top right
			batch.putVertex(backgroundVertices[1], normal, backgroundColor, 0f, 0f)
			// Bottom right
			batch.putVertex(backgroundVertices[2], normal, backgroundColor, 0f, 0f)
			// Bottom left
			batch.putVertex(backgroundVertices[3], normal, backgroundColor, 0f, 0f)
			batch.putQuadIndices()
		}

		if (style.underlined || style.strikeThrough) {
			batch.begin()
			glState.setTexture(glState.whitePixel)
			glState.blendMode(BlendMode.NORMAL, false)

			// Top left
			batch.putVertex(lineVertices[0], normal, fontColor, 0f, 0f)
			// Top right
			batch.putVertex(lineVertices[1], normal, fontColor, 0f, 0f)
			// Bottom right
			batch.putVertex(lineVertices[2], normal, fontColor, 0f, 0f)
			// Bottom left
			batch.putVertex(lineVertices[3], normal, fontColor, 0f, 0f)
			batch.putQuadIndices()
		}

		if (u == u2 || v == v2 || glyph.width <= 0f || glyph.height <= 0f) return // Nothing to draw
		batch.begin()
		glState.setTexture(glyph.texture)
		glState.blendMode(BlendMode.NORMAL, glyph.premultipliedAlpha)

		if (glyph.isRotated) {
			// Top left
			batch.putVertex(charVertices[0], normal, fontColor, u2, v)
			// Top right
			batch.putVertex(charVertices[1], normal, fontColor, u2, v2)
			// Bottom right
			batch.putVertex(charVertices[2], normal, fontColor, u, v2)
			// Bottom left
			batch.putVertex(charVertices[3], normal, fontColor, u, v)
		} else {
			// Top left
			batch.putVertex(charVertices[0], normal, fontColor, u, v)
			// Top right
			batch.putVertex(charVertices[1], normal, fontColor, u2, v)
			// Bottom right
			batch.putVertex(charVertices[2], normal, fontColor, u2, v2)
			// Bottom left
			batch.putVertex(charVertices[3], normal, fontColor, u, v2)
		}

		batch.putQuadIndices()
	}

	override fun dispose() {
		pool.free(this)
	}

	override fun clear() {
		explicitWidth = null
		char = CHAR_PLACEHOLDER
		style = null
		textParent = null
		x = 0f
		y = 0f
		u = 0f
		v = 0f
		u2 = 0f
		v2 = 0f
		visible = false
		kerning = 0f
		fontColor = Color.BLACK
		backgroundColor = Color.CLEAR
	}

	companion object {
		private const val CHAR_PLACEHOLDER = 'a'
		private val pool = ClearableObjectPool { CharElement() }

		fun obtain(char: Char, charStyle: CharElementStyle): CharElement {
			val c = pool.obtain()
			c.char = char
			c.style = charStyle
			return c
		}
	}
}