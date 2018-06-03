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

import com.acornui.component.InteractivityMode
import com.acornui.component.UiComponentImpl
import com.acornui.core.*
import com.acornui.core.assets.*
import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphics.TextureAtlasDataSerializer
import com.acornui.core.graphics.loadAndCacheAtlasPage
import com.acornui.core.time.onTick
import com.acornui.component.Sprite
import com.acornui.gl.core.GlState
import com.acornui.graphics.ColorRo
import com.acornui.math.MinMaxRo

class ParticleEffectComponent(
		owner: Owned
) : UiComponentImpl(owner) {

	private val glState = inject(GlState)

	init {
		interactivityMode = InteractivityMode.NONE

		onTick {
			val effect = _effect
			if (effect != null) {
				effect.update(it)
				window.requestRender()
			}
		}
	}

	/**
	 * Loads a particle effect and its textures, then assigning it to this component.
	 * @param pDataPath The path to the particle effect json.
	 * @param atlasPath The path to the atlas json for where the texture atlas the particle images are located.
	 * @param disposeOld If true, the old effect will be disposed and cached files decremented.
	 * @return Returns a deferred loaded particle effect in order to handle the wait.
	 */
	fun load(pDataPath: String, atlasPath: String, disposeOld: Boolean = true) = async {
		val oldEffect = _effect
		effect = loadParticleEffect(pDataPath, atlasPath)
		if (disposeOld)
			oldEffect?.dispose() // Dispose after load in order to reuse cached files.
		effect!!
	}

	override fun onActivated() {
		super.onActivated()
		_effect?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		_effect?.refDec()
	}

	private var _effect: LoadedParticleEffect? = null

	private var effect: LoadedParticleEffect?
		get() = _effect
		set(value) {
			val oldValue = _effect
			if (oldValue == value) return
			_effect = value
			if (isActive) {
				value?.refInc()
				oldValue?.refDec()
			}
		}

	fun effect(value: LoadedParticleEffect?) {
		effect = value
	}

	val effectInstance: ParticleEffectInstance?
		get() = _effect?.effectInstance

	override fun draw(viewport: MinMaxRo) {
		val effect = _effect ?: return
		glState.camera(camera, concatenatedTransform)
		effect.render(concatenatedColorTint)
	}

	override fun dispose() {
		effect = null
		super.dispose()
	}
}

class LoadedParticleEffect(

		val effectInstance: ParticleEffectInstance,

		private val renderers: List<ParticleEmitterRenderer>,

		/**
		 * The cached group the particle effect used to load all the files.
		 */
		private val group: CachedGroup
) : Updatable, Disposable {

	private val emitterInstances = effectInstance.emitterInstances

	init {
	}

	fun refInc() {
		for (i in 0..renderers.lastIndex) {
			renderers[i].refInc()
		}
	}

	fun refDec() {
		for (i in 0..renderers.lastIndex) {
			renderers[i].refDec()
		}
	}

	override fun update(stepTime: Float) {
		for (i in 0..emitterInstances.lastIndex) {
			emitterInstances[i].update(stepTime)
		}
	}

	override fun dispose() {
		group.dispose()
	}

	fun render(concatenatedColorTint: ColorRo) {
		for (i in 0..renderers.lastIndex) {
			renderers[i].render(concatenatedColorTint)
		}
	}
}

suspend fun Scoped.loadParticleEffect(particleEffect: ParticleEffect, atlasPath: String, group: CachedGroup = cachedGroup()): LoadedParticleEffect {
	val atlasDataPromise = loadAndCacheJson(atlasPath, TextureAtlasDataSerializer, group)
	val atlasData = atlasDataPromise.await()
	val emitterRenderers = ArrayList<ParticleEmitterRenderer>(particleEffect.emitters.size)
	val effectInstance = particleEffect.createInstance()

	for (emitterInstance in effectInstance.emitterInstances) {
		val emitter = emitterInstance.emitter
		val sprites = ArrayList<Sprite>(emitter.imageEntries.size)
		for (i in 0..emitter.imageEntries.lastIndex) {
			val imagePath = emitter.imageEntries[i].path

			val (page, region) = atlasData.findRegion(imagePath) ?: throw Exception("Could not find $imagePath in the atlas $atlasPath")
			val texture = loadAndCacheAtlasPage(atlasPath, page, group).await()

			val sprite = Sprite()
			sprite.texture = texture
			sprite.isRotated = region.isRotated
			sprite.setRegion(region.bounds)
			sprite.updateUv()
			sprites.add(sprite)
		}
		emitterRenderers.add(ParticleEmitterRenderer2d(injector, emitterInstance, sprites))
	}
	return LoadedParticleEffect(effectInstance, emitterRenderers, group)
}

suspend fun Scoped.loadParticleEffect(pDataPath: String, atlasPath: String, group: CachedGroup = cachedGroup()): LoadedParticleEffect {
	val atlasDataPromise = loadAndCacheJson(atlasPath, TextureAtlasDataSerializer, group)
	val particleEffect = loadAndCacheJson(pDataPath, ParticleEffectSerializer, group).await()
	val atlasData = atlasDataPromise.await()
	val emitterRenderers = ArrayList<ParticleEmitterRenderer>(particleEffect.emitters.size)
	val effectInstance = particleEffect.createInstance()

	for (emitterInstance in effectInstance.emitterInstances) {
		val emitter = emitterInstance.emitter
		val sprites = ArrayList<Sprite>(emitter.imageEntries.size)
		for (i in 0..emitter.imageEntries.lastIndex) {
			val imagePath = emitter.imageEntries[i].path

			val (page, region) = atlasData.findRegion(imagePath) ?: throw Exception("Could not find $imagePath in the atlas $atlasPath")
			val texture = loadAndCacheAtlasPage(atlasPath, page, group).await()

			val sprite = Sprite()
			sprite.texture = texture
			sprite.isRotated = region.isRotated
			sprite.setRegion(region.bounds)
			sprite.updateUv()
			sprites.add(sprite)
		}
		emitterRenderers.add(ParticleEmitterRenderer2d(injector, emitterInstance, sprites))
	}
	return LoadedParticleEffect(effectInstance, emitterRenderers, group)
}

interface ParticleEmitterRenderer {

	fun refInc()

	fun refDec()

	fun render(concatenatedColorTint: ColorRo)

}


fun Owned.particleEffectComponent(init: ParticleEffectComponent.() -> Unit = {}): ParticleEffectComponent {
	val p = ParticleEffectComponent(this)
	p.init()
	return p
}

fun Owned.particleEffectComponent(pDataPath: String, atlasPath: String, init: ParticleEffectComponent.() -> Unit = {}): ParticleEffectComponent {
	val p = ParticleEffectComponent(this)
	p.load(pDataPath, atlasPath)
	p.init()
	return p
}
