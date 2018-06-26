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

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.acornui.particle


import com.acornui.collection.ArrayList
import com.acornui.collection.Clearable
import com.acornui.core.userInfo
import com.acornui.graphics.Color
import com.acornui.math.MathUtils.clamp
import com.acornui.math.Vector3
import com.acornui.math.Vector3Ro
import com.acornui.math.ceil
import kotlin.math.cos
import kotlin.math.sin

class ParticleEffectInstance(

		val emitterInstances: List<ParticleEmitterInstance>

) {

	private val _position = Vector3()

	val position: Vector3Ro
		get() = _position

	fun setPosition(x: Float, y: Float, z: Float) {
		translatePosition(x - _position.x, y - _position.y, z - _position.z)
	}

	fun translatePosition(xD: Float, yD: Float, zD: Float) {
		for (i in 0..emitterInstances.lastIndex) {
			emitterInstances[i].position.add(xD, yD, zD)
		}
	}
}

class ParticleEmitterInstance(

		val emitter: ParticleEmitter
) {

	val position = Vector3()

	val particles: List<Particle>

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

	/**
	 * The duration of this emitter.
	 */
	val duration: Float
		get() = _duration

	/**
	 * The sum of the delay values and the duration.
	 */
	val totalTime: Float
		get() = _delayBefore + _duration + _delayAfter

	/**
	 * The current progress of this emitter. This will be a value between 0f and 1f
	 */
	val progress: Float
		get() = (_currentTime + _delayBefore) / maxOf(0.001f, totalTime)

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

		particles = ArrayList(count, {
			Particle(timelineInstances = emitter.propertyTimelines.map { timelineInstance(it) })
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
				accumulator = 0f
				return
			}
		}

		val alpha = _currentTime * _durationInv

		emitter.emissionRate.apply(emissionRateValue, alpha)
		emitter.particleLifeExpectancy.apply(lifeExpectancyValue, alpha)
		if (_currentTime < _duration && _currentTime > 0f) {
			// Create new particles if the accumulator surpasses 1.
			accumulator += emissionRateValue.current * maxParticlesScale * stepTime
			if (accumulator > 1f) {
				for (i in 0..particles.lastIndex) {
					val particle = particles[i]
					if (!particle.active)
						activateParticle(particle)
					if (_activeCount >= count) accumulator = 0f
					if (accumulator < 1f) break
				}
			}
		}

		val alphaClamped = clamp(alpha, 0f, 1f)
		for (i in 0..particles.lastIndex) {
			val particle = particles[i]
			if (particle.active) {
				particle.update(stepTime, alphaClamped)
				if (particle.life > particle.lifeExpectancy) {
					particle.active = false
					_activeCount--
				}
			}
		}
	}

	private fun activateParticle(particle: Particle) {
		particle.clear()
		particle.active = true
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
		_delayBefore = emitter.duration.delayBefore.getValue()
		_delayAfter = emitter.duration.delayAfter.getValue()
		_currentTime = -_delayBefore
		_duration = emitter.duration.duration.getValue()
		_durationInv = 1f / _duration
		endTime = _duration + _delayAfter
		emitter.emissionRate.reset(emissionRateValue)
		emitter.particleLifeExpectancy.reset(lifeExpectancyValue)
	}

	companion object {
		var maxParticlesScale: Float = if (userInfo.isMobile) 0.5f else 1f
	}

}

class Particle(
		val timelineInstances: List<TimelineInstance>
) : Clearable {

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

	val scale = Vector3(1f, 1f, 1f)

	val rotation = Vector3()
	val rotationalVelocity = Vector3()

	/**
	 * pitch, yaw, and roll values in radians.
	 */
	val forwardDirection = Vector3()
	val forwardDirectionVelocity = Vector3()
	var forwardVelocity = 0f

	val colorTint: Color = Color.WHITE.copy()

	/**
	 * The origin of the particle, with percent-based values.
	 * (Default 0.5f, 0.5f, 0.5f - the center of the particle)
	 */
	val origin = Vector3(0.5f, 0.5f, 0.5f)

	var imageIndex = 0

	fun update(stepTime: Float, emitterAlphaClamped: Float) {
		life += stepTime
		val alpha = life * _lifeExpectancyInv
		val alphaClamped = clamp(alpha, 0f, 1f)

		for (i in 0..timelineInstances.lastIndex) {
			timelineInstances[i].apply(this, alphaClamped, emitterAlphaClamped)
		}

		position.add(velocity)
		rotation.add(rotationalVelocity)
		forwardDirection.add(forwardDirectionVelocity)
		if (forwardVelocity != 0f) {
			if (forwardDirection.y != 0f || forwardDirection.x != 0f) {
				// TODO: 3d forward direction.
			} else if (forwardDirection.z != 0f) {
				val theta = forwardDirection.z
				position.add(cos(theta) * forwardVelocity, sin(theta) * forwardVelocity, 0f)
			}
		}
	}

	override fun clear() {
		active = false
		life = 0f
		_lifeExpectancy = 0f
		_lifeExpectancyInv = 0f
		position.clear()
		velocity.clear()
		scale.set(1f, 1f, 1f)
		rotation.clear()
		rotationalVelocity.clear()
		forwardDirection.clear()
		forwardDirectionVelocity.clear()
		forwardVelocity = 0f
		colorTint.set(Color.WHITE)
		origin.set(0.5f, 0.5f, 0.5f)
		imageIndex = 0

		// Set all properties to the low value.
		for (i in 0..timelineInstances.lastIndex) {
			timelineInstances[i].reset(this)
		}
	}
}

