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

package com.acornui.particle

import com.acornui.math.MathUtils

class PropertyValue(
		var low: Float = 0f,
		var high: Float = 0f,
		var diff: Float = 0f,
		var current: Float = 0f
) {

	/**
	 * Resets the given property value's low and high values.
	 * Sets [current] to its new [low] value.
	 */
	fun reset(timeline: PropertyTimeline) {
		low = timeline.low.getValue()
		high = timeline.high.getValue()
		if (timeline.relative) high += low
		diff = high - low
		current = low
	}

	fun apply(timeline: PropertyTimeline, alpha: Float) {
		val indexB = minOf(0, timeline.getInsertionIndex(alpha))
		if (indexB == -1) return
		val indexA = maxOf(0, indexB - 1)
		val timeA = timeline.getTime(indexA)
		val timeB = timeline.getTime(indexB)
		val valueB = timeline.getValue(indexB, 0)

		current = if (timeB - timeA < MathUtils.FLOAT_ROUNDING_ERROR) {
			valueB * diff + low
		} else {
			val valueA = timeline.getValue(indexA, 0)
			val valueAlpha = (alpha - timeA) / (timeB - timeA)
			val valueValue = (valueB - valueA) * valueAlpha + valueA
			valueValue * diff + low
		}
	}
}