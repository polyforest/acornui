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

import com.acornui.number.closeTo
import com.acornui.graphic.Color.Companion.fromStr
import com.acornui.math.clamp
import com.acornui.serialization.Reader
import com.acornui.serialization.Writer
import com.acornui.string.toRadix
import kotlinx.serialization.*
import kotlin.math.abs

/**
 * A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the
 * internal values after execution.
 *
 * @author mzechner
 */
@Serializable(with = ColorSerializer::class)
data class Color(
	val r: Double = 0.0,
	val g: Double = 0.0,
	val b: Double = 0.0,
	val a: Double = 0.0
) {

	constructor(rgba: Long) : this(
		((rgba and 0xff000000).ushr(24)).toDouble() / 255.0,
		((rgba and 0x00ff0000).ushr(16)).toDouble() / 255.0,
		((rgba and 0x0000ff00).ushr(8)).toDouble() / 255.0,
		((rgba and 0x000000ff)).toDouble() / 255.0
	)

	/**
	 * Creates a Color object through an integer representation.
	 * 4294967295 - white
	 *
	 * @return this Color for chaining
	 * @see [rgba8888]
	 */
	constructor(rgba: Int) : this(
		((rgba and -16777216).ushr(24)).toDouble() / 255.0,
		((rgba and 0x00ff0000).ushr(16)).toDouble() / 255.0,
		((rgba and 0x0000ff00).ushr(8)).toDouble() / 255.0,
		((rgba and 0x000000ff)).toDouble() / 255.0
	)

	/**
	 * Multiplies the r, g, and b components by [value].
	 * Does not multiply the alpha component.
	 */
	operator fun times(value: Double): Color {
		if (value == 1.0) return this
		return Color(r * value, g * value, b * value, a)
	}

	/**
	 * Multiplies this Color's color components by the given values.
	 */
	fun times(r: Double = 1.0, g: Double = 1.0, b: Double = 1.0, a: Double = 1.0): Color {
		if (r == 1.0 && g == 1.0 && b == 1.0 && a == 1.0) return this
		return Color(this.r * r, this.g * g, this.b * b, this.a * a)
	}

	operator fun times(value: Color): Color {
		if (value == WHITE) return this
		return Color(r * value.r, g * value.g, b * value.b, a * value.a)
	}

	operator fun plus(other: Color): Color {
		if (other == CLEAR) return this
		return Color(r + other.r, g + other.g, b + other.b, a + other.a)
	}

	fun plus(r: Double = 0.0, g: Double = 0.0, b: Double = 0.0, a: Double = 0.0): Color {
		if (r == 0.0 && g == 0.0 && b == 0.0 && a == 0.0) return this
		return Color(this.r + r, this.g + g, this.b + b, this.a + a)
	}

	operator fun minus(other: Color): Color {
		if (other == CLEAR) return this
		return Color(r - other.r, g - other.g, b - other.b, a - other.a)
	}

	fun minus(r: Double = 0.0, g: Double = 0.0, b: Double = 0.0, a: Double = 0.0): Color {
		if (r == 0.0 && g == 0.0 && b == 0.0 && a == 0.0) return this
		return Color(this.r - r, this.g - g, this.b - b, this.a - a)
	}

	/**
	 * rgba(255, 255, 255, 1)
	 */
	fun toCssString(): String =
		"rgba(${(r * 255).toInt()}, ${(g * 255).toInt()}, ${(b * 255).toInt()}, $a)"

	/**
	 * Returns a String in the form of: "rrggbb"
	 */
	fun toRgbString(): String = r.toOctet() + g.toOctet() + b.toOctet()

	/**
	 * Returns a String in the form of: "rrggbbaa"
	 */
	fun toRgbaString(): String {
		return r.toOctet() + g.toOctet() + b.toOctet() + a.toOctet()
	}

	/**
	 * Returns this color as a hue saturation luminance object.
	 */
	fun toHsl(): Hsl {
		check(isClamped) { "Color must be clamped first." }
		val max = maxOf(r, g, b)
		val min = minOf(r, g, b)
		val l = (max + min) * 0.5
		var h: Double
		val s: Double

		val d = max - min
		if (d == 0.0) {
			h = 0.0
			s = 0.0
		} else {
			h = when (max) {
				r -> 60.0 * ((g - b) / d)
				g -> 60.0 * ((b - r) / d + 2.0)
				b -> 60.0 * ((r - g) / d + 4.0)
				else -> error("unreachable")
			}
			if (h < 0.0) h += 360.0
			if (h >= 360.0) h -= 360.0
			s = d / (1.0 - abs(2 * l - 1.0))
		}
		return Hsl(h, s, l, a)
	}

	/**
	 * Returns this color as a hue saturation value object.
	 */
	fun toHsv(): Hsv {
		check(isClamped) { "Color must be clamped first." }
		val max = maxOf(r, g, b)
		val min = minOf(r, g, b)
		var h: Double
		val s: Double

		val d = max - min
		if (d == 0.0) {
			h = 0.0
			s = 0.0
		} else {
			h = when (max) {
				r -> 60.0 * ((g - b) / d)
				g -> 60.0 * ((b - r) / d + 2.0)
				b -> 60.0 * ((r - g) / d + 4.0)
				else -> error("unreachable")
			}
			if (h < 0.0) h += 360.0
			if (h >= 360.0) h -= 360.0
			s = if (max == 0.0) 0.0
			else d / max
		}
		return Hsv(h, s, max, a)
	}

	/**
	 * Returns true if each of the channels is close to the other color within the given tolerance.
	 */
	fun closeTo(other: Color, tolerance: Double = 0.0001): Boolean {
		if (this === other) return true
		if (!r.closeTo(other.r, tolerance)) return false
		if (!g.closeTo(other.g, tolerance)) return false
		if (!b.closeTo(other.b, tolerance)) return false
		if (!a.closeTo(other.a, tolerance)) return false
		return true
	}

	/**
	 * Returns true if each component is within 0.0 (inclusive) and 1.0 (inclusive)
	 */
	val isClamped: Boolean
		get() = r in 0.0..1.0 && g in 0.0..1.0 && b in 0.0..1.0 && a in 0.0..1.0

	/**
	 * Clamps this Color's components to a valid range [0 - 1]
	 */
	fun clamp(): Color {
		return if (isClamped) this else
			Color(
				clamp(r, 0.0, 1.0),
				clamp(g, 0.0, 1.0),
				clamp(b, 0.0, 1.0),
				clamp(a, 0.0, 1.0)
			)
	}


	/**
	 * Linearly interpolates between this color and the target color by t which is in the range [0,1].
	 * @param target The target color
	 * @param alpha The interpolation coefficient
	 */
	fun lerp(target: Color, alpha: Double): Color =
		this * (1.0 - alpha) + target * alpha

	/**
	 * Returns this color where the r, g, and b values are inverted. (1 - v)
	 */
	fun invert(): Color =
		Color(1.0 - r, 1.0 - g, 1.0 - b, a)

	/**
	 * Multiplies the RGB values by the alpha.
	 */
	fun premultiplyAlpha(): Color =
		Color(r * a, g * a, b * a, a)

	companion object {
		val CLEAR = Color(0.0, 0.0, 0.0, 0.0)
		val WHITE = Color(1.0, 1.0, 1.0, 1.0)
		val BLACK = Color(0.0, 0.0, 0.0, 1.0)
		val RED = Color(1.0, 0.0, 0.0, 1.0)
		val LIGHT_RED = Color(1.0, 0.68, 0.68, 1.0)
		val BROWN = Color(0.5, 0.3, 0.0, 1.0)
		val GREEN = Color(0.0, 1.0, 0.0, 1.0)
		val LIGHT_GREEN = Color(0.68, 1.0, 0.68, 1.0)
		val BLUE = Color(0.0, 0.0, 1.0, 1.0)
		val LIGHT_BLUE = Color(0.68, 0.68, 1.0, 1.0)
		val LIGHT_GRAY = Color(0.75, 0.75, 0.75, 1.0)
		val GRAY = Color(0.5, 0.5, 0.5, 1.0)
		val DARK_GRAY = Color(0.25, 0.25, 0.25, 1.0)
		val PINK = Color(1.0, 0.68, 0.68, 1.0)
		val ORANGE = Color(1.0, 0.78, 0.0, 1.0)
		val YELLOW = Color(1.0, 1.0, 0.0, 1.0)
		val MAGENTA = Color(1.0, 0.0, 1.0, 1.0)
		val CYAN = Color(0.0, 1.0, 1.0, 1.0)
		val OLIVE = Color(0.5, 0.5, 0.0, 1.0)
		val PURPLE = Color(0.5, 0.0, 0.5, 1.0)
		val INDIGO = Color(0.29, 0.0, 0.51, 1.0)
		val MAROON = Color(0.5, 0.0, 0.0, 1.0)
		val TEAL = Color(0.0, 0.5, 0.5, 1.0)
		val NAVY = Color(0.0, 0.0, 0.5, 1.0)

		/**
		 * Mask to prevent color from converting to NaN range.
		 */
		private const val floatBitsMask = 0xfeffffff.toInt()

		/**
		 * Returns a new color from a hex string with one of the following formats (alpha is optional):
		 * RRGGBBAA,
		 * #RRGGBBAA
		 * 0xRRGGBBAA,
		 * rgb(255, 255, 255),
		 * rgba(255, 255, 255, 255),
		 * or by name (blue)
		 * @see [getToRgbaString]
		 */
		fun fromStr(str: String): Color {
			return if (str.startsWith("0x")) fromRgbaStr(str.substring(2))
			else if (str.startsWith("#")) fromRgbaStr(str.substring(1))
			else if (str.startsWith("rgb", ignoreCase = true)) fromCssStr(str)
			else fromName(str)?.copy() ?: fromRgbaStr(str)
		}

		fun from8888Str(value: String): Color =
			Color(value.trim().toLong())

		fun from888Str(value: String): Color =
			from888(value.trim().toIntOrNull() ?: 0)

		/**
		 * From rgb(255, 255, 255) or rgba(255, 255, 255, 1)
		 */
		fun fromCssStr(value: String): Color {
			val i = value.indexOf("(")
			if (i == -1) return BLACK.copy()
			val sub = value.substring(i + 1, value.length - 1)
			val split = sub.split(',')
			val r = split[0].trim().toDouble() / 255.0
			val g = split[1].trim().toDouble() / 255.0
			val b = split[2].trim().toDouble() / 255.0
			val a = if (split.size < 4) 1.0 else split[3].trim().toDouble()
			return Color(r, g, b, a)
		}

		/**
		 * Returns a new color from a hex string with the format RRGGBBAA.
		 * @see [getToRgbaString]
		 */
		fun fromRgbaStr(hex: String): Color {
			val r = hex.substring(0, 2).toIntOrNull(16) ?: 0
			val g = hex.substring(2, 4).toIntOrNull(16) ?: 0
			val b = hex.substring(4, 6).toIntOrNull(16) ?: 0
			val a = if (hex.length != 8) 255 else hex.substring(6, 8).toIntOrNull(16) ?: 0
			return Color(r.toDouble() / 255.0, g.toDouble() / 255.0, b.toDouble() / 255.0, a.toDouble() / 255.0)
		}

		fun rgb888(r: Double, g: Double, b: Double): Int {
			return ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
		}

		fun rgba8888(r: Double, g: Double, b: Double, a: Double): Int {
			return ((r * 255).toInt() shl 24) or ((g * 255).toInt() shl 16) or ((b * 255).toInt() shl 8) or (a * 255).toInt()
		}

		fun rgb888(color: Color): Int {
			val clamped = color.clamp()
			return ((clamped.r * 255).toInt() shl 16) or ((clamped.g * 255).toInt() shl 8) or (clamped.b * 255).toInt()
		}

		fun rgba8888(color: Color): Int {
			val clamped = color.clamp()
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
		fun fromName(name: String): Color? {
			return nameColorMap[name.toLowerCase().trim()]
		}


		/**
		 * Sets this color's component values through an integer representation.
		 * 16777215 - white
		 */
		fun from888(rgb: Int): Color = Color(
			r = ((rgb and 0xff0000).ushr(16)).toDouble() / 255.0,
			g = ((rgb and 0x00ff00).ushr(8)).toDouble() / 255.0,
			b = ((rgb and 0x0000ff)).toDouble() / 255.0,
			a = 1.0
		)
	}

}

@Serializer(forClass = Color::class)
object ColorSerializer : KSerializer<Color> {

	override val descriptor: SerialDescriptor = PrimitiveDescriptor("Color", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: Color) {
		encoder.encodeString("#" + value.toRgbaString())
	}

	override fun deserialize(decoder: Decoder): Color {
		return fromStr(decoder.decodeString())
	}
}

/**
 * Calculates a luminance value weighting the rgb components based on how the human eye sees them.
 * This is unrelated to HSL Lightness.  [Hsl.l]
 */
val Color.luminancePerceived: Double
	get() {
		return r * 0.299 + g * 0.587 + b * 0.114
	}

fun Writer.color(color: Color) {
	string("#" + color.toRgbaString())
}

fun Writer.color(name: String, color: Color) = property(name).color(color)

fun Reader.color(): Color? {
	val str = string() ?: return null
	return fromStr(str)
}

fun Reader.color(name: String): Color? = get(name)?.color()

private fun Double.toOctet(): String {
	return clamp(this * 255, 0.0, 255.0).toInt().toRadix(16).padStart(2, '0')
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
