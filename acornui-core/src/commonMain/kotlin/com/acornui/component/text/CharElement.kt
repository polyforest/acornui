/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.component.text

import com.acornui.async.Deferred
import com.acornui.async.resultOrNull
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import com.acornui.core.graphic.BlendMode
import com.acornui.gl.core.GlState
import com.acornui.gl.core.putQuadIndices
import com.acornui.gl.core.putVertex
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.string.isBreaking
import kotlin.math.floor

/**
 * Represents a single character in a [TextField], typically within a [TextSpanElement].
 */
class CharElement private constructor() : TextElement, Clearable {

	private val _bounds = Bounds()
	override val bounds: BoundsRo
		get() = _bounds.set(explicitWidth ?: advanceX, lineHeight, parentSpan?.baseline ?: 0f)

	private lateinit var glState: GlState

	override var char: Char = CHAR_PLACEHOLDER
	override var parentSpan: TextSpanElementRo<TextElementRo>? = null

	private val style: CharElementStyleRo?
		get() = parentSpan?.charElementStyle

	val glyph: Glyph?
		get() {
			return style?.font?.resultOrNull()?.getGlyphSafe(char)
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
	private val normalWorld = Vector3()

	private val backgroundVertices: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())

	private val lineVertices: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())

	private val tmpVec = Vector3()

	private var fontColor: ColorRo = Color.BLACK
	private var backgroundColor: ColorRo = Color.CLEAR
	private val colorTmp = Color()

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

	override fun validateVertices(leftClip: Float, topClip: Float, rightClip: Float, bottomClip: Float) {
		val style = style ?: return
		val x = x
		val y = y
		val glyph = glyph ?: return

		var charL = glyph.offsetX + x
		var charT = glyph.offsetY + y
		var charR = charL + glyph.width
		var charB = charT + glyph.height

		val lineHeight = parentSpan?.lineHeight ?: 0f
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
		charVertices[0].set(charL, charT, 0f)
		charVertices[1].set(charR, charT, 0f)
		charVertices[2].set(charR, charB, 0f)
		charVertices[3].set(charL, charB, 0f)

		// Background vertices
		backgroundVertices[0].set(bgL, bgT, 0f)
		backgroundVertices[1].set(bgR, bgT, 0f)
		backgroundVertices[2].set(bgR, bgB, 0f)
		backgroundVertices[3].set(bgL, bgB, 0f)

		if (style.underlined || style.strikeThrough) {
			var lineL = x
			if (lineL < leftClip) lineL = leftClip
			var lineR = x + glyph.advanceX
			if (lineL > rightClip) lineR = rightClip
			var lineT = y + if (style.strikeThrough) {
				floor((baseline / 2f))
			} else {
				baseline + 1f
			}
			if (lineT < topClip) lineT = topClip
			var lineB = lineT + style.lineThickness
			if (lineB > bottomClip) lineB = bottomClip

			lineVertices[0].set(lineL, lineT, 0f)
			lineVertices[1].set(lineR, lineT, 0f)
			lineVertices[2].set(lineR, lineB, 0f)
			lineVertices[3].set(lineL, lineB, 0f)
		}
	}

	override fun render(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		if (!visible || tint.a <= 0f) return
		val style = style ?: return
		val glyph = glyph ?: return
		val glState = glState
		val batch = glState.batch
		val colorTmp = colorTmp
		transform.rot(normalWorld.set(Vector3.NEG_Z)).nor()

		if (backgroundColor.a > 0f) {
			colorTmp.set(backgroundColor).mul(tint)
			batch.begin()
			glState.setTexture(glState.whitePixel)
			glState.blendMode(BlendMode.NORMAL, false)
			// Top left
			batch.putVertex(transform.prj(tmpVec.set(backgroundVertices[0])), normalWorld, colorTmp, 0f, 0f)
			// Top right
			batch.putVertex(transform.prj(tmpVec.set(backgroundVertices[1])), normalWorld, colorTmp, 0f, 0f)
			// Bottom right
			batch.putVertex(transform.prj(tmpVec.set(backgroundVertices[2])), normalWorld, colorTmp, 0f, 0f)
			// Bottom left
			batch.putVertex(transform.prj(tmpVec.set(backgroundVertices[3])), normalWorld, colorTmp, 0f, 0f)
			batch.putQuadIndices()
		}

		colorTmp.set(fontColor).mul(tint)

		if (style.underlined || style.strikeThrough) {
			batch.begin()
			glState.setTexture(glState.whitePixel)
			glState.blendMode(BlendMode.NORMAL, false)

			// Top left
			batch.putVertex(transform.prj(tmpVec.set(lineVertices[0])), normalWorld, colorTmp, 0f, 0f)
			// Top right
			batch.putVertex(transform.prj(tmpVec.set(lineVertices[1])), normalWorld, colorTmp, 0f, 0f)
			// Bottom right
			batch.putVertex(transform.prj(tmpVec.set(lineVertices[2])), normalWorld, colorTmp, 0f, 0f)
			// Bottom left
			batch.putVertex(transform.prj(tmpVec.set(lineVertices[3])), normalWorld, colorTmp, 0f, 0f)
			batch.putQuadIndices()
		}

		if (u == u2 || v == v2 || glyph.width <= 0f || glyph.height <= 0f) return // Nothing to draw
		batch.begin()
		glState.setTexture(glyph.texture)
		glState.blendMode(BlendMode.NORMAL, glyph.premultipliedAlpha)

		if (glyph.isRotated) {
			// Top left
			batch.putVertex(transform.prj(tmpVec.set(charVertices[0])), normalWorld, colorTmp, u2, v)
			// Top right
			batch.putVertex(transform.prj(tmpVec.set(charVertices[1])), normalWorld, colorTmp, u2, v2)
			// Bottom right
			batch.putVertex(transform.prj(tmpVec.set(charVertices[2])), normalWorld, colorTmp, u, v2)
			// Bottom left
			batch.putVertex(transform.prj(tmpVec.set(charVertices[3])), normalWorld, colorTmp, u, v)
		} else {
			// Top left
			batch.putVertex(transform.prj(tmpVec.set(charVertices[0])), normalWorld, colorTmp, u, v)
			// Top right
			batch.putVertex(transform.prj(tmpVec.set(charVertices[1])), normalWorld, colorTmp, u2, v)
			// Bottom right
			batch.putVertex(transform.prj(tmpVec.set(charVertices[2])), normalWorld, colorTmp, u2, v2)
			// Bottom left
			batch.putVertex(transform.prj(tmpVec.set(charVertices[3])), normalWorld, colorTmp, u, v2)
		}
		batch.putQuadIndices()
	}

	override fun dispose() {
		pool.free(this)
	}

	override fun clear() {
		explicitWidth = null
		char = CHAR_PLACEHOLDER
		parentSpan = null
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

		internal fun obtain(char: Char, glState: GlState): CharElement {
			val c = pool.obtain()
			c.char = char
			c.glState = glState
			return c
		}
	}
}

