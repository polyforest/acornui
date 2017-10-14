package com.acornui.particle

import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphics.BlendMode
import com.acornui.gl.component.Sprite
import com.acornui.gl.core.GlState
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.Matrix4Ro

class ParticleEmitterRenderer(
		override val injector: Injector,
		private val particleEmitter: ParticleEmitter
) : Scoped {

	private val glState: GlState = inject(GlState)

	val sprite = Sprite()

	fun draw(concatenatedTransform: Matrix4Ro, concatenatedColorTint: ColorRo) {
		sprite.texture ?: return

		val particles = particleEmitter.particles!!
		val active = particleEmitter.active!!

		for (i in 0..active.lastIndex) {
			if (active[i]) {
				particles[i]!!.draw(concatenatedTransform, concatenatedColorTint)
			}
		}
	}

	private val finalColor = Color()

	private fun Particle.draw(concatenatedTransform: Matrix4Ro, concatenatedColorTint: ColorRo) {
		val blendMode = if (particleEmitter.isAdditive) BlendMode.ADDITIVE else BlendMode.NORMAL
		sprite.blendMode = blendMode
		sprite.premultipliedAlpha = particleEmitter.isPremultipliedAlpha

		sprite.updateWorldVertices(concatenatedTransform, sprite.naturalWidth * scale, sprite.naturalHeight * scale, x, y, z, rotation, sprite.naturalWidth * scale * 0.5f, sprite.naturalHeight * scale * 0.5f)
		sprite.draw(glState, finalColor.set(colorTint).mul(concatenatedColorTint))
	}

}