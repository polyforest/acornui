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

package com.acornui.component

import com.acornui.collection.ArrayList
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleProperty
import com.acornui.core.radToDeg
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.color
import com.acornui.math.*
import com.acornui.serialization.*
import kotlin.math.atan2

open class BoxStyle : StyleBase() {

	override val type: StyleType<BoxStyle> = Companion

	var linearGradient: LinearGradient? by prop(null)
	var backgroundColor: ColorRo by prop(Color.BLACK)

	var borderColors: BorderColorsRo by prop(BorderColors())

	var borderThicknesses: PadRo by prop(Pad())

	var borderRadii: CornersRo by prop(Corners())

	var margin: PadRo by prop(Pad())
	var padding: PadRo by prop(Pad())

	companion object : StyleType<BoxStyle>
}

object BoxStyleSerializer : To<BoxStyle>, From<BoxStyle> {

	override fun BoxStyle.write(writer: Writer) {
		writer.styleProperty(this, ::linearGradient)?.obj(linearGradient, LinearGradientSerializer)
		writer.styleProperty(this, ::backgroundColor)?.color(backgroundColor)
		writer.styleProperty(this, ::borderColors)?.obj(borderColors, BorderColorsSerializer)
		writer.styleProperty(this, ::borderThicknesses)?.obj(borderThicknesses, PadSerializer)
		writer.styleProperty(this, ::borderRadii)?.obj(borderRadii, CornersSerializer)
		writer.styleProperty(this, ::margin)?.obj(margin, PadSerializer)
	}

	override fun read(reader: Reader): BoxStyle {
		val boxStyle = BoxStyle()
		read(reader, boxStyle)
		return boxStyle
	}

	fun read(reader: Reader, boxStyle: BoxStyle) {
		reader.contains(boxStyle::linearGradient.name) { boxStyle.linearGradient = it.obj(LinearGradientSerializer) }
		reader.contains(boxStyle::backgroundColor.name) { boxStyle.backgroundColor = it.color()!! }
		reader.contains(boxStyle::borderColors.name) { boxStyle.borderColors = it.obj(BorderColorsSerializer)!! }
		reader.contains(boxStyle::borderThicknesses.name) { boxStyle.borderThicknesses = it.obj(PadSerializer)!! }
		reader.contains(boxStyle::borderRadii.name) { boxStyle.borderRadii = it.obj(CornersSerializer)!! }
		reader.contains(boxStyle::margin.name) { boxStyle.margin = it.obj(PadSerializer)!! }
	}
}

fun boxStyle(init: BoxStyle.() -> Unit): BoxStyle {
	val b = BoxStyle()
	b.init()
	return b
}

data class LinearGradient(

		/**
		 * The direction the gradient should go. If direction is [GradientDirection.ANGLE], then the [angle] property
		 * will be used.
		 */
		val direction: GradientDirection,

		/**
		 * In radians, angles start pointing towards top, rotating clockwise.
		 * PI / 2 is pointing to the right, PI is pointing to bottom, 3 / 2f * PI is pointing to right.
		 */
		val angle: Float,

		val colorStops: List<ColorStop>
) {

	/**
	 * Creates a linear gradient where the color stops are distributed evenly for the given colors.
	 */
	constructor(direction: GradientDirection, vararg colors: ColorRo): this(direction, 0f, ArrayList(colors.size) { ColorStop(colors[it]) })

	constructor(direction: GradientDirection, vararg colorStops: ColorStop) : this(direction, 0f, colorStops.toList())

	constructor(angle: Float, vararg colorStops: ColorStop) : this(GradientDirection.ANGLE, angle, colorStops.toList())

	fun toCssString(): String {
		val angleStr = when (direction) {
			GradientDirection.TOP_LEFT -> "to left top"
			GradientDirection.TOP -> "to top"
			GradientDirection.TOP_RIGHT -> "to top right"
			GradientDirection.RIGHT -> "to right"
			GradientDirection.BOTTOM_RIGHT -> "to bottom right"
			GradientDirection.BOTTOM -> "to bottom"
			GradientDirection.BOTTOM_LEFT -> "to bottom left"
			GradientDirection.LEFT -> "to left"
			GradientDirection.ANGLE -> "${radToDeg(angle)}deg"
		}
		var colorStopsStr = ""
		for (colorStop in colorStops) {
			if (colorStopsStr != "") colorStopsStr += ", "
			colorStopsStr += colorStop.toCssString()
		}
		return "linear-gradient($angleStr, $colorStopsStr)"
	}


	fun getAngle(width: Float, height: Float): Float {
		return if (direction == GradientDirection.ANGLE) angle
		else direction.getAngle(width, height)
	}
}

