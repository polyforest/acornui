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

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.acornui.graphic

import com.acornui.closeTo
import com.acornui.graphic.Color.Companion.fromStr
import com.acornui.math.MathUtils.clamp
import com.acornui.recycle.Clearable
import com.acornui.serialization.Reader
import com.acornui.serialization.Writer
import com.acornui.string.toRadix
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlin.math.abs

@Serializable(with = ColorSerializer::class)
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
	 * rrggbb
	 */
	fun toRgbString(): String {
		return r.toOctet() + g.toOctet() + b.toOctet()
	}

	/**
	 * rrggbbaa
	 */
	fun toRgbaString(): String {
		return r.toOctet() + g.toOctet() + b.toOctet() + a.toOctet()
	}

	fun toHsl(out: Hsl = Hsl()): Hsl {
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

	fun toHsv(out: Hsv = Hsv()): Hsv {
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
@Serializable(with = ColorSerializer::class)
data class Color(
		override var r: Float = 0f,
		override var g: Float = 0f,
		override var b: Float = 0f,
		override var a: Float = 0f
) : ColorRo, Clearable {

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
	 * Multiplies this color and the given color
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
	fun set(r: Float = this.r, g: Float = this.g, b: Float = this.b, a: Float = this.a): Color {
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
		val LIGHT_RED: ColorRo = Color(1f, 0.68f, 0.68f, 1f)
		val BROWN: ColorRo = Color(0.5f, 0.3f, 0f, 1f)
		val GREEN: ColorRo = Color(0f, 1f, 0f, 1f)
		val LIGHT_GREEN: ColorRo = Color(0.68f, 1f, 0.68f, 1f)
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
		val INDIGO: ColorRo = Color(0.29f, 0f, 0.51f, 1f)
		val MAROON: ColorRo = Color(0.5f, 0f, 0f, 1f)
		val TEAL: ColorRo = Color(0f, 0.5f, 0.5f, 1f)
		val NAVY: ColorRo = Color(0f, 0f, 0.5f, 1f)

		/**
		 * Mask to prevent color from converting to NaN range.
		 */
		private const val floatBitsMask = 0xfeffffff.toInt()

		private val clamped = Color()

		/**
		 * Returns a new color from a hex string with one of the following formats (alpha is optional):
		 * RRGGBBAA,
		 * #RRGGBBAA
		 * 0xRRGGBBAA,
		 * rgb(255, 255, 255),
		 * rgba(255, 255, 255, 255),
		 * or by name (blue)
		 * @see [toRgbaString]
		 */
		fun fromStr(str: String): Color {
			return if (str.startsWith("0x")) fromRgbaStr(str.substring(2))
			else if (str.startsWith("#")) fromRgbaStr(str.substring(1))
			else if (str.startsWith("rgb", ignoreCase = true)) fromCssStr(str)
			else fromName(str)?.copy() ?: fromRgbaStr(str)
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
			if (i == -1) return BLACK.copy()
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

		private val nameColorMap = hashMapOf(
				"clear" to CLEAR,
				"white" to WHITE,
				"black" to BLACK,
				"red" to RED,
				"light-red" to LIGHT_RED,
				"brown" to BROWN,
				"green" to GREEN,
				"light-green" to LIGHT_GREEN,
				"blue" to BLUE,
				"light-blue" to LIGHT_BLUE,
				"gray" to GRAY,
				"grey" to GRAY,
				"light-gray" to LIGHT_GRAY,
				"light-grey" to LIGHT_GRAY,
				"dark-gray" to DARK_GRAY,
				"dark-grey" to DARK_GRAY,
				"pink" to PINK,
				"orange" to ORANGE,
				"yellow" to YELLOW,
				"magenta" to MAGENTA,
				"cyan" to CYAN,
				"olive" to OLIVE,
				"purple" to PURPLE,
				"indigo" to INDIGO,
				"maroon" to MAROON,
				"teal" to TEAL,
				"navy" to NAVY
		)

		/**
		 * Returns the color for the given name.
		 * The only colors supported are the constants on the Color companion object.
		 */
		fun fromName(name: String): ColorRo? {
			return nameColorMap[name.toLowerCase().trim()]
		}
	}
}

@Serializer(forClass = Color::class)
object ColorSerializer : KSerializer<Color> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("Color")

	override fun serialize(encoder: Encoder, obj: Color) {
		encoder.encodeString("#" + obj.toRgbaString())
	}

	override fun deserialize(decoder: Decoder): Color {
		return fromStr(decoder.decodeString())
	}
}

/**
 * Calculates a luminance value weighting the rgb components based on how the human eye sees them.
 * This is unrelated to HSL Lightness.  [Hsl.l]
 */
val ColorRo.luminancePerceived: Float
	get() {
		return r * 0.299f + g * 0.587f + b * 0.114f
	}

fun Writer.color(color: ColorRo) {
	string("#" + color.toRgbaString())
}

fun Writer.color(name: String, color: ColorRo) = property(name).color(color)

fun Reader.color(): Color? {
	val str = string() ?: return null
	return fromStr(str)
}

fun Reader.color(name: String): Color? = get(name)?.color()

private fun Float.toOctet(): String {
	return clamp(this * 255, 0f, 255f).toInt().toRadix(16).padStart(2, '0')
}

val colorValidationRegex = Regex("""^(#|0x)?([\da-fA-F]{2})([\da-fA-F]{2})([\da-fA-F]{2})([\da-fA-F]{2})?${'$'}""")

/**
 * First validates that the given string is parsable, and returns the [fromStr] value if it is.
 * If the string is not a valid color, returns null.
 */
fun String.toColorOrNull(): Color? {
	return if (colorValidationRegex.matches(this)) fromStr(this)
	else null
}
