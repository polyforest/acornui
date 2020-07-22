/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.css

import kotlinx.serialization.*

@Serializable(with = LengthSerializer::class)
class Length(private val value: String) {

	override fun toString(): String = value

	operator fun minus(other: Length): Length {
		return Length("calc($this - $other)")
	}

	operator fun plus(other: Length): Length {
		return Length("calc($this + $other)")
	}

	operator fun times(other: Double): Length {
		return Length("calc($this * $other)")
	}

	operator fun div(other: Double): Length {
		return Length("calc($this / $other)")
	}
}

@Serializer(forClass = Length::class)
object LengthSerializer : KSerializer<Length> {

	override val descriptor: SerialDescriptor = PrimitiveDescriptor("Length", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: Length) {
		encoder.encodeString(value.toString())
	}

	override fun deserialize(decoder: Decoder): Length {
		return Length(decoder.decodeString())
	}
}


fun String.toLengthOrNull(): Length? {
	if (isEmpty()) return null
	return Length(this)
}

/**
 * centimeters
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.cm: Length
	get() = Length("${this}cm")

/**
 * millimeters
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.mm: Length
	get() = Length("${this}mm")

/**
 * inches
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.inches: Length
	get() = Length("${this}in")

/**
 * pixels
 * Pixels (px) are relative to the viewing device. For low-dpi devices, 1px is one device pixel (dot) of the display.
 * For printers and high resolution screens 1px implies multiple device pixels.
 */
val Double.px: Length
	get() = Length("${this}px")

/**
 * points (1pt = 1/72 of 1in)
 *
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.pt: Length
	get() = Length("${this}pt")

/**
 * picas (1pc = 12 pt)
 *
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Double.pc: Length
	get() = Length("${this}pc")

/**
 * Relative to the font-size of the element (2em means 2 times the size of the current font)
 */
val Double.em: Length
	get() = Length("${this}em")

/**
 * Relative to the x-height of the current font (rarely used)
 */
val Double.ex: Length
	get() = Length("${this}ex")

/**
 * Relative to the width of the "0" (zero)
 */
val Double.ch: Length
	get() = Length("${this}ch")

/**
 * Relative to font-size of the root element
 */
val Double.rem: Length
	get() = Length("${this}rem")

/**
 * Relative to 1% of the width of the viewport
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Double.vw: Length
	get() = Length("${this}vw")

/**
 * Relative to 1% of the height of the viewport
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Double.vh: Length
	get() = Length("${this}vh")

/**
 * Relative to 1% of viewport's smaller dimension
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Double.vmin: Length
	get() = Length("${this}vmin")

/**
 * Relative to 1% of viewport's larger dimension
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Double.vmax: Length
	get() = Length("${this}vmax")

/**
 * Relative to the parent element
 */
val Double.percent: Length
	get() = Length("${this}%")


// Int

/**
 * centimeters
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.cm: Length
	get() = Length("${this}cm")

/**
 * millimeters
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.mm: Length
	get() = Length("${this}mm")

/**
 * inches
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.inches: Length
	get() = Length("${this}in")

/**
 * pixels
 * Pixels (px) are relative to the viewing device. For low-dpi devices, 1px is one device pixel (dot) of the display.
 * For printers and high resolution screens 1px implies multiple device pixels.
 */
val Int.px: Length
	get() = Length("${this}px")

/**
 * points (1pt = 1/72 of 1in)
 *
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.pt: Length
	get() = Length("${this}pt")

/**
 * picas (1pc = 12 pt)
 *
 * Absolute length units are not recommended for use on screen, because screen sizes vary so much. However, they can be
 * used if the output medium is known, such as for print layout.
 */
val Int.pc: Length
	get() = Length("${this}pc")

/**
 * Relative to the font-size of the element (2em means 2 times the size of the current font)
 */
val Int.em: Length
	get() = Length("${this}em")

/**
 * Relative to the x-height of the current font (rarely used)
 */
val Int.ex: Length
	get() = Length("${this}ex")

/**
 * Relative to the width of the "0" (zero)
 */
val Int.ch: Length
	get() = Length("${this}ch")

/**
 * Relative to font-size of the root element
 */
val Int.rem: Length
	get() = Length("${this}rem")

/**
 * Relative to 1% of the width of the viewport
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Int.vw: Length
	get() = Length("${this}vw")

/**
 * Relative to 1% of the height of the viewport
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Int.vh: Length
	get() = Length("${this}vh")

/**
 * Relative to 1% of viewport's smaller dimension
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Int.vmin: Length
	get() = Length("${this}vmin")

/**
 * Relative to 1% of viewport's larger dimension
 * Viewport = the browser window size. If the viewport is 50cm wide, 1vw = 0.5cm.
 */
val Int.vmax: Length
	get() = Length("${this}vmax")

/**
 * Relative to the parent element
 */
val Int.percent: Length
	get() = Length("${this}%")

operator fun Double.times(other: Length): Length {
	return Length("calc($this * $other)")
}

operator fun Double.div(other: Length): Length {
	return Length("calc($this / $other)")
}