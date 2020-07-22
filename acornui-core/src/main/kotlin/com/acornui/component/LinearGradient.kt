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

package com.acornui.component

import com.acornui.collection.produceList
import com.acornui.graphic.Color
import com.acornui.math.PI
import com.acornui.number.radToDeg
import kotlinx.serialization.Serializable
import kotlin.math.atan2


@Serializable
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
	val angle: Double,

	val colorStops: List<ColorStop>
) {

	/**
	 * Creates a linear gradient where the color stops are distributed evenly for the given colors.
	 */
	constructor(direction: GradientDirection, vararg colors: Color): this(direction, 0.0, produceList(colors.size) { ColorStop(colors[it]) })

	constructor(direction: GradientDirection, vararg colorStops: ColorStop) : this(direction, 0.0, colorStops.toList())

	constructor(angle: Double, vararg colorStops: ColorStop) : this(GradientDirection.ANGLE, angle, colorStops.toList())

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


	fun getAngle(width: Double, height: Double): Double {
		return if (direction == GradientDirection.ANGLE) angle
		else direction.getAngle(width, height)
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

	fun getAngle(width: Double, height: Double): Double {
		return when (this) {
			TOP_LEFT -> atan2(-height, width)
			TOP -> 0.0
			TOP_RIGHT -> atan2(height, width)
			RIGHT -> 0.5f * PI
			BOTTOM_RIGHT -> atan2(height, -width)
			BOTTOM -> PI
			BOTTOM_LEFT -> atan2(-height, -width)
			LEFT -> 1.5f * PI
			ANGLE -> 0.0
		}
	}
}

@Serializable
data class ColorStop(

	/**
	 * The color value at this stop.
	 */
	val color: Color,

	/**
	 * The percent towards the end position of the gradient. This number is absolute, not relative to
	 * the previous color stop.
	 * If neither [dp] or [percent] is set, this stop will be halfway between the previous stop and the next.
	 */
	val percent: Double? = null,

	/**
	 * The number of dips towards the end position of the gradient. This number is absolute, not relative to
	 * the previous color stop.
	 * If neither [dp] or [percent] is set, this stop will be halfway between the previous stop and the next.
	 */
	val dp: Double? = null
) {

	constructor(rgba: Long, dp: Double? = null, pixels: Double? = null) : this(Color(rgba), dp, pixels)

	fun toCssString(): String {
		var str = color.toCssString()
		if (percent != null) str += " ${percent * 100.0}%"
		else if (dp != null) str += " ${dp}px"
		return str
	}

}
