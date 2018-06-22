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
		val emissionRate: FloatTimeline,

		/**
		 * Calculates the life of a newly created particle.
		 */
		val particleLifeExpectancy: FloatTimeline,

		val blendMode: BlendMode,

		val premultipliedAlpha: Boolean,

		val imageEntries: List<ParticleImageEntry>,

		/**
		 * Timelines relative to the particle life.
		 */
		val propertyTimelines: List<FloatTimeline>
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
				name = reader.string("name")!!,
				enabled = reader.bool("enabled") ?: true,
				loops = reader.bool("loops") ?: true,
				blendMode = BlendMode.fromStr(reader.string("blendMode") ?: "normal")!!,
				duration = reader.obj("duration", EmitterDurationVoSerializer)!!,
				count = reader.int("count")!!,
				spawnLocation = reader.obj("spawnLocation", ParticleSpawnSerializer)!!,
				emissionRate = reader.obj("emissionRate", FloatTimelineSerializer)!!,
				imageEntries = reader.arrayList("imageEntries", ParticleImageEntrySerializer)!!,
				particleLifeExpectancy = reader.obj("particleLifeExpectancy", FloatTimelineSerializer)!!,
				premultipliedAlpha = reader.bool("premultipliedAlpha") ?: false,
				propertyTimelines = reader.arrayList("propertyTimelines", FloatTimelineSerializer)!!
		)
	}

	override fun ParticleEmitter.write(writer: Writer) {
		writer.string("name", name)
		writer.bool("enabled", enabled)
		writer.bool("loops", loops)
		writer.string("blendMode", blendMode.name)
		writer.obj("duration", duration, EmitterDurationVoSerializer)
		writer.int("count", count)
		writer.obj("spawnLocation", spawnLocation, ParticleSpawnSerializer)
		writer.obj("emissionRate", emissionRate, FloatTimelineSerializer)
		writer.array("imageEntries", imageEntries, ParticleImageEntrySerializer)
		writer.obj("particleLifeExpectancy", particleLifeExpectancy, FloatTimelineSerializer)
		writer.bool("premultipliedAlpha", premultipliedAlpha)
		writer.array("propertyTimelines", propertyTimelines, FloatTimelineSerializer)
	}
}

object EmitterDurationVoSerializer : From<EmitterDuration>, To<EmitterDuration> {

	override fun read(reader: Reader): EmitterDuration {
		return EmitterDuration(
				duration = reader.obj("duration", FloatRangeSerializer)!!,
				delayBefore = reader.obj("delayBefore", FloatRangeSerializer)!!,
				delayAfter = reader.obj("delayAfter", FloatRangeSerializer)!!

		)
	}

	override fun EmitterDuration.write(writer: Writer) {
		writer.obj("duration", duration, FloatRangeSerializer)
		writer.obj("delayBefore", delayBefore, FloatRangeSerializer)
		writer.obj("delayAfter", delayAfter, FloatRangeSerializer)
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