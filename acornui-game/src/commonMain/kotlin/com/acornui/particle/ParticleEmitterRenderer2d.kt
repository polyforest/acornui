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

import com.acornui.component.RenderContext
import com.acornui.component.RenderContextRo
import com.acornui.component.Sprite
import com.acornui.core.LifecycleBase
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.math.PI
import com.acornui.math.Quaternion
import com.acornui.math.Vector3

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

	override fun render(renderContext: RenderContextRo) {
		if (!emitterInstance.emitter.enabled) return
		val particles = emitterInstance.particles
		for (i in 0..particles.lastIndex) {
			val particle = particles[i]
			if (particle.active) {
				particle.draw(renderContext)
			}
		}
	}

	private val childRenderContext = RenderContext()

	private fun Particle.draw(renderContext: RenderContextRo) {
		val sprite = sprites.getOrNull(imageIndex) ?: return
		val emitter = emitterInstance.emitter

		val emitterPosition = emitterInstance.position

		if (emitter.orientToForwardDirection) {
			rotationFinal.set(rotation).add(forwardDirection)
			rotationFinal.z += HALF_PI
		} else {
			rotationFinal.set(rotation)
		}
		sprite.setSize(null, null)

		val rotation = rotationFinal
		val origin = origin
		childRenderContext.parentContext = renderContext
		childRenderContext.modelTransformLocal.apply {
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
		childRenderContext.colorTintLocal.set(colorTint)
		sprite.render(childRenderContext)
	}

	companion object {
		private val quat = Quaternion()
		private val rotationFinal = Vector3()
	}
}

private const val HALF_PI: Float = PI * 0.5f
