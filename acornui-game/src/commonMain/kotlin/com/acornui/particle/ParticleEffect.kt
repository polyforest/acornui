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

import com.acornui.UidUtil
import com.acornui.Version
import com.acornui.graphic.BlendMode
import com.acornui.serialization.*
import kotlinx.serialization.Serializable

data class ParticleEffect(
		
		val emitters: List<ParticleEmitter>
) {
	fun createInstance(maxParticlesScale: Float): ParticleEffectInstance {
		val emitterInstances = ArrayList<ParticleEmitterInstance>(emitters.size)
		for (i in 0..emitters.lastIndex) {
			val emitterInstance = emitters[i].createInstance(maxParticlesScale)
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
		 * If true, the forward direction affects the rotation.
		 */
		val orientToForwardDirection: Boolean,

		/**
		 * Timelines relative to the particle life.
		 */
		val propertyTimelines: List<PropertyTimeline<*>>
) {
	fun createInstance(maxParticlesScale: Float): ParticleEmitterInstance {
		return ParticleEmitterInstance(this, maxParticlesScale)
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

	val serializationVersion = Version(0, 2, 0)

	override fun read(reader: Reader): ParticleEffect {
		val version = Version.fromStr(reader.string("version") ?: "0.2.0.0")
		if (!version.isApiCompatible(serializationVersion)) throw Exception("Cannot read from version ${version.toVersionString()}")
		return ParticleEffect(reader.arrayList("emitters", ParticleEmitterSerializer)!!)
	}

	override fun ParticleEffect.write(writer: Writer) {
		writer.string("version", serializationVersion.toVersionString())
		writer.array("emitters", emitters, ParticleEmitterSerializer)
	}
}

object ParticleEmitterSerializer : From<ParticleEmitter>, To<ParticleEmitter> {

	override fun read(reader: Reader): ParticleEmitter {
		return ParticleEmitter(
				name = reader.string("name")!!,
				enabled = reader.bool("enabled") ?: true,
				loops = reader.bool("loops") ?: true,
				duration = reader.obj("duration", EmitterDurationVoSerializer)!!,
				count = reader.int("count")!!,
				emissionRate = reader.obj("emissionRate", FloatTimelineSerializer)!!,
				particleLifeExpectancy = reader.obj("particleLifeExpectancy", FloatTimelineSerializer)!!,
				blendMode = BlendMode.fromStr(reader.string("blendMode") ?: "normal")!!,
				premultipliedAlpha = reader.bool("premultipliedAlpha") ?: false,
				imageEntries = reader.arrayList("imageEntries", ParticleImageEntrySerializer)!!,
				orientToForwardDirection = reader.bool("orientToForwardDirection") ?: false,
				propertyTimelines = reader.arrayList("propertyTimelines", PropertyTimelineSerializer)!!
		)
	}

	override fun ParticleEmitter.write(writer: Writer) {
		writer.string("name", name)
		writer.bool("enabled", enabled)
		writer.bool("loops", loops)
		writer.obj("duration", duration, EmitterDurationVoSerializer)
		writer.int("count", count)
		writer.obj("emissionRate", emissionRate, FloatTimelineSerializer)
		writer.obj("particleLifeExpectancy", particleLifeExpectancy, FloatTimelineSerializer)
		writer.string("blendMode", blendMode.name)
		writer.bool("premultipliedAlpha", premultipliedAlpha)
		writer.array("imageEntries", imageEntries, ParticleImageEntrySerializer)
		writer.bool("orientToForwardDirection", orientToForwardDirection)
		writer.array("propertyTimelines", propertyTimelines, PropertyTimelineSerializer)
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
