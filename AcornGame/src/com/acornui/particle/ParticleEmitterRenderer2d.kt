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

import com.acornui.core.LifecycleBase
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.component.Sprite
import com.acornui.gl.core.GlState
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo

class ParticleEmitterRenderer2d(
		override val injector: Injector,
		private val emitterInstance: ParticleEmitterInstance,
		private val sprites: List<Sprite>
) : LifecycleBase(), Scoped, ParticleEmitterRenderer {

	private val glState: GlState = inject(GlState)

	init {
	}

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

	override fun render(concatenatedColorTint: ColorRo) {
		if (!emitterInstance.emitter.enabled) return
		val particles = emitterInstance.particles
		for (i in 0..particles.lastIndex) {
			val particle = particles[i]
			if (particle.active)
				particle.draw(concatenatedColorTint)
		}
	}

	private val finalColor = Color()

	private fun Particle.draw(concatenatedColorTint: ColorRo) {
		val sprite = sprites.getOrNull(imageIndex) ?: return
		sprite.blendMode = emitterInstance.emitter.blendMode
		sprite.premultipliedAlpha = emitterInstance.emitter.premultipliedAlpha

		val w = sprite.naturalWidth * scale.x
		val h = sprite.naturalHeight * scale.y
		val emitterPosition = emitterInstance.position
		sprite.updateVertices(w, h, position.x + emitterPosition.x, position.y + emitterPosition.y, position.z + emitterPosition.z, rotation.z, w * origin.x, h * origin.y)
		sprite.draw(glState, finalColor.set(colorTint).mul(concatenatedColorTint))
	}

}