/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.graphic

import com.acornui.recycle.Clearable
import com.acornui.core.closeTo
import com.acornui.serialization.Reader
import com.acornui.serialization.Writer
import com.acornui.string.toRadix
import kotlin.math.abs

interface ColorRo {

	val r: Float
	val g: Float
	val b: Float
	val a: Float

	operator fun times(value: Float): Color {
		return Color().set(this).mulRgb(value)
	}

	operator fun plus(color: ColorRo): Color {
		return Color().set(this).add(color)
	}

	operator fun minus(color: ColorRo): Color {
		return Color().set(this).sub(color)
	}

	/**
	 * rgba(255, 255, 255, 1)
	 */
	fun toCssString(): String {
		return "rgba(${(r * 255).toInt()}, ${(g * 255).toInt()}, ${(b * 255).toInt()}, $a)"
	}

	/**
	 * RRGGBB
	 */
	fun toRgbString(): String {
		return r.toOctet() + g.toOctet() + b.toOctet()
	}

	/**
	 * RRGGBBAA
	 */
	fun toRgbaString(): String {
		return r.toOctet() + g.toOctet() + b.toOctet() + a.toOctet()
	}

	fun toHsl(out: Hsl): Hsl {
		out.a = a
		val max = maxOf(r, g, b)
		val min = minOf(r, g, b)
		out.l = (max + min) * 0.5f

		val d = max - min
		if (d == 0f) {
			out.h = 0f
			out.s = 0f
		} else {
			if (max == r) {
				out.h = 60f * ((g - b) / d)
			} else if (max == g) {
				out.h = 60f * ((b - r) / d + 2f)
			} else if (max == b) {
				out.h = 60f * ((r - g) / d + 4f)
			}
			if (out.h < 0f) out.h += 360f
			if (out.h >= 360f) out.h -= 360f
			out.s = d / (1f - abs(2 * out.l - 1f))
		}
		return out
	}

	fun toHsv(out: Hsv): Hsv {
		out.a = a
		val max = maxOf(r, g, b)
		val min = minOf(r, g, b)
		out.v = max

		val d = max - min
		if (d == 0f) {
			out.h = 0f
		} else {
			if (max == r) {
				out.h = 60f * ((g - b) / d)
			} else if (max == g) {
				out.h = 60f * ((b - r) / d + 2f)
			} else if (max == b) {
				out.h = 60f * ((r - g) / d + 4f)
			}
			if (out.h < 0f) out.h += 360f
			if (out.h >= 360f) out.h -= 360f
			if (max == 0f) out.s = 0f
			else out.s = d / max
		}
		return out
	}

	/**
	 * Returns true if each of the channels is close to the other color within the given tolerance.
	 */
	fun closeTo(other: ColorRo, tolerance: Float = 0.0001f): Boolean {
		if (this === other) return true
		if (!r.closeTo(other.r, tolerance)) return false
		if (!g.closeTo(other.g, tolerance)) return false
		if (!b.closeTo(other.b, tolerance)) return false
		if (!a.closeTo(other.a, tolerance)) return false
		return true
	}

	fun copy(): Color {
		return Color().set(this)
	}

}

fun Color(rgba: Long): Color {
	return Color().set8888(rgba)
}

/**
 * A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 *
 * @author mzechner
 */
