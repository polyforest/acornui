/*
 * Copyright 2018 Nicholas Bilyk
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
import com.acornui.core.UidUtil
import com.acornui.core.graphics.BlendMode
import com.acornui.serialization.*

data class ParticleEffect(

		val emitters: List<ParticleEmitter>
) {
	fun createInstance(): ParticleEffectInstance {
		val emitterInstances = ArrayList<ParticleEmitterInstance>(emitters.size)
		for (i in 0..emitters.lastIndex) {
			val emitterInstance = emitters[i].createInstance()
			emitterInstances.add(emitterInstance)
		}
		return ParticleEffectInstance(emitterInstances)
	}
}

data class ParticleEmitter(

		val id: String = UidUtil.createUid(),

		val name: String,

		val enabled: Boolean,

		/**
		 * If false, this emitter will not loop after the total duration.
		 */
		val loops: Boolean,

		/**
		 * Represents when and how long this emitter will be active.
		 */
		val duration: EmitterDuration,

		/**
		 * The maximum number of particles to create.
		 */
		val count: Int,

		val spawnLocation: ParticleSpawn,

		/**
		 * The rate of emissions, in particles per second.
		 */
		val emissionRate: PropertyTimeline,

		/**
		 * Calculates the life of a newly created particle.
		 */
		val particleLifeExpectancy: PropertyTimeline,

		val blendMode: BlendMode,

		val premultipliedAlpha: Boolean,

		val imageEntries: List<ParticleImageEntry>,

		val propertyTimelines: List<PropertyTimeline>
) {
	fun createInstance(): ParticleEmitterInstance {
		return ParticleEmitterInstance(this)
	}
}

data class EmitterDuration(

		/**
		 * The number of seconds this emitter will create particles.
		 */
		val duration: FloatRange,

		/**
		 * The time, in seconds, before the emitter begins.
		 */
		val delayBefore: FloatRange,

		/**
		 * The time, in seconds, after completion before restarting.
		 */
		val delayAfter: FloatRange
)

data class ParticleImageEntry(
		val time: Float,
		val path: String
)

data class PropertyTimeline(

		val property: String,

		/**
		 * If true, the final value will not be the high value, but the high + low
		 */
		val relative: Boolean,

		val low: FloatRange,

		val high: FloatRange,

		/**
		 * A list of control points for interpolated values. If this is empty, the low value will always be used.
		 */
		val timeline: List<TimelineValue>
) {

	fun reset(target: PropertyValue) {
		target.low = low.getValue()
		target.high = high.getValue()
		if (relative) target.high += target.low
		target.diff = target.high - target.low
		target.current = target.low
	}

	private val comparator: (Float, TimelineValue) -> Int = {
		time, element ->
		time.compareTo(element.time)
	}

	fun apply(target: PropertyValue, alpha: Float) {
		val n = timeline.size
		if (n == 0) return
		val timelineIndex = timeline.sortedInsertionIndex(alpha, matchForwards = true, comparator = comparator) - 1
		val timeA: Float
		val valueA: Float
		if (timelineIndex < 0) {
			timeA = 0f
			valueA = timeline.first().value
		} else {
			val timelineEntry = timeline[timelineIndex]
			timeA = timelineEntry.time
			valueA = timelineEntry.value
		}

		val timeB: Float
		val valueB: Float
		if (timelineIndex >= n - 1) {
			timeB = 1f
			valueB = timeline.last().value
		} else {
			val timelineEntry = timeline[timelineIndex + 1]
			timeB = timelineEntry.time
			valueB = timelineEntry.value
		}
		val valueAlpha = (alpha - timeA) / (timeB - timeA)
		val valueValue = (valueB - valueA) * valueAlpha + valueA
		target.current = valueValue * target.diff + target.low
	}
}

/**
 * Represents the range value at the given time.
 */
data class TimelineValue(

		/**
		 * A value of 0f - 1f indicating the current progress of this particle where the [value] will be used.
		 */
		val time: Float,

		/**
		 * A value of 0f - 1f indicating the interpolation of the low to high value.
		 */
		val value: Float
) : Comparable<TimelineValue> {

	override fun compareTo(other: TimelineValue): Int {
		return time.compareTo(other.time)
	}
}

class PropertyValue(
		var low: Float = 0f,
		var high: Float = 0f,
		var diff: Float = 0f,
		var current: Float = 0f
)

