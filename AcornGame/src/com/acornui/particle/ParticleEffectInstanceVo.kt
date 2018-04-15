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


import com.acornui.collection.ArrayList
import com.acornui.core.userInfo
import com.acornui.graphics.Color
import com.acornui.math.*
import com.acornui.serialization.*

class ParticleEffectInstanceVo(

		val emitterInstances: List<ParticleEmitterInstance>
)

class ParticleEmitterInstance(

		val emitter: ParticleEmitterVo
) {

	val particles: List<ParticleVo>

	private val count = (emitter.count * maxParticlesScale).ceil()
	private var _activeCount: Int = 0

	val activeCount: Int
		get() = _activeCount


	private var _delayBefore = 0f

	/**
	 * The value of the emitter's [EmitterDuration.delayBefore] at the time of the last reset.
	 */
	val delayBefore: Float
		get() = _delayBefore

	private var _delayAfter = 0f

	/**
	 * The value of the emitter's [EmitterDuration.delayAfter] at the time of the last reset.
	 */
	val delayAfter: Float
		get() = _delayAfter

	private var _currentTime: Float = 0f

	/**
	 * This emitter instance's current time. This may be negative if there was a positive [delayBefore] value.
	 */
	val currentTime: Float
		get() = _currentTime

	private var _duration = 0f
	private var _durationInv = 0f

	private var endTime: Float = 0f
	private var _isComplete = false
	private var loops = emitter.loops

	private var accumulator = 0f

	private val emissionRateValue = PropertyValue()
	private val lifeExpectancyValue = PropertyValue()

	/**
	 * Returns true if this emitter is no longer doing anything.
	 */
	val isComplete: Boolean
		get() = _isComplete

	init {
		val propertyTimelines = emitter.propertyTimelines
		particles = ArrayList(count, {
			ParticleVo().apply {
				for (i in 0..propertyTimelines.lastIndex) {
					val timeline = propertyTimelines[i]
					timelines.add(ParticleTimelineInstance(timeline))
				}
			}
		})

		rewind()
	}

	fun update(stepTime: Float) {
		if (_isComplete) return
		if (!emitter.enabled) return

		_currentTime += stepTime

		if (_currentTime >= endTime) {
			if (loops) {
				val remainder = _currentTime - endTime
				rewind()
				_currentTime += remainder
			} else if (_activeCount == 0) {
				_isComplete = true
				return
			}
		}

		val alpha = _currentTime * _durationInv

		emitter.emissionRate.apply(emissionRateValue, alpha)
		emitter.particleLifeExpectancy.apply(lifeExpectancyValue, alpha)
		accumulator += emissionRateValue.current * stepTime
		if (accumulator > 1f) {
			for (i in 0..particles.lastIndex) {
				val particle = particles[i]
				if (!particle.active)
					activateParticle(particle)
				if (_activeCount >= count) accumulator = 0f
				if (accumulator < 1f) break
			}
		}

		for (i in 0..particles.lastIndex) {
			val particle = particles[i]
			if (particle.active) {
				particle.update(stepTime)
				if (particle.life > particle.lifeExpectancy) {
					particle.active = false
					_activeCount--
				}
			}
		}
	}

	private fun activateParticle(particle: ParticleVo) {
		particle.active = true
		for (j in 0..particle.timelines.lastIndex) {
			val prop = particle.timelines[j]
			prop.timeline.reset(prop.value)
			prop.setter(particle, prop.value.current)
		}
		emitter.spawnLocation.calculate(particle.position)
		particle.lifeExpectancy = lifeExpectancyValue.current
		_activeCount++
		accumulator--
	}

	/**
	 * Clears the current particles without changing the current time or state.
	 */
	fun clearParticles() {
		for (i in 0..particles.lastIndex) {
			particles[i].active = false
		}
		_activeCount = 0
		accumulator = 0f
	}

	fun reset() {
		clearParticles()
		_isComplete = false
		loops = emitter.loops
		rewind()
	}

	/**
	 * Stops the emitter.
	 * @param allowCompletion If true, the emitter won't deactivate existing particles, allowing them to finish.
	 */
	fun stop(allowCompletion: Boolean) {
		loops = false
		_currentTime = endTime
		if (!allowCompletion) {
			clearParticles()
			_isComplete = true
		}
	}

	fun rewind() {
		_delayBefore = emitter.duration.delayBefore
		_delayAfter = emitter.duration.delayAfter
		_currentTime = -_delayBefore
		_duration = emitter.duration.calculateDuration()
		_durationInv = 1f / _duration
		endTime = _duration + _delayAfter
		emitter.emissionRate.reset(emissionRateValue)
		emitter.particleLifeExpectancy.reset(lifeExpectancyValue)
	}

	companion object {
		var maxParticlesScale: Float = if (userInfo.isMobile) 0.5f else 1f
	}

}

