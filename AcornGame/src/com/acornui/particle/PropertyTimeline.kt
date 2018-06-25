/*
 * Copyright 2018 Poly Forest
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

import com.acornui.collection.sortedInsertionIndex
import com.acornui.core.closeTo
import com.acornui.serialization.*
import kotlin.math.abs

interface PropertyTimeline<T> {

	val id: Int

	val property: String

	val timeline: List<TimelineValue<T>>

	fun getValueCloseToTime(time: Float, affordance: Float = 0.02f): TimelineValue<T>? {
		val closest = getValueClosestToTime(time) ?: return null
		return if (closest.time.closeTo(time, affordance)) closest else null
	}

	fun getValueClosestToTime(time: Float, startIndex: Int = 0, endIndex: Int = timeline.size): TimelineValue<T>? {
		var closestDelta: Float = Float.MAX_VALUE
		var closestValue: TimelineValue<T>? = null
		for (i in maxOf(0, startIndex)..minOf(timeline.size, endIndex) - 1) {
			val iValue = timeline[i]
			val delta = abs(time - iValue.time)
			if (delta < closestDelta) {
				closestDelta = delta
				closestValue = iValue
			}
		}
		return closestValue
	}

	companion object {
		private var _nextId = 0

		fun nextId(): Int {
			return ++_nextId
		}
	}
}

data class TimelineValue<T>(val time: Float, val value: T) : Comparable<TimelineValue<T>> {

	override fun compareTo(other: TimelineValue<T>): Int {
		return time.compareTo(other.time)
	}
}

private val comparator: (Float, TimelineValue<*>) -> Int = { time, element ->
	time.compareTo(element.time)
}

fun List<TimelineValue<*>>.getIndexOfTime(time: Float): Int {
	return sortedInsertionIndex(time, matchForwards = true, comparator = comparator)
}

object PropertyTimelineSerializer : From<PropertyTimeline<*>>, To<PropertyTimeline<*>> {

	override fun read(reader: Reader): PropertyTimeline<*> {
		val property = reader.string("property")
		return if (property == "color") ColorTimelineSerializer.read(reader)
		else FloatTimelineSerializer.read(reader)
	}

	override fun PropertyTimeline<*>.write(writer: Writer) {
		if (property == "color") ColorTimelineSerializer.write2(this as ColorTimeline, writer)
		else FloatTimelineSerializer.write2(this as FloatTimeline, writer)
	}
}