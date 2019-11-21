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
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.abs

@Serializable
data class PropertyTimeline(

		/**
		 * The name of the property this timeline controls.
		 */
		val property: String,

		/**
		 * A list of `[time, value0, value1, valueN, ... ]`
		 */
		val timeline: FloatArray,

		/**
		 * The number of values for each set in the timeline.
		 */
		val numComponents: Int = 1,

		/**
		 * If true, relative to the particle's lifespan, if false, relative to the emitter duration.
		 */
		val useEmitterDuration: Boolean = false,

		/**
		 * If true, the final value will not be the high value, but the high + low
		 */
		val relative: Boolean = false,

		/**
		 * When the values are initialized / reset for a new particle, this will be the low range.
		 */
		val low: FloatRange = FloatRange(1f),

		/**
		 * When the values are initialized / reset for a new particle, this will be the high range.
		 */
		val high: FloatRange = FloatRange(1f)
) {

	/**
	 * The number of elements between each index in [timeline].
	 */
	@Transient
	val stride: Int = numComponents + 1

	@Transient
	val size: Int = timeline.size / stride

	init {
		check(timeline.size % stride == 0) { "timeline should be divisible by stride <$stride>"}
	}

	@Transient
	val id: Int = nextId()

	/**
	 * Returns the insertion index of the given time.
	 *
	 * Example:
	 * If [stride] is 4, and timeline is [0f, v0_0, v_0_1, v_0_2, 3f, v1_0, v_1_1, v_1_2, 4f, v2_0, v_2_1, v_2_2]
	 * ```
	 * getInsertionIndex(0f) will return 1
	 * getInsertionIndex(1f) will return 1
	 * getInsertionIndex(3f) will return 2
	 * getInsertionIndex(4f) will return 2
	 * getInsertionIndex(5f) will return 2
	 * ```
	 */
	fun getInsertionIndex(time: Float, fromIndex: Int = 0, toIndex: Int = size): Int {
		var indexA = fromIndex
		var indexB = toIndex
		while (indexA < indexB) {
			val midIndex = (indexA + indexB) ushr 1
			if (time >= timeline[midIndex * stride]) {
				indexA = midIndex + 1
			} else {
				indexB = midIndex
			}
		}
		return indexA
	}

	fun getTime(index: Int): Float = timeline[index * stride]
	fun getValue(index: Int, offset: Int): Float = timeline[index * stride + offset + 1]

	/**
	 * Retrieves the values (including time) at the given index.
	 * @param out An array to populate with values. This should be of length [stride].
	 * @return Returns [out].
	 */
	fun getValues(index: Int, out: FloatArray): FloatArray {
		val p = index * stride
		timeline.copyInto(out, 0, p, p + stride)
		return out
	}

	fun getIndexCloseToTime(time: Float, affordance: Float = 0.02f): Int {
		val closestIndex = getIndexClosestToTime(time)
		if (closestIndex == -1) return -1
		val closestTime = timeline[closestIndex * stride]
		return if (closestTime.closeTo(time, affordance)) closestIndex else -1
	}

	fun getIndexClosestToTime(time: Float, fromIndex: Int = 0, toIndex: Int = size): Int {
		if (toIndex <= fromIndex) return -1
		if (toIndex == fromIndex + 1) return fromIndex
		val a = getInsertionIndex(time, fromIndex, toIndex)
		if (a <= 0) return 0
		val diffA = abs(time - timeline[a * stride])
		val diffB = abs(time - timeline[(a - 1) * stride])
		return if (diffA < diffB) a else a - 1
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is PropertyTimeline) return false
		if (id != other.id) return false
		return true
	}

	override fun hashCode(): Int {
		return id
	}


	companion object {
		private var _nextId = 0

		fun nextId(): Int {
			return ++_nextId
		}
	}
}