class ParticleVo {

	var active = false

	/**
	 * When life reaches [lifeExpectancy], the particle will be recycled.
	 */
	var life = 0f

	private var _lifeExpectancy = 0f
	private var _lifeExpectancyInv = 0f

	var lifeExpectancy: Float
		get() = _lifeExpectancy
		set(value) {
			_lifeExpectancy = value
			_lifeExpectancyInv = 1f / value
		}

	val position = Vector3()
	val velocity = Vector3()

	val scale = Vector3()
	val scaleVelocity = Vector3()

	val rotation = Vector3()
	val rotationalVelocity = Vector3()

	/**
	 * pitch, yaw, and roll values in radians.
	 */
	val forwardDirection = Vector3()
	var forwardVelocity = 0f

	val colorTint = Color()

	/**
	 * The origin of the particle, with percent-based values.
	 * (Default 0.5f, 0.5f, 0.5f - the center of the particle)
	 */
	val origin = Vector3(0.5f, 0.5f, 0.5f)

	var imageIndex = 0

	val timelines: MutableList<ParticleTimelineInstance> = ArrayList()

	//	private val tmpQuat = Quaternion()

	fun update(stepTime: Float) {
		life += stepTime
		val alpha = life * _lifeExpectancyInv

		for (i in 0..timelines.lastIndex) {
			val prop = timelines[i]
			prop.timeline.apply(prop.value, alpha)
			prop.setter(this, prop.value.current)
		}

		position.add(velocity)
		scale.add(scaleVelocity)
		rotation.add(rotationalVelocity)
		if (forwardVelocity != 0f) {
			if (forwardDirection.z != 0f) {
				if (forwardDirection.y != 0f || forwardDirection.x != 0f) {
					// TODO: 3d forward direction.
//					tmpQuat.setEulerAnglesRad(forwardDirection.y, forwardDirection.x, forwardDirection.z)
//					tmpQuat.getAxisAngleRad(tmpVec)
//					position.add(tmpVec.x * forwardVelocity, tmpVec.y * forwardVelocity, tmpVec.z * forwardVelocity)
				} else {
					val theta = forwardDirection.z
					position.add(MathUtils.cos(theta) * forwardVelocity, MathUtils.sin(theta) * forwardVelocity, 0f)
				}

			}
		}
	}
}

class ParticleTimelineInstance(
		val timeline: PropertyTimeline
) {
	val value = PropertyValue()
	val setter: ParticleSetter = RegisteredParticleSetters.setters[timeline.name]
			?: throw Exception("Could not find property setter with the name ${timeline.name}")
}

typealias ParticleSetter = (ParticleVo, Float) -> Unit

object RegisteredParticleSetters {

