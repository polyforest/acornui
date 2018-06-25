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

import com.acornui.math.MathUtils
import com.acornui.serialization.*

data class FloatTimeline(

		override val property: String,

		/**
		 * If true, the final value will not be the high value, but the high + low
		 */
		val relative: Boolean,

		val low: FloatRange,

		val high: FloatRange,

		/**
		 * A list of control points for interpolated values. If this is empty, the low value will always be used.
		 */
		override val timeline: List<TimelineValue<Float>>,

		/**
		 * If true, relative to the particle's lifespan, if false, relative to the emitter duration.
		 */
		val useParticleLife: Boolean

) : PropertyTimeline<Float> {

	fun reset(target: PropertyValue) {
		target.low = low.getValue()
		target.high = high.getValue()
		if (relative) target.high += target.low
		target.diff = target.high - target.low
		target.current = target.low
	}

	fun apply(target: PropertyValue, alpha: Float) {
		val n = timeline.size
		if (n == 0) return
		val timelineIndex = timeline.getIndexOfTime(alpha) - 1
		val timeA: Float
		val valueA: Float
		if (timelineIndex < 0) {
			timeA = 0f
			valueA = timeline[0].value
		} else {
			val timelineEntry = timeline[timelineIndex]
			timeA = timelineEntry.time
			valueA = timelineEntry.value
		}

		val timeB: Float
		val valueB: Float
		if (timelineIndex >= n - 1) {
			timeB = 1f
			valueB = timeline[timeline.lastIndex].value
		} else {
			val timelineEntry = timeline[timelineIndex + 1]
			timeB = timelineEntry.time
			valueB = timelineEntry.value
		}
		if (timeB - timeA < MathUtils.FLOAT_ROUNDING_ERROR) {
			target.current = valueB * target.diff + target.low
		} else {
			val valueAlpha = (alpha - timeA) / (timeB - timeA)
			val valueValue = (valueB - valueA) * valueAlpha + valueA
			target.current = valueValue * target.diff + target.low
		}
	}
}

class PropertyValue(
		var low: Float = 0f,
		var high: Float = 0f,
		var diff: Float = 0f,
		var current: Float = 0f
)


object FloatTimelineSerializer : From<FloatTimeline>, To<FloatTimeline> {

	override fun read(reader: Reader): FloatTimeline {
		val timelineFloats = reader.floatArray("timeline") ?: floatArrayOf()
		val timeline = ArrayList<TimelineValue<Float>>(timelineFloats.size shr 1)
		for (i in 0..timelineFloats.lastIndex step 2) {
			timeline.add(TimelineValue(timelineFloats[i], timelineFloats[i + 1]))
		}

		return FloatTimeline(
				property = reader.string("property")!!,
				relative = reader.bool("relative") ?: false,
				low = reader.obj("min", FloatRangeSerializer)!!,
				high = reader.obj("max", FloatRangeSerializer)!!,
				timeline = timeline,
				useParticleLife = reader.bool("useParticleLife") ?: true
		)
	}

	override fun FloatTimeline.write(writer: Writer) {
		writer.obj("min", low, FloatRangeSerializer)
		writer.obj("max", high, FloatRangeSerializer)
		writer.string("property", property)
		writer.bool("relative", relative)
		val timelineFloats = FloatArray(timeline.size shl 1)
		for (i in 0..timeline.lastIndex) {
			val j = i shl 1
			val t = timeline[i]
			timelineFloats[j] = t.time
			timelineFloats[j + 1] = t.value
		}
		writer.floatArray("timeline", timelineFloats)
		writer.bool("useParticleLife", useParticleLife)
	}
}