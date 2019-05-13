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

package com.acornui.graphic

import com.acornui.recycle.Clearable

/**
 *
 * Usage example:
 *
 *```
 * val b = BestSampler<String>(3)
 * c.sample(1f, "A")
 *
 * ```
 * @author nbilyk
 */
class BestSampler<T>(val count: Int) : Clearable {

	private val scores = FloatArray(count)
	private val _data = ArrayList<T>(count)

	private val data: List<T>
		get() = _data

	private var lowestIndex = 0
	private var highestValue = 0f
	private var lowestValue = 0f

	init {
		clear()
	}

	override fun clear() {
		_data.clear()
		lowestIndex = count - 1
		lowestValue = Float.POSITIVE_INFINITY
		highestValue = Float.NEGATIVE_INFINITY
	}

	fun sample(score: Float, value: T) {
		if (!isFull) {
			if (score < lowestValue) {
				lowestValue = score
				lowestIndex = _data.size
			}
			if (score > highestValue) {
				highestValue = score
			}
			scores[_data.size] = score
			_data.add(value)
		} else {
			if (score > lowestValue) {
				if (score > highestValue) {
					highestValue = score
				}
				scores[lowestIndex] = score
				_data[lowestIndex] = value
				lowestValue = score
				for (i in 0..count - 1) {
					// Find the new lowest sample.
					val sample = scores[i]
					if (sample < lowestValue) {
						lowestIndex = i
						lowestValue = sample
					}
				}
			}
		}
	}

	/**
	 * Returns true if the data list is [count] size.
	 */
	val isFull: Boolean
		get() = _data.size == count

}
