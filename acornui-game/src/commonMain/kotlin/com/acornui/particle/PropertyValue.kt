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

	/**
	 * Sets the current value to the interpolated value from the given timeline.
	 * It's assumed that the timeline has numComponents == 1
	 */
	fun setCurrent(timeline: PropertyTimeline, time: Float) {
		current = timeline.getValueAtTime(time) * diff + low
	}
}