object LinearGradientSerializer : To<LinearGradient>, From<LinearGradient> {

	override fun read(reader: Reader): LinearGradient {
		return LinearGradient(
				direction = GradientDirection.valueOf(reader.string("direction")!!),
				angle = reader.float("angle")!!,
				colorStops = reader.array2("colorStops", ColorStopSerializer)!!.toMutableList()
		)
	}

	override fun LinearGradient.write(writer: Writer) {
		writer.string("direction", direction.name)
		writer.float("angle", angle)
		writer.array("colorStops", colorStops, ColorStopSerializer)
	}
}

enum class GradientDirection {
	TOP_LEFT,
	TOP,
	TOP_RIGHT,
	RIGHT,
	BOTTOM_RIGHT,
	BOTTOM,
	BOTTOM_LEFT,
	LEFT,
	ANGLE;

	fun getAngle(width: Float, height: Float): Float {
		return when (this) {
			TOP_LEFT -> atan2(-height, width)
			TOP -> 0f
			TOP_RIGHT -> atan2(height, width)
			RIGHT -> 0.5f * PI
			BOTTOM_RIGHT -> atan2(height, -width)
			BOTTOM -> PI
			BOTTOM_LEFT -> atan2(-height, -width)
			LEFT -> 1.5f * PI
			ANGLE -> 0f
		}
	}
}

data class ColorStop(

		/**
		 * The color value at this stop.
		 */
		val color: ColorRo,

		/**
		 * The percent towards the end position of the gradient. This number is absolute, not relative to
		 * the previous color stop.
		 * If neither [pixels] or [percent] is set, this stop will be halfway between the previous stop and the next.
		 */
		val percent: Float? = null,

		/**
		 * The number of pixels towards the end position of the gradient. This number is absolute, not relative to
		 * the previous color stop.
		 * If neither [pixels] or [percent] is set, this stop will be halfway between the previous stop and the next.
		 */
		val pixels: Float? = null
) {

	constructor(rgba: Long, percent: Float? = null, pixels: Float? = null) : this(Color(rgba), percent, pixels)

	fun toCssString(): String {
		var str = color.toCssString()
		if (percent != null) str += " ${percent!! * 100f}%"
		else if (pixels != null) str += " ${pixels}px"
		return str
	}

}

object ColorStopSerializer : To<ColorStop>, From<ColorStop> {

	override fun read(reader: Reader): ColorStop {
		val c = ColorStop(
				color = reader.color("color")!!,
				percent = reader.float("percent"),
				pixels = reader.float("pixels")
		)
		return c
	}

	override fun ColorStop.write(writer: Writer) {
		writer.color("color", color)
		if (percent != null) writer.float("percent", percent)
		if (pixels != null) writer.float("pixels", pixels)
	}
}

interface BorderColorsRo {

	val top: ColorRo
	val right: ColorRo
	val bottom: ColorRo
	val left: ColorRo

	fun copy(top: ColorRo = this.top, right: ColorRo = this.right, bottom: ColorRo = this.bottom, left: ColorRo = this.left): BorderColors {
		return BorderColors(top.copy(), right.copy(), bottom.copy(), left.copy())
	}
}

data class BorderColors(
		override val top: Color,
		override val right: Color,
		override val bottom: Color,
		override val left: Color
) : BorderColorsRo {

	constructor(all: ColorRo) : this(all.copy(), all.copy(), all.copy(), all.copy()) {
		set(all)
	}

	constructor(
			top: ColorRo,
			right: ColorRo,
			bottom: ColorRo,
			left: ColorRo
	) : this(top.copy(), right.copy(), bottom.copy(), left.copy())

	constructor() : this(Color.CLEAR)

	fun set(all: ColorRo): BorderColors {
		top.set(all)
		right.set(all)
		bottom.set(all)
		left.set(all)
		return this
	}

	fun set(other: BorderColorsRo): BorderColors {
		top.set(other.top)
		right.set(other.right)
		bottom.set(other.bottom)
		left.set(other.left)
		return this
	}

	/**
	 * Multiplies all border colors by the given value.
	 */
	fun mul(value: ColorRo) {
		top.mul(value)
		right.mul(value)
		bottom.mul(value)
		left.mul(value)
	}
}

object BorderColorsSerializer : To<BorderColorsRo>, From<BorderColors> {
	override fun BorderColorsRo.write(writer: Writer) {
		writer.color("top", top)
		writer.color("right", right)
		writer.color("bottom", bottom)
		writer.color("left", left)
	}

	override fun read(reader: Reader): BorderColors {
		return BorderColors(
				reader.color("top")!!,
				reader.color("right")!!,
				reader.color("bottom")!!,
				reader.color("left")!!
		)
	}
}
