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

import com.acornui.async.getCompletedOrNull
import com.acornui.gl.core.GlState
import com.acornui.gl.core.putQuadIndices
import com.acornui.gl.core.putVertex
import com.acornui.graphic.BlendMode
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Bounds
import com.acornui.math.BoundsRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.Vector3
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import com.acornui.string.isBreaking
import kotlinx.coroutines.Deferred
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
			return style?.font?.getCompletedOrNull()?.getGlyphSafe(char)
		}

	override var x = 0f
	override var y = 0f

	private val scaleX: Float
		get() = style?.scaleX ?: 1f

	private val scaleY: Float
		get() = style?.scaleY ?: 1f

	override val advanceX: Float
		get() = (glyph?.advanceX?.toFloat() ?: 0f) / scaleX

	override var explicitWidth: Float? = null

	override var kerning: Float = 0f

	override fun getKerning(next: TextElementRo): Float {
		val d = glyph?.data ?: return 0f
		val c = next.char ?: return 0f
		return d.getKerning(c).toFloat() / scaleX
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
	private val charVerticesWorld: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())
	private val normalWorld = Vector3()

	private val backgroundVertices: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())
	private val backgroundVerticesWorld: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())

	private val lineVertices: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())
	private val lineVerticesWorld: Array<Vector3> = arrayOf(Vector3(), Vector3(), Vector3(), Vector3())

	private val fontColorWorld = Color()
	private val backgroundColorWorld = Color()

	override val clearsLine: Boolean
		get() = char == '\n'
	override val clearsTabstop: Boolean
		get() = char == '\t'
	override val isBreaking: Boolean
		get() = char.isBreaking()
	override val overhangs: Boolean
		get() = char == ' '

	override var selected: Boolean = false

	override fun updateVertices(leftClip: Float, topClip: Float, rightClip: Float, bottomClip: Float) {
		val style = style ?: return
		val x = x
		val y = y
		val glyph = glyph ?: return

		val lineHeight = parentSpan?.lineHeight ?: 0f
		val bgL = maxOf(leftClip, x)
		val bgT = maxOf(topClip, y)
		val bgR = minOf(rightClip, x + width + kerning)
		val bgB = minOf(bottomClip, y + lineHeight)

		visible = bgL < rightClip && bgT < bottomClip && bgR > leftClip && bgB > topClip
		if (!visible)
			return

		val region = glyph.region
		val textureW = glyph.texture.widthPixels.toFloat()
		val textureH = glyph.texture.heightPixels.toFloat()

		// Pixels
		var regionX = region.x.toFloat()
		var regionY = region.y.toFloat()
		var regionR = region.right.toFloat()
		var regionB = region.bottom.toFloat()


		// Points
		var charL = x + glyph.offsetX / scaleX
		var charT = y + glyph.offsetY / scaleY
		var charR = charL + glyph.width / scaleX
		var charB = charT + glyph.height / scaleY

		if (charL < leftClip) {
			if (glyph.isRotated) regionY += leftClip - charL
			else regionX += (leftClip - charL) * scaleX
			charL = leftClip
		}
		if (charT < topClip) {
			if (glyph.isRotated) regionX += topClip - charT
			else regionY += (topClip - charT) * scaleY
			charT = topClip
		}
		if (charR > rightClip) {
			if (glyph.isRotated) regionB -= charR - rightClip
			else regionR -= (charR - rightClip) * scaleX
			charR = rightClip
		}
		if (charB > bottomClip) {
			if (glyph.isRotated) regionR -= charB - bottomClip
			else regionB -= (charB - bottomClip) * scaleY
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

	override fun updateWorldVertices(transform: Matrix4Ro, tint: ColorRo) {
		val style = style ?: return

		fontColorWorld.set(if (selected) style.selectedTextColorTint else style.textColorTint).mul(tint)
		backgroundColorWorld.set(if (selected) style.selectedBackgroundColor else style.backgroundColor).mul(tint)
		transform.rot(normalWorld.set(Vector3.NEG_Z)).nor()

		for (i in 0..3) {
			transform.prj(charVerticesWorld[i].set(charVertices[i]))
		}

		for (i in 0..3) {
			transform.prj(backgroundVerticesWorld[i].set(backgroundVertices[i]))
		}

		if (style.underlined || style.strikeThrough) {
			for (i in 0..3) {
				transform.prj(lineVerticesWorld[i].set(lineVertices[i]))
			}
		}
	}

	override fun renderBackground() {
		if (!visible) return
		val style = style ?: return
		val glState = glState
		val batch = glState.batch
		val backgroundColorWorld = backgroundColorWorld
		val fontColorWorld = fontColorWorld
		val backgroundVerticesWorld = backgroundVerticesWorld
		val normalWorld = normalWorld

		if (backgroundColorWorld.a > 0f) {
			batch.begin()
			glState.setTexture(glState.whitePixel)
			glState.blendMode(BlendMode.NORMAL, false)
			// Top left
			batch.putVertex(backgroundVerticesWorld[0], normalWorld, backgroundColorWorld, 0f, 0f)
			// Top right
			batch.putVertex(backgroundVerticesWorld[1], normalWorld, backgroundColorWorld, 0f, 0f)
			// Bottom right
			batch.putVertex(backgroundVerticesWorld[2], normalWorld, backgroundColorWorld, 0f, 0f)
			// Bottom left
			batch.putVertex(backgroundVerticesWorld[3], normalWorld, backgroundColorWorld, 0f, 0f)
			batch.putQuadIndices()
		}

		if (style.underlined || style.strikeThrough && fontColorWorld.a > 0f) {
			batch.begin()
			glState.setTexture(glState.whitePixel)
			glState.blendMode(BlendMode.NORMAL, false)
			val lineVerticesWorld = lineVerticesWorld

			// Top left
			batch.putVertex(lineVerticesWorld[0], normalWorld, fontColorWorld, 0f, 0f)
			// Top right
			batch.putVertex(lineVerticesWorld[1], normalWorld, fontColorWorld, 0f, 0f)
			// Bottom right
			batch.putVertex(lineVerticesWorld[2], normalWorld, fontColorWorld, 0f, 0f)
			// Bottom left
			batch.putVertex(lineVerticesWorld[3], normalWorld, fontColorWorld, 0f, 0f)
			batch.putQuadIndices()
		}

	}

	override fun renderForeground() {
		if (!visible) return
		val glyph = glyph ?: return
		val glState = glState
		val batch = glState.batch
		
		val fontColorWorld = fontColorWorld
		val normalWorld = normalWorld
		val charVerticesWorld = charVerticesWorld
		if (u == u2 || v == v2 || glyph.width <= 0f || glyph.height <= 0f || fontColorWorld.a <= 0f) return // Nothing to draw
		batch.begin()
		glState.setTexture(glyph.texture)
		glState.blendMode(BlendMode.NORMAL, glyph.premultipliedAlpha)

		if (glyph.isRotated) {
			// Top left
			batch.putVertex(charVerticesWorld[0], normalWorld, fontColorWorld, u2, v)
			// Top right
			batch.putVertex(charVerticesWorld[1], normalWorld, fontColorWorld, u2, v2)
			// Bottom right
			batch.putVertex(charVerticesWorld[2], normalWorld, fontColorWorld, u, v2)
			// Bottom left
			batch.putVertex(charVerticesWorld[3], normalWorld, fontColorWorld, u, v)
		} else {
			// Top left
			batch.putVertex(charVerticesWorld[0], normalWorld, fontColorWorld, u, v)
			// Top right
			batch.putVertex(charVerticesWorld[1], normalWorld, fontColorWorld, u2, v)
			// Bottom right
			batch.putVertex(charVerticesWorld[2], normalWorld, fontColorWorld, u2, v2)
			// Bottom left
			batch.putVertex(charVerticesWorld[3], normalWorld, fontColorWorld, u, v2)
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
	val scaleX: Float
	val scaleY: Float
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
	override var scaleX: Float = 1f
	override var scaleY: Float = 1f

	fun set(charStyle: CharStyle) {
		font = charStyle.getFontAsync()
		underlined = charStyle.underlined
		strikeThrough = charStyle.strikeThrough
		lineThickness = charStyle.lineThickness
		selectedTextColorTint.set(charStyle.selectedColorTint)
		selectedBackgroundColor.set(charStyle.selectedBackgroundColor)
		textColorTint.set(charStyle.colorTint)
		backgroundColor.set(charStyle.backgroundColor)
		scaleX = charStyle.scaleX
		scaleY = charStyle.scaleY
	}
}
