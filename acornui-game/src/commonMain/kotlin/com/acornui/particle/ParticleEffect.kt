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
import com.acornui.graphic.BlendMode
import kotlinx.serialization.Serializable

@Serializable
data class ParticleEffect(
		val version: String = "0.4.0",
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

@Serializable
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
		val emissionRate: PropertyTimeline,

		/**
		 * Calculates the life of a newly created particle.
		 */
		val particleLifeExpectancy: PropertyTimeline,

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
		val propertyTimelines: List<PropertyTimeline>
) {
	fun createInstance(maxParticlesScale: Float): ParticleEmitterInstance {
		return ParticleEmitterInstance(this, maxParticlesScale)
	}
}

@Serializable
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

@Serializable
data class ParticleImageEntry(
		val time: Float,
		val path: String
)