data class Color(
		override var r: Float = 0f,
		override var g: Float = 0f,
		override var b: Float = 0f,
		override var a: Float = 0f
): ColorRo, Clearable {

	/**
	 * Sets this color to the given color.
	 *
	 * @param color the Color
	 * @return this Color
	 */
	fun set(color: ColorRo): Color {
		this.r = color.r
		this.g = color.g
		this.b = color.b
		this.a = color.a
		return this
	}

	/**
	 * Multiplies the this color and the given color
	 *
	 * @param color the color
	 * @return this Color.
	 */
	fun mul(color: ColorRo): Color {
		this.r *= color.r
		this.g *= color.g
		this.b *= color.b
		this.a *= color.a
		return this
	}

	/**
	 * Multiplies all components of this Color with the given value.
	 *
	 * @param value the value
	 * @return this Color.
	 */
	fun mul(value: Float): Color {
		this.r *= value
		this.g *= value
		this.b *= value
		this.a *= value
		return this
	}

	/**
	 * Multiplies rgb components of this Color with the given value.
	 *
	 * @param value the value
	 * @return this Color.
	 */
	fun mulRgb(value: Float): Color {
		this.r *= value
		this.g *= value
		this.b *= value
		return this
	}

	/**
	 * Adds the given color to this color.
	 *
	 * @param color the color
	 * @return this Color.
	 */
	fun add(color: ColorRo): Color {
		this.r += color.r
		this.g += color.g
		this.b += color.b
		this.a += color.a
		return this
	}

	/**
	 * Sets this color to CLEAR
	 */
	override fun clear() {
		r = 0f
		g = 0f
		b = 0f
		a = 0f
	}

	/**
	 * Subtracts the given color from this color
	 *
	 * @param color the color
	 * @return this Color.
	 */
	fun sub(color: ColorRo): Color {
		this.r -= color.r
		this.g -= color.g
		this.b -= color.b
		this.a -= color.a
		return this
	}

	/**
	 * Clamps this Color's components to a valid range [0 - 1]
	 * @return this Color for chaining
	 */
	fun clamp(): Color {
		if (r < 0f) r = 0f
		else if (r > 1f) r = 1f

		if (g < 0f) g = 0f
		else if (g > 1f) g = 1f

		if (b < 0f) b = 0f
		else if (b > 1f) b = 1f

		if (a < 0f) a = 0f
		else if (a > 1f) a = 1f
		return this
	}

	/**
	 * Sets this Color's component values.
	 *
	 * @param r Red component
	 * @param g Green component
	 * @param b Blue component
	 * @param a Alpha component
	 *
	 * @return this Color for chaining
	 */
	fun set(r: Float, g: Float, b: Float, a: Float): Color {
		this.r = r
		this.g = g
		this.b = b
		this.a = a
		return this
	}

	/**
	 * Sets this color's component values through an integer representation.
	 * 4294967295 - white
	 *
	 * @return this Color for chaining
	 * @see [rgba8888]
	 */
	fun set8888(rgba: Long): Color {
		r = ((rgba and 0xff000000).ushr(24)).toFloat() / 255f
		g = ((rgba and 0x00ff0000).ushr(16)).toFloat() / 255f
		b = ((rgba and 0x0000ff00).ushr(8)).toFloat() / 255f
		a = ((rgba and 0x000000ff)).toFloat() / 255f
		return this
	}

	/**
	 * Sets this color's component values through an integer representation.
	 * 4294967295 - white
	 *
	 * @return this Color for chaining
	 * @see [rgba8888]
	 */
	fun set8888(rgba: Int): Color {
		r = ((rgba and -16777216).ushr(24)).toFloat() / 255f
		g = ((rgba and 0x00ff0000).ushr(16)).toFloat() / 255f
		b = ((rgba and 0x0000ff00).ushr(8)).toFloat() / 255f
		a = ((rgba and 0x000000ff)).toFloat() / 255f
		return this
	}

	/**
	 * Sets this color's component values through an integer representation.
	 * 16777215 - white
	 *
	 * @return this Color for chaining
	 * @see [rgb888]
	 */
	fun set888(rgb: Int): Color {
		r = ((rgb and 0xff0000).ushr(16)).toFloat() / 255f
		g = ((rgb and 0x00ff00).ushr(8)).toFloat() / 255f
		b = ((rgb and 0x0000ff)).toFloat() / 255f
		a = 1f
		return this
	}

	/**
	 * Adds the given color component values to this Color's values.
	 *
	 * @param r Red component
	 * @param g Green component
	 * @param b Blue component
	 * @param a Alpha component
	 *
	 * @return this Color for chaining
	 */
	fun add(r: Float, g: Float, b: Float, a: Float): Color {
		this.r += r
		this.g += g
		this.b += b
		this.a += a
		return this
	}

	/**
	 * Subtracts the given values from this Color's component values.
	 *
	 * @param r Red component
	 * @param g Green component
	 * @param b Blue component
	 * @param a Alpha component
	 *
	 * @return this Color for chaining
	 */
	fun sub(r: Float, g: Float, b: Float, a: Float): Color {
		this.r -= r
		this.g -= g
		this.b -= b
		this.a -= a
		return this
	}

	/**
	 * Multiplies this Color's color components by the given ones.
	 *
	 * @param r Red component
	 * @param g Green component
	 * @param b Blue component
	 * @param a Alpha component
	 *
	 * @return this Color for chaining
	 */
	fun mul(r: Float, g: Float, b: Float, a: Float): Color {
		this.r *= r
		this.g *= g
		this.b *= b
		this.a *= a
		return this
	}

	/**
	 * Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in this
	 * color.
	 * @param target The target color
	 * @param t The interpolation coefficient
	 * @return This color for chaining.
	 */
	fun lerp(target: ColorRo, t: Float): Color {
		this.r += t * (target.r - this.r)
		this.g += t * (target.g - this.g)
		this.b += t * (target.b - this.b)
		this.a += t * (target.a - this.a)
		return this
	}

	/**
	 * Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in this
	 * color.
	 * @param r The red component of the target color
	 * @param g The green component of the target color
	 * @param b The blue component of the target color
	 * @param a The alpha component of the target color
	 * @param t The interpolation coefficient
	 * @return This color for chaining.
	 */
	fun lerp(r: Float, g: Float, b: Float, a: Float, t: Float): Color {
		this.r += t * (r - this.r)
		this.g += t * (g - this.g)
		this.b += t * (b - this.b)
		this.a += t * (a - this.a)
		return this
	}

	fun invert() {
		r = 1 - r
		g = 1 - g
		b = 1 - b
	}

	/**
	 * Multiplies the RGB values by the alpha.
	 */
	fun premultiplyAlpha(): Color {
		r *= a
		g *= a
		b *= a
		return this
	}

	companion object {
		val CLEAR: ColorRo = Color(0f, 0f, 0f, 0f)
		val WHITE: ColorRo = Color(1f, 1f, 1f, 1f)
		val BLACK: ColorRo = Color(0f, 0f, 0f, 1f)
		val RED: ColorRo = Color(1f, 0f, 0f, 1f)
		val BROWN: ColorRo = Color(0.5f, 0.3f, 0f, 1f)
		val GREEN: ColorRo = Color(0f, 1f, 0f, 1f)
		val BLUE: ColorRo = Color(0f, 0f, 1f, 1f)
		val LIGHT_BLUE: ColorRo = Color(0.68f, 0.68f, 1f, 1f)
		val LIGHT_GRAY: ColorRo = Color(0.75f, 0.75f, 0.75f, 1f)
		val GRAY: ColorRo = Color(0.5f, 0.5f, 0.5f, 1f)
		val DARK_GRAY: ColorRo = Color(0.25f, 0.25f, 0.25f, 1f)
		val PINK: ColorRo = Color(1f, 0.68f, 0.68f, 1f)
		val ORANGE: ColorRo = Color(1f, 0.78f, 0f, 1f)
		val YELLOW: ColorRo = Color(1f, 1f, 0f, 1f)
		val MAGENTA: ColorRo = Color(1f, 0f, 1f, 1f)
		val CYAN: ColorRo = Color(0f, 1f, 1f, 1f)
		val OLIVE: ColorRo = Color(0.5f, 0.5f, 0f, 1f)
		val PURPLE: ColorRo = Color(0.5f, 0f, 0.5f, 1f)
		val MAROON: ColorRo = Color(0.5f, 0f, 0f, 1f)
		val TEAL: ColorRo = Color(0f, 0.5f, 0.5f, 1f)
		val NAVY: ColorRo = Color(0f, 0f, 0.5f, 1f)

		private val clamped = Color()

		/**
		 * Returns a new color from a hex string with one of the following formats (alpha is optional):
		 * RRGGBBAA,
		 * #RRGGBBAA
		 * 0xRRGGBBAA,
		 * rgb(255, 255, 255),
		 * rgba(255, 255, 255, 255),
		 * or #RRGGBBAA.
		 * @see [toRgbaString]
		 */
		fun fromStr(str: String): Color {
			return if (str.startsWith("0x")) fromRgbaStr(str.substring(2))
			else if (str.startsWith("#")) fromRgbaStr(str.substring(1))
			else if (str.startsWith("rgb", ignoreCase = true)) fromCssStr(str)
			else fromRgbaStr(str)
		}

		fun from8888Str(value: String): Color {
			val c = Color()
			c.set8888(value.trim().toLong())
			return c
		}

		fun from888Str(value: String): Color {
			val c = Color()
			c.set888(value.trim().toIntOrNull() ?: 0)
			return c
		}

		/**
		 * From rgb(255, 255, 255) or rgba(255, 255, 255, 1)
		 */
		fun fromCssStr(value: String): Color {
			val i = value.indexOf("(")
			if (i == -1) return Color.BLACK.copy()
			val sub = value.substring(i + 1, value.length - 1)
			val split = sub.split(',')
			val r = split[0].trim().toFloat() / 255f
			val g = split[1].trim().toFloat() / 255f
			val b = split[2].trim().toFloat() / 255f
			val a = if (split.size < 4) 1f else split[3].trim().toFloat()
			return Color(r, g, b, a)
		}

		/**
		 * Returns a new color from a hex string with the format RRGGBBAA.
		 * @see [toRgbaString]
		 */
		fun fromRgbaStr(hex: String): Color {
			val r = hex.substring(0, 2).toIntOrNull(16) ?: 0
			val g = hex.substring(2, 4).toIntOrNull(16) ?: 0
			val b = hex.substring(4, 6).toIntOrNull(16) ?: 0
			val a = if (hex.length != 8) 255 else hex.substring(6, 8).toIntOrNull(16) ?: 0
			return Color(r.toFloat() / 255f, g.toFloat() / 255f, b.toFloat() / 255f, a.toFloat() / 255f)
		}

		fun rgb888(r: Float, g: Float, b: Float): Int {
			return ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
		}

		fun rgba8888(r: Float, g: Float, b: Float, a: Float): Int {
			return ((r * 255).toInt() shl 24) or ((g * 255).toInt() shl 16) or ((b * 255).toInt() shl 8) or (a * 255).toInt()
		}

		fun rgb888(color: ColorRo): Int {
			clamped.set(color).clamp()
			return ((clamped.r * 255).toInt() shl 16) or ((clamped.g * 255).toInt() shl 8) or (clamped.b * 255).toInt()
		}

		fun rgba8888(color: ColorRo): Int {
			clamped.set(color).clamp()
			return ((clamped.r * 255).toInt() shl 24) or ((clamped.g * 255).toInt() shl 16) or ((clamped.b * 255).toInt() shl 8) or (clamped.a * 255).toInt()
		}
	}
}