object ParticleEffectSerializer : From<ParticleEffect>, To<ParticleEffect> {

	override fun read(reader: Reader): ParticleEffect {
		return ParticleEffect(reader.arrayList("emitters", ParticleEmitterSerializer)!!)
	}

	override fun ParticleEffect.write(writer: Writer) {
		writer.array("emitters", emitters, ParticleEmitterSerializer)
	}
}

object ParticleEmitterSerializer : From<ParticleEmitter>, To<ParticleEmitter> {

	override fun read(reader: Reader): ParticleEmitter {
		return ParticleEmitter(
				id = reader.string("id")!!,
				name = reader.string("name")!!,
				enabled = reader.bool("enabled") ?: true,
				loops = reader.bool("loops") ?: true,
				blendMode = BlendMode.fromStr(reader.string("blendMode") ?: "normal")!!,
				duration = reader.obj("duration", EmitterDurationVoSerializer)!!,
				count = reader.int("count")!!,
				spawnLocation = reader.obj("spawnLocation", ParticleSpawnSerializer)!!,
				emissionRate = reader.obj("emissionRate", PropertyTimelineSerializer)!!,
				imageEntries = reader.arrayList("imageEntries", ParticleImageEntrySerializer)!!,
				particleLifeExpectancy = reader.obj("particleLifeExpectancy", PropertyTimelineSerializer)!!,
				premultipliedAlpha = reader.bool("premultipliedAlpha") ?: false,
				propertyTimelines = reader.arrayList("propertyTimelines", PropertyTimelineSerializer)!!
		)
	}

	override fun ParticleEmitter.write(writer: Writer) {
		writer.string("id", id)
		writer.string("name", name)
		writer.bool("enabled", enabled)
		writer.bool("loops", loops)
		writer.string("blendMode", blendMode.name)
		writer.obj("duration", duration, EmitterDurationVoSerializer)
		writer.int("count", count)
		writer.obj("spawnLocation", spawnLocation, ParticleSpawnSerializer)
		writer.obj("emissionRate", emissionRate, PropertyTimelineSerializer)
		writer.array("imageEntries", imageEntries, ParticleImageEntrySerializer)
		writer.obj("particleLifeExpectancy", particleLifeExpectancy, PropertyTimelineSerializer)
		writer.bool("premultipliedAlpha", premultipliedAlpha)
		writer.array("propertyTimelines", propertyTimelines, PropertyTimelineSerializer)
	}
}

object EmitterDurationVoSerializer : From<EmitterDuration>, To<EmitterDuration> {

	override fun read(reader: Reader): EmitterDuration {
		return EmitterDuration(
				duration = reader.obj("duration", FloatRangeSerializer)!!,
				delayBefore =  reader.obj("delayBefore", FloatRangeSerializer)!!,
				delayAfter =  reader.obj("delayAfter", FloatRangeSerializer)!!

		)
	}

	override fun EmitterDuration.write(writer: Writer) {
		writer.obj("duration", duration, FloatRangeSerializer)
		writer.obj("delayBefore", delayBefore, FloatRangeSerializer)
		writer.obj("delayAfter", delayAfter, FloatRangeSerializer)
	}
}

object PropertyTimelineSerializer : From<PropertyTimeline>, To<PropertyTimeline> {

	override fun read(reader: Reader): PropertyTimeline {
		val timelineFloats = reader.floatArray("timeline") ?: floatArrayOf()
		val timeline = ArrayList<TimelineValue>(timelineFloats.size shr 1)
		for (i in 0..timelineFloats.lastIndex step 2) {
			timeline.add(TimelineValue(timelineFloats[i], timelineFloats[i + 1]))
		}

		return PropertyTimeline(
				property = reader.string("property")!!,
				relative = reader.bool("relative") ?: false,
				low = reader.obj("min", FloatRangeSerializer)!!,
				high = reader.obj("max", FloatRangeSerializer)!!,
				timeline = timeline

		)
	}

	override fun PropertyTimeline.write(writer: Writer) {
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
	}
}

object ParticleImageEntrySerializer : From<ParticleImageEntry>, To<ParticleImageEntry> {

	override fun read(reader: Reader): ParticleImageEntry {
		return ParticleImageEntry(
				time = reader.float("time")!!,
				path = reader.string("path")!!
		)
	}

	override fun ParticleImageEntry.write(writer: Writer) {
		writer.string("path", path)
		writer.float("time", time)
	}
}