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

package com.acornui.math

import com.acornui.collection.Clearable

/**
 * A read-only view of a 2D range describing optional min and max boundaries.
 *
 * @author nbilyk
 */
interface Range2Ro<T: Comparable<T>> {

	val min: T?
	val max: T?

	fun contains(value: T): Boolean {
		if (min != null && min!! > value) return false
		if (max != null && max!! < value) return false
		return true
	}

	fun clamp(value:T?): T? {
		if (value == null) return null
		if (min != null && value < min!!) return min!!
		if (max != null && value > max!!) return max!!
		return value
	}

	fun clamp2(value:T): T {
		if (min != null && value < min!!) return min!!
		if (max != null && value > max!!) return max!!
		return value
	}

	fun copy(min: T? = this.min, max: T? = this.max): Range2<T> = Range2(min, max)
}

/**
 * A 2D range describing optional min and max boundaries.
 *
 * @author nbilyk
 */
class Range2<T: Comparable<T>> (
		override var min: T? = null,
		override var max: T? = null
) : Clearable, Range2Ro<T> {

	/**
	 * Use the max of the two min values and the min of the two max values.
	 * Sets the result on this Range2
	 * @return Returns this Range2 for chaining.
	 */
	fun bound(range:Range2Ro<T>): Range2<T> {
		if (min == null) min = range.min
		else if (range.min != null) min = maxOf(range.min!!, min!!)
		if (max == null) max = range.max
		else if (range.max != null) max = minOf(range.max!!, max!!)
		return this
	}

	fun set(other: Range2Ro<T>) {
		min = other.min
		max = other.max
	}

	override fun clear() {
		min = null
		max = null
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as Range2Ro<*>
		if (min != other.min) return false
		if (max != other.max) return false
		return true
	}

	override fun hashCode(): Int {
		var result = min?.hashCode() ?: 0
		result = 31 * result + (max?.hashCode() ?: 0)
		return result
	}


}