/**
 * A read-only representation of an Hsl value.
 */
interface HslRo {
	val h: Float
	val s: Float
	val l: Float
	val a: Float

	fun copy(): Hsl {
		return Hsl().set(this)
	}
}

/**
 * Hue saturation luminance
 */
class Hsl(
		override var h: Float = 0f,
		override var s: Float = 0f,
		override var l: Float = 0f,
		override var a: Float = 1f
) : HslRo, Clearable {

	fun toRgb(out: Color): Color {
		out.a = a
		val c = (1f - abs(2f * l - 1f)) * s
		val x = c * (1f - abs((h / 60f) % 2f - 1f))
		val m = l - c / 2f
		if (h < 60f) {
			out.r = c + m
			out.g = x + m
			out.b = 0f + m
		} else if (h < 120f) {
			out.r = x + m
			out.g = c + m
			out.b = 0f + m
		} else if (h < 180f) {
			out.r = 0f + m
			out.g = c + m
			out.b = x + m
		} else if (h < 240f) {
			out.r = 0f + m
			out.g = x + m
			out.b = c + m
		} else if (h < 300f) {
			out.r = x + m
			out.g = 0f + m
			out.b = c + m
		} else {
			out.r = c + m
			out.g = 0f + m
			out.b = x + m
		}
		return out
	}

	override fun clear() {
		h = 0f
		s = 0f
		l = 0f
		a = 0f
	}

	fun set(other: HslRo): Hsl {
		h = other.h
		s = other.s
		l = other.l
		a = other.a
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is HslRo) return false
		if (h != other.h) return false
		if (s != other.s) return false
		if (l != other.l) return false
		if (a != other.a) return false

		return true
	}

	override fun hashCode(): Int {
		var result = h.hashCode()
		result = 31 * result + s.hashCode()
		result = 31 * result + l.hashCode()
		result = 31 * result + a.hashCode()
		return result
	}

}

