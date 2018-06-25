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

import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.MathUtils
import com.acornui.serialization.*

data class ColorTimeline(

		override val property: String,

		override val timeline: List<TimelineValue<ColorRo>>,

		/**
		 * If true, relative to the particle's lifespan, if false, relative to the emitter duration.
		 */
		val useParticleLife: Boolean

) : PropertyTimeline<ColorRo> {

	fun apply(target: Color, alpha: Float) {
		val n = timeline.size
		if (n == 0) return
		val timelineIndex = timeline.getIndexOfTime(alpha) - 1
		val timeA: Float
		val valueA: ColorRo
		if (timelineIndex < 0) {
			timeA = 0f
			valueA = timeline[0].value
		} else {
			val timelineEntry = timeline[timelineIndex]
			timeA = timelineEntry.time
			valueA = timelineEntry.value
		}

		val timeB: Float
		val valueB: ColorRo
		if (timelineIndex >= n - 1) {
			timeB = 1f
			valueB = timeline[timeline.lastIndex].value
		} else {
			val timelineEntry = timeline[timelineIndex + 1]
			timeB = timelineEntry.time
			valueB = timelineEntry.value
		}
		if (timeB - timeA < MathUtils.FLOAT_ROUNDING_ERROR) {
			target.set(valueB)
		} else {
			val valueAlpha = (alpha - timeA) / (timeB - timeA)
			target.set(valueA).lerp(valueB, valueAlpha)
		}
	}
}


object ColorTimelineSerializer : From<ColorTimeline>, To<ColorTimeline> {

	override fun read(reader: Reader): ColorTimeline {
		val timelineFloats = reader.floatArray("timeline") ?: floatArrayOf()
		val timeline = ArrayList<TimelineValue<ColorRo>>(timelineFloats.size shr 1)
		for (i in 0..timelineFloats.lastIndex step 4) {
			timeline.add(TimelineValue(timelineFloats[i], Color(timelineFloats[i + 1], timelineFloats[i + 2], timelineFloats[i + 3], 1f)))
		}

		return ColorTimeline(
				property = reader.string("property")!!,
				timeline = timeline,
				useParticleLife = reader.bool("useParticleLife") ?: true
		)
	}

	override fun ColorTimeline.write(writer: Writer) {
		writer.string("property", property)
		val timelineFloats = FloatArray(timeline.size shl 1)
		for (i in 0..timeline.lastIndex) {
			val j = i * 4
			val t = timeline[i]
			timelineFloats[j] = t.time
			timelineFloats[j + 1] = t.value.r
			timelineFloats[j + 2] = t.value.g
			timelineFloats[j + 3] = t.value.b
		}
		writer.floatArray("timeline", timelineFloats)
		writer.bool("useParticleLife", useParticleLife)
	}
}