	val setters: MutableMap<String, ParticleSetter> = hashMapOf(

			"x" to { target, v -> target.position.x = v },
			"y" to { target, v -> target.position.y = v },
			"z" to { target, v -> target.position.z = v },

			"velocityX" to { target, v -> target.velocity.x = v },
			"velocityY" to { target, v -> target.velocity.y = v },
			"velocityZ" to { target, v -> target.velocity.z = v },

			"originX" to { target, v -> target.origin.x = v },
			"originY" to { target, v -> target.origin.y = v },
			"originZ" to { target, v -> target.origin.z = v },

			"scaleX" to { target, v -> target.scale.x = v },
			"scaleY" to { target, v -> target.scale.y = v },
			"scaleZ" to { target, v -> target.scale.z = v },

			"scaleVelocityX" to { target, v -> target.scaleVelocity.x = v },
			"scaleVelocityY" to { target, v -> target.scaleVelocity.y = v },
			"scaleVelocityZ" to { target, v -> target.scaleVelocity.z = v },

			"rotationX" to { target, v -> target.rotation.x = v },
			"rotationY" to { target, v -> target.rotation.y = v },
			"rotationZ" to { target, v -> target.rotation.z = v },

			"rotationalVelocityX" to { target, v -> target.rotationalVelocity.x = v },
			"rotationalVelocityY" to { target, v -> target.rotationalVelocity.y = v },
			"rotationalVelocityZ" to { target, v -> target.rotationalVelocity.z = v },

			"forwardDirectionX" to { target, v -> target.forwardDirection.x = v },
			"forwardDirectionY" to { target, v -> target.forwardDirection.y = v },
			"forwardDirectionZ" to { target, v -> target.forwardDirection.z = v },
			"forwardVelocity" to { target, v -> target.forwardVelocity = v },

			"colorR" to { target, v -> target.colorTint.r = v },
			"colorG" to { target, v -> target.colorTint.g = v },
			"colorB" to { target, v -> target.colorTint.b = v },
			"colorA" to { target, v -> target.colorTint.a = v },

			"imageIndex" to { target, v -> target.imageIndex = v.toInt() }

	)

}

interface ParticleSpawn {

	val type: String

	/**
	 * Calculates a new particle spawn position.
	 * @out The vector to populate.
	 * @return Returns the [out] parameter.
	 */
	fun calculate(out: Vector3): Vector3
}

interface ParticleSpawnSerializer<T : ParticleSpawn> : To<T>, From<T> {

	val type: String

	companion object : From<ParticleSpawn>, To<ParticleSpawn> {

		override fun read(reader: Reader): ParticleSpawn {
			val type = reader.string("type") ?: "point"
			val factory = ParticleSpawnRegistry.getSerializer(type) ?: throw Exception("Unknown spawn type $type")
			return factory.read(reader)
		}

		override fun ParticleSpawn.write(writer: Writer) {
			writer.string("type", type)
			val factory = ParticleSpawnRegistry.getSerializer(type) ?: throw Exception("Unknown spawn type $type")
			factory.write2(this, writer)
		}
	}
}

object ParticleSpawnRegistry {

	private val nameToSerializers: MutableMap<String, ParticleSpawnSerializer<*>> = HashMap()

	init {
		addSerializer(PointSpawnSerializer)
	}

	fun <T : ParticleSpawn> addSerializer(serializer: ParticleSpawnSerializer<T>) {
		nameToSerializers[serializer.type] = serializer
	}

	@Suppress("UNCHECKED_CAST")
	fun getSerializer(name: String): ParticleSpawnSerializer<ParticleSpawn>? {
		return nameToSerializers[name] as ParticleSpawnSerializer<ParticleSpawn>?
	}
}

data class PointSpawn(val x: Float, val y: Float, val z: Float) : ParticleSpawn {

	override val type = TYPE

	override fun calculate(out: Vector3): Vector3 {
		out.set(x, y, z)
		return out
	}

	companion object {
		const val TYPE = "point"
	}
}

object PointSpawnSerializer : ParticleSpawnSerializer<PointSpawn> {

	override val type = PointSpawn.TYPE

	override fun read(reader: Reader): PointSpawn {
		return PointSpawn(
				reader.float("x") ?: 0f,
				reader.float("y") ?: 0f,
				reader.float("z") ?: 0f
		)
	}

	override fun PointSpawn.write(writer: Writer) {
		writer.float("x", x)
		writer.float("y", y)
		writer.float("z", z)
	}
}