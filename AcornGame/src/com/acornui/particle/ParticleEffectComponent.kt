package com.acornui.particle

import com.acornui.action.Decorator
import com.acornui.async.Deferred
import com.acornui.async.async
import com.acornui.component.InteractivityMode
import com.acornui.component.UiComponentImpl
import com.acornui.core.assets.*
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphics.TextureAtlasDataSerializer
import com.acornui.core.graphics.loadAndCacheAtlasPage
import com.acornui.core.time.onTick
import com.acornui.core.userInfo
import com.acornui.gl.core.GlState
import com.acornui.math.ceil

class ParticleEffectComponent(owner: Owned) : UiComponentImpl(owner) {

	private val glState = inject(GlState)

	val effect = ParticleEffect()

	private val emitterRenderers = ArrayList<ParticleEmitterRenderer>()

	private val maxParticleCountScale: Float = if (userInfo.isMobile) 0.5f else 1f
	private val minParticleCountScale: Float = 1f

	init {
		interactivityMode = InteractivityMode.NONE
		onTick {
			effect.update(it)
		}
	}

	override fun onActivated() {
		super.onActivated()
		for (i in 0..emitterRenderers.lastIndex) {
			emitterRenderers[i].sprite.texture?.refInc()
		}
	}

	override fun onDeactivated() {
		super.onDeactivated()
		for (i in 0..emitterRenderers.lastIndex) {
			emitterRenderers[i].sprite.texture?.refDec()
		}
	}

	private var group: CachedGroup? = null

	fun load(pDataPath: String, atlasPath: String): Deferred<Unit> = async {
		clearRenderers()
		group?.dispose()
		group = cachedGroup()
		val group = group!!
		val atlasDataAsync = loadAndCacheJson(atlasPath, TextureAtlasDataSerializer, group)
		val pEffectDataAsync = loadAndCache(pDataPath, AssetType.TEXT, group)

		val pEffect = ParticleEffectAssetDecorator.decorate(pEffectDataAsync.await())
		if (maxParticleCountScale != 1f || minParticleCountScale != 1f) {
			for (emitter in effect.emitters) {
				emitter.maxParticleCount = (emitter.maxParticleCount * maxParticleCountScale).ceil()
				emitter.minParticleCount = (emitter.minParticleCount * minParticleCountScale).ceil()
			}
		}
		effect.set(pEffect)
		effect.flipY()

		val textureAtlasData = atlasDataAsync.await()

		for (emitter in effect.emitters) {
			val imagePath = emitter.imagePath ?: continue
			val (page, region) = textureAtlasData.findRegion(imagePath) ?: continue
			val texture = loadAndCacheAtlasPage(atlasPath, page, group).await()

			val renderer = ParticleEmitterRenderer(injector, emitter)
			emitter.spriteWidth = region.originalWidth.toFloat()
			emitter.spriteHeight = region.originalHeight.toFloat()
			emitterRenderers.add(renderer)
			val s = renderer.sprite
			s.texture = texture
			s.isRotated = region.isRotated
			s.setRegion(region.bounds)
			s.updateUv()
			if (isActive)
				texture.refInc()
		}
	}

	override fun draw() {
		val concatenatedTransform = concatenatedTransform
		val concatenatedColorTint = concatenatedColorTint
		glState.camera(camera)
		for (i in 0..emitterRenderers.lastIndex) {
			emitterRenderers[i].draw(concatenatedTransform, concatenatedColorTint)
		}
	}

	private fun clearRenderers() {
		if (isActive) {
			for (i in 0..emitterRenderers.lastIndex) {
				emitterRenderers[i].sprite.texture?.refDec()
			}
		}
		emitterRenderers.clear()
	}

	override fun dispose() {
		super.dispose()

		clearRenderers()
		group?.dispose()
		group = null
	}
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

// We cache a ParticleEffect with the loaded data, then use that to set used instances.
object ParticleEffectAssetDecorator : Decorator<String, ParticleEffect> {
	override fun decorate(target: String): ParticleEffect {
		return ParticleEffectReader.read(target)
	}
}