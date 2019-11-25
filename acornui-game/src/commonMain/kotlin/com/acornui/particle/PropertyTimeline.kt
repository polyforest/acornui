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

import com.acornui.closeTo
import com.acornui.collection.getInsertionIndex
import com.acornui.math.MathUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.abs

@Serializable
data class PropertyTimeline(

		val id: Int = nextId(),

		/**
		 * The name of the property this timeline controls.
		 */
		val property: String,

		/**
		 * A list of `[time, value0, value1, valueN, ... ]`
		 */
		val timeline: FloatArray = floatArrayOf(),

		/**
		 * The number of values for each set in the timeline.
		 */
		val numComponents: Int = 1,

		/**
		 * If true, relative to the particle's lifespan, if false, relative to the emitter duration.
		 */
		val useEmitterDuration: Boolean = false,

		/**
		 * If true, the final value will not be the high value, but the high + low.
		 */
		val relative: Boolean = false,

		/**
		 * When the values are initialized / reset for a new particle, this will be the low range.
		 */
		val low: FloatRange = FloatRange(0f),

		/**
		 * When the values are initialized / reset for a new particle, this will be the high range.
		 */
		val high: FloatRange = FloatRange(0f)
) {

	/**
	 * The number of elements between each index in [timeline].
	 */
	@Transient
	val stride: Int = numComponents + 1

	init {
		check(timeline.size % stride == 0) { "timeline should be divisible by stride <$stride>"}
	}

	override fun equals(other: Any?): Boolean {
		return this === other
	}

	override fun hashCode(): Int {
		return id
	}

	/**
	 * In the case where numComponents is 1, getValueAtTime will return the linearly interpolated value for the given
	 * time.
	 * If the timeline is empty, 0f will be returned.
	 * @param time The time position to interpolate between the two neighbor value sets.
	 * @return Returns the interpolated value.
	 */
	fun getValueAtTime(time: Float): Float {
		if (timeline.isEmpty()) return 0f
		val offset = 1
		var indexB = timeline.getInsertionIndex(time, stride)
		val indexA = maxOf(0, indexB - stride)
		if (indexB == timeline.size) indexB -= stride
		val timeA = timeline[indexA]
		val timeB = timeline[indexB]
		val valueA = timeline[indexA + offset]

		return if (timeB - timeA < MathUtils.FLOAT_ROUNDING_ERROR) {
			valueA
		} else {
			val valueB = timeline[indexB + offset]
			val valueAlpha = (time - timeA) / (timeB - timeA)
			val valueValue = (valueB - valueA) * valueAlpha + valueA
			valueValue
		}
	}

	/**
	 * In the case where numComponents is greater than of equal to 1, getValuesAtTime will set [out] with the linearly
	 * interpolated values for the given time.
	 * If the timeline is empty, the array will be filled with 0f
	 * @param time The time position to interpolate between the two neighbor value sets.
	 * @param out A FloatArray of size [numComponents] to be populated with the interpolated values.
	 */
	fun getValuesAtTime(time: Float, out: FloatArray = FloatArray(numComponents)): FloatArray {
		if (timeline.isEmpty()) {
			out.fill(0f)
			return out
		}
		var indexB = timeline.getInsertionIndex(time, stride)
		val indexA = maxOf(0, indexB - stride)
		if (indexB == timeline.size) indexB -= stride
		val timeA = timeline[indexA]
		val timeB = timeline[indexB]

		if (timeB - timeA < MathUtils.FLOAT_ROUNDING_ERROR) {
			for (i in 0..numComponents - 1) {
				out[i] = timeline[indexA + i + 1]
			}
		} else {
			for (i in 0..numComponents - 1) {
				val valueA = timeline[indexA + i + 1]
				val valueB = timeline[indexB + i + 1]
				val valueAlpha = (time - timeA) / (timeB - timeA)
				out[i] = (valueB - valueA) * valueAlpha + valueA
			}
		}
		return out
	}

	fun getIndexCloseToTime(time: Float, affordance: Float = 0.02f): Int {
		val closestIndex = getIndexClosestToTime(time)
		if (closestIndex == -1) return -1
		val closestTime = timeline[closestIndex]
		return if (closestTime.closeTo(time, affordance)) closestIndex else -1
	}

	fun getIndexClosestToTime(time: Float): Int {
		val a = timeline.getInsertionIndex(time, stride)
		if (a <= 0) return 0
		val b = a - stride
		val diffA = abs(time - timeline[a])
		val diffB = abs(time - timeline[b])
		return if (diffA < diffB) a else b
	}

	companion object {
		private var _nextId = 0

		fun nextId(): Int {
			return ++_nextId
		}
	}
}