interface HsvRo {
	val h: Float
	val s: Float
	val v: Float
	val a: Float

	fun copy(): Hsv {
		return Hsv().set(this)
	}

	fun toRgb(out: Color = Color()): Color {
		out.a = a
		val c = v * s
		val x = c * (1f - abs((h / 60f) % 2f - 1f))
		val m = v - c
		if (h < 60f) {
			out.r = c + m
			out.g = x + m
			out.b = 0f + m
		} else if (h < 120f) {
			out.r = x + m
			out.g = c + m
			out.b = 0f + m
		} else if (h < 180f) {
			out.r = 0f + m
			out.g = c + m
			out.b = x + m
		} else if (h < 240f) {
			out.r = 0f + m
			out.g = x + m
			out.b = c + m
		} else if (h < 300f) {
			out.r = x + m
			out.g = 0f + m
			out.b = c + m
		} else {
			out.r = c + m
			out.g = 0f + m
			out.b = x + m
		}
		return out
	}
}

/**
 * Hue saturation value
 */
class Hsv(
		override var h: Float = 0f,
		override var s: Float = 0f,
		override var v: Float = 0f,
		override var a: Float = 1f
) : HsvRo, Clearable {

	override fun clear() {
		h = 0f
		s = 0f
		v = 0f
		a = 0f
	}

	fun set(other: HsvRo): Hsv {
		h = other.h
		s = other.s
		v = other.v
		a = other.a
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is HsvRo) return false

		if (h != other.h) return false
		if (s != other.s) return false
		if (v != other.v) return false
		if (a != other.a) return false

		return true
	}

	override fun hashCode(): Int {
		var result = h.hashCode()
		result = 31 * result + s.hashCode()
		result = 31 * result + v.hashCode()
		result = 31 * result + a.hashCode()
		return result
	}
}

fun Writer.color(color: ColorRo) {
	string("#" + color.toRgbaString())
}

fun Writer.color(name: String, color: ColorRo) = property(name).color(color)

fun Reader.color(): Color? {
	val str = string() ?: return null
	return Color.fromStr(str)
}

fun Reader.color(name: String): Color? = get(name)?.color()

private fun Float.toOctet(): String {
	return (this * 255).toInt().toRadix(16).padStart(2, '0')
}