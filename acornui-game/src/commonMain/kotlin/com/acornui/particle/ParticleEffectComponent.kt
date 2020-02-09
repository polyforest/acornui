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

import com.acornui.Disposable
import com.acornui.Updatable
import com.acornui.asset.*
import com.acornui.async.UI
import com.acornui.async.globalAsync
import com.acornui.component.InteractivityMode
import com.acornui.component.Sprite
import com.acornui.component.UiComponentImpl
import com.acornui.di.Context
import com.acornui.gl.core.CachedGl20
import com.acornui.graphic.ColorRo
import com.acornui.graphic.TextureAtlasData
import com.acornui.graphic.loadAndCacheAtlasPage
import com.acornui.math.Matrix4Ro
import com.acornui.serialization.binaryParse
import com.acornui.serialization.jsonParse
import com.acornui.time.onTick
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers

class ParticleEffectComponent(
		owner: Context
) : UiComponentImpl(owner) {

	/**
	 * If true, the effect will automatically play when loaded.
	 */
	var autoPlay = true

	init {
		interactivityMode = InteractivityMode.NONE
		onTick(::tick)
	}

	private fun tick(dT: Float) {
		_effect?.update(dT)
	}

	/**
	 * Loads a particle effect and its textures, then assigning it to this component.
	 * @param pDataPath The path to the particle effect json.
	 * @param atlasPath The path to the atlas json for where the texture atlas the particle images are located.
	 * @param disposeOld If true, the old effect will be disposed and cached files decremented.
	 * @return Returns a deferred loaded particle effect in order to handle the wait.
	 */
	fun load(pDataPath: String, atlasPath: String, disposeOld: Boolean = true, maxParticlesScale: Float = 1f): Deferred<LoadedParticleEffect> = globalAsync(Dispatchers.UI) {
		val oldEffect = _effect
		effect = loadParticleEffect(pDataPath, atlasPath, maxParticlesScale = maxParticlesScale)
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
			if (!autoPlay)
				value?.effectInstance?.stop(false)
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

	override fun draw() {
		val effect = _effect ?: return
		effect.render(transformGlobal, colorTintGlobal)
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
		private val cachedGroup: CachedGroup
) : Updatable, Disposable {

	private val emitterInstances = effectInstance.emitterInstances

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

	override fun update(dT: Float) {
		for (i in 0..emitterInstances.lastIndex) {
			emitterInstances[i].update(dT)
		}
	}

	override fun dispose() {
		cachedGroup.dispose()
	}

	fun render(transform: Matrix4Ro, tint: ColorRo) {
		for (i in 0..renderers.lastIndex) {
			renderers[i].render(transform, tint)
		}
	}
}

typealias SpriteResolver = suspend (emitter: ParticleEmitter, imageEntry: ParticleImageEntry) -> Sprite

suspend fun Context.loadParticleEffect(pDataPath: String, atlasPath: String, group: CachedGroup = cachedGroup(), maxParticlesScale: Float = 1f): LoadedParticleEffect {
	@Suppress("DeferredResultUnused")
	loadAndCacheJsonAsync(TextureAtlasData.serializer(), atlasPath, group) // Start the atlas loading and parsing in parallel.
	val particleEffect = if (pDataPath.endsWith("bin", ignoreCase = true)) {
		// Binary
		binaryParse(ParticleEffect.serializer(), loadBinary(pDataPath))
	} else {
		jsonParse(ParticleEffect.serializer(), loadText(pDataPath))
	}
	return loadParticleEffect(particleEffect, atlasPath, group, maxParticlesScale)
}

suspend fun Context.loadParticleEffect(particleEffect: ParticleEffect, atlasPath: String, group: CachedGroup = cachedGroup(), maxParticlesScale: Float = 1f): LoadedParticleEffect {
	val atlasData = loadAndCacheJsonAsync(TextureAtlasData.serializer(), atlasPath, group).await()
	val gl = inject(CachedGl20)

	val spriteResolver: SpriteResolver = { emitter, imageEntry ->
		val (page, region) = atlasData.findRegion(imageEntry.path)
				?: throw Exception("Could not find \"${imageEntry.path}\" in the atlas $atlasPath")
		val texture = loadAndCacheAtlasPage(atlasPath, page, group).await()

		val sprite = Sprite(gl)
		sprite.blendMode = emitter.blendMode
		sprite.premultipliedAlpha = emitter.premultipliedAlpha
		sprite.texture = texture
		sprite.setRegion(region.bounds, region.isRotated)
		sprite
	}
	return loadParticleEffect(particleEffect, group, spriteResolver, maxParticlesScale)
}

/**
 * Given a particle effect and a sprite resolver, creates a particle effect instance, and requests a [Sprite] for every
 * emitter.
 */
suspend fun Context.loadParticleEffect(particleEffect: ParticleEffect, group: CachedGroup = cachedGroup(), spriteResolver: SpriteResolver, maxParticlesScale: Float = 1f): LoadedParticleEffect {
	val emitterRenderers = ArrayList<ParticleEmitterRenderer>(particleEffect.emitters.size)
	val effectInstance = particleEffect.createInstance(maxParticlesScale)

	for (emitterInstance in effectInstance.emitterInstances) {
		val emitter = emitterInstance.emitter
		val sprites = ArrayList<Sprite>(emitter.imageEntries.size)
		for (i in 0..emitter.imageEntries.lastIndex) {
			val sprite = spriteResolver(emitter, emitter.imageEntries[i])
			sprites.add(sprite)
		}
		emitterRenderers.add(ParticleEmitterRenderer2d(emitterInstance, sprites))
	}
	return LoadedParticleEffect(effectInstance, emitterRenderers, group)
}

interface ParticleEmitterRenderer {

	fun refInc()

	fun refDec()

	fun render(transform: Matrix4Ro, tint: ColorRo)

}


fun Context.particleEffectComponent(init: ParticleEffectComponent.() -> Unit = {}): ParticleEffectComponent {
	val p = ParticleEffectComponent(this)
	p.init()
	return p
}

fun Context.particleEffectComponent(pDataPath: String, atlasPath: String, maxParticlesScale: Float = 1f, init: ParticleEffectComponent.() -> Unit = {}): ParticleEffectComponent {
	val p = ParticleEffectComponent(this)
	p.load(pDataPath, atlasPath, maxParticlesScale = maxParticlesScale)
	p.init()
	return p
}