interface TimelineInstance {

	fun apply(particle: Particle, particleAlphaClamped: Float, emitterAlphaClamped: Float)
	fun reset(particle: Particle)

}

fun timelineInstance(timeline: PropertyTimeline<*>): TimelineInstance {
	return if (timeline.property == "color") ColorTimelineInstance(timeline as ColorTimeline)
	else FloatTimelineInstance(timeline as FloatTimeline)
}

class FloatTimelineInstance(
		private val timeline: FloatTimeline
) : TimelineInstance {

	private val value = PropertyValue()

	private val offset = RegisteredParticleSetters.offsets[timeline.property] ?: 0f

	private val updater: ParticlePropertyUpdater = RegisteredParticleSetters.updaters[timeline.property]
			?: throw Exception("Could not find property updater with the name ${timeline.property}")

	override fun apply(particle: Particle, particleAlphaClamped: Float, emitterAlphaClamped: Float) {
		if (timeline.timeline.isEmpty()) return
		val previous = value.current
		timeline.apply(value, if (timeline.useEmitterDuration) emitterAlphaClamped else particleAlphaClamped)
		updater(particle, value.current - previous)
	}

	override fun reset(particle: Particle) {
		timeline.reset(value)
		updater(particle, value.current - offset)
	}
}

class ColorTimelineInstance(
		private val timeline: ColorTimeline
) : TimelineInstance {

	private val previous = Color.WHITE.copy()
	private val value = Color.WHITE.copy()

	override fun apply(particle: Particle, particleAlphaClamped: Float, emitterAlphaClamped: Float) {
		previous.set(value)
		timeline.apply(value, if (timeline.useEmitterDuration) emitterAlphaClamped else particleAlphaClamped)
		particle.colorTint.r += value.r - previous.r
		particle.colorTint.g += value.g - previous.g
		particle.colorTint.b += value.b - previous.b
	}

	override fun reset(particle: Particle) {
		value.set(Color.WHITE)
		particle.colorTint.set(Color.WHITE)
	}
}

typealias ParticlePropertyUpdater = (Particle, Float) -> Unit

object RegisteredParticleSetters {

	val offsets = hashMapOf(
			"scale" to 1f,
			"scaleX" to 1f,
			"scaleY" to 1f,
			"scaleZ" to 1f,

			"originX" to 0.5f,
			"originY" to 0.5f,
			"originZ" to 0.5f,

			"colorR" to 1f,
			"colorG" to 1f,
			"colorB" to 1f,
			"colorA" to 1f
	)

	val updaters: Map<String, ParticlePropertyUpdater> = hashMapOf(

			"x" to { target, delta -> target.position.x += delta },
			"y" to { target, delta -> target.position.y += delta },
			"z" to { target, delta -> target.position.z += delta },

			"velocityX" to { target, delta -> target.velocity.x += delta },
			"velocityY" to { target, delta -> target.velocity.y += delta },
			"velocityZ" to { target, delta -> target.velocity.z += delta },

			"originX" to { target, delta -> target.origin.x += delta },
			"originY" to { target, delta -> target.origin.y += delta },
			"originZ" to { target, delta -> target.origin.z += delta },

			"scale" to { target, delta -> target.scale.x += delta; target.scale.y += delta; target.scale.z += delta },
			"scaleX" to { target, delta -> target.scale.x += delta },
			"scaleY" to { target, delta -> target.scale.y += delta },
			"scaleZ" to { target, delta -> target.scale.z += delta },

			"rotationX" to { target, delta -> target.rotation.x += delta },
			"rotationY" to { target, delta -> target.rotation.y += delta },
			"rotationZ" to { target, delta -> target.rotation.z += delta },

			"rotationalVelocityX" to { target, delta -> target.rotationalVelocity.x += delta },
			"rotationalVelocityY" to { target, delta -> target.rotationalVelocity.y += delta },
			"rotationalVelocityZ" to { target, delta -> target.rotationalVelocity.z += delta },

			"forwardDirectionX" to { target, delta ->
				target.forwardDirection.x += delta
			},
			"forwardDirectionY" to { target, delta -> target.forwardDirection.y += delta },
			"forwardDirectionZ" to { target, delta -> target.forwardDirection.z += delta },

			"forwardDirectionVelocityZ" to { target, delta ->
				target.forwardDirectionVelocity.z += delta
			},

			"forwardVelocity" to { target, delta -> target.forwardVelocity += delta },

			"colorR" to { target, delta -> target.colorTint.r += delta },
			"colorG" to { target, delta -> target.colorTint.g += delta },
			"colorB" to { target, delta -> target.colorTint.b += delta },
			"colorA" to { target, delta -> target.colorTint.a += delta },

			"imageIndex" to { target, delta -> target.imageIndex += delta.toInt() }
	)

}