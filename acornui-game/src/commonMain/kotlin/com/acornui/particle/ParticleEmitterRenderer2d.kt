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

import com.acornui.component.Sprite
import com.acornui.LifecycleBase
import com.acornui.di.Injector
import com.acornui.di.Scoped
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*

class ParticleEmitterRenderer2d(
		override val injector: Injector,
		private val emitterInstance: ParticleEmitterInstance,
		private val sprites: List<Sprite>
) : LifecycleBase(), Scoped, ParticleEmitterRenderer {

	override fun refInc() {
		for (i in 0..sprites.lastIndex) {
			sprites[i].texture?.refInc()
		}
	}

	override fun refDec() {
		for (i in 0..sprites.lastIndex) {
			sprites[i].texture?.refDec()
		}
	}

	override fun render(transform: Matrix4Ro, tint: ColorRo) {
		if (!emitterInstance.emitter.enabled) return
		val particles = emitterInstance.particles
		for (i in 0..particles.lastIndex) {
			val particle = particles[i]
			if (particle.active) {
				particle.draw(transform, tint)
			}
		}
	}

	private val transform = Matrix4()
	private val tint = Color()

	private fun Particle.draw(emitterTransform: Matrix4Ro, emitterTint: ColorRo) {
		val sprite = sprites.getOrNull(imageIndex) ?: return
		val emitter = emitterInstance.emitter

		val emitterPosition = emitterInstance.position

		if (emitter.orientToForwardDirection) {
			rotationFinal.set(rotation).add(forwardDirection)
			rotationFinal.z += HALF_PI
		} else {
			rotationFinal.set(rotation)
		}

		val rotation = rotationFinal
		val origin = origin
		transform.apply {
			idt()
			trn(position.x + emitterPosition.x, position.y + emitterPosition.y, position.z + emitterPosition.z)
			if (!rotation.isZero()) {
				quat.setEulerAngles(rotation.x, rotation.y, rotation.z)
				rotate(quat)
			}
			scale(scale)
			if (!origin.isZero())
				translate(-origin.x * sprite.naturalWidth, -origin.y * sprite.naturalHeight)
		}
		transform.mulLeft(emitterTransform)
		tint.set(emitterTint).mul(colorTint)
		sprite.updateGlobalVertices(transform = transform, tint = tint)
		sprite.render()
	}

	companion object {
		private val quat = Quaternion()
		private val rotationFinal = Vector3()
	}
}

private const val HALF_PI: Float = PI * 0.5f