interface CharElementStyleRo {
	val font: Deferred<BitmapFont>?
	val underlined: Boolean
	val strikeThrough: Boolean
	val lineThickness: Float
	val selectedTextColorTint: ColorRo
	val selectedBackgroundColor: ColorRo
	val textColorTint: ColorRo
	val backgroundColor: ColorRo
}

/**
 * A [TextSpanElement] will decorate the span's characters all the same way. This class is used to store those
 * calculated properties.
 */
class CharElementStyle : CharElementStyleRo {
	override var font: Deferred<BitmapFont>? = null
	override var underlined: Boolean = false
	override var strikeThrough: Boolean = false
	override var lineThickness: Float = 1f
	override val selectedTextColorTint = Color()
	override val selectedBackgroundColor = Color()
	override val textColorTint = Color()
	override val backgroundColor = Color()

	fun set(charStyle: CharStyle) {
		font = charStyle.font
		underlined = charStyle.underlined
		strikeThrough = charStyle.strikeThrough
		lineThickness = charStyle.lineThickness
		selectedTextColorTint.set(charStyle.selectedColorTint)
		selectedBackgroundColor.set(charStyle.selectedBackgroundColor)
		textColorTint.set(charStyle.colorTint)
		backgroundColor.set(charStyle.backgroundColor)
	}
}
