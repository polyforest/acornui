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

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.acornui.component

import com.acornui.asset.CacheSet
import com.acornui.asset.cacheSet
import com.acornui.asset.loadAndCacheJsonAsync
import com.acornui.async.launchSupervised
import com.acornui.collection.fill
import com.acornui.collection.forEach2
import com.acornui.di.Context
import com.acornui.gl.core.CachedGl20
import com.acornui.graphic.*
import com.acornui.mainContext
import com.acornui.math.Bounds
import com.acornui.recycle.Clearable
import com.acornui.time.onTick
import kotlinx.coroutines.Job
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class SpriteAnimation(owner: Context) : UiComponentImpl(owner), Clearable {

	/**
	 * The current animation frame. This will increment after this animation's framerate duration has passed.
	 */
	var currentFrame = 0

	/**
	 * After [endFrame] the animation will start back on this index.
	 *
	 */
	var startFrame: Int = 0

	/**
	 * After this frame, the animation will loop back to [startFrame]
	 * If this value is negative, the last frame in the sequence will be used.
	 */
	var endFrame: Int = -1

	/**
	 * The number of frames per second. By default this will match the main looper's framerate.
	 */
	var frameRate: Int = mainContext.looper.frameRate

	/**
	 * If true, when the animation hits [endFrame] it will loop back to [startFrame]
	 */
	var loops = true

	/**
	 * The time interval between frames, in seconds.
	 */
	val frameTimeS: Float
		get() = 1f / frameRate.toFloat()

	/**
	 * If true, this animation will not progress.
	 */
	var paused: Boolean = false

	private var elapsed: Float = 0f

	init {
		onTick { dT ->
			val animation = animation
			if (!paused && animation != null) {
				val tickTime = frameTimeS

				elapsed += dT
				while (elapsed >= tickTime) {
					// Tick a frame
					elapsed -= tickTime
					val end = if (endFrame < 0) animation.frames.lastIndex else endFrame
					if (currentFrame >= end) {
						if (loops)
							currentFrame = startFrame
					} else {
						currentFrame++
					}
				}
				window.requestRender()
			}
		}
	}

	/**
	 * The [Job] set from loading a texture from a path.
	 * This may be cancelled to stop loading.
	 */
	var loaderJob: Job? = null
		private set(value) {
			field?.cancel()
			field = value
		}

	/**
	 * Reference counting to the textures loaded.
	 */
	private var cacheSet: CacheSet? = null
		private set(value) {
			field?.dispose()
			field = value
		}

	/**
	 * The current animation being used.
	 * This can be set by one of the animation setters: `animation(...)`
	 */
	var animation: LoadedAnimation? = null
		private set(value) {
			val old = field
			if (old == value) return
			field = value
			if (isActive) {
				old?.refDec()
				value?.refInc()
			}
			invalidateLayout()
		}

	/**
	 * Loads the sprite animation within the specified texture atlas region.
	 * @return Returns a [Job] that represents the load and set work. This may be cancelled.
	 * NB: This does not clear the current animation immediately, only after loading has completed. To clear
	 * immediately, use [clear].
	 */
	fun animation(atlasPath: String, regionName: String): Job {
		clear()
		cacheSet = cacheSet()
		return launchSupervised {
			animation = loadSpriteAnimation(atlasPath, regionName, cacheSet!!)
		}.also {
			loaderJob = it
		}
	}

	/**
	 * If the animation was set via `animation(value: LoadedAnimation)`, this will return that value.
	 */
	var explicitAnimation: LoadedAnimation? = null
		private set

	fun animation(value: LoadedAnimation?) {
		if (explicitAnimation == value) return
		clear()
		this.animation = value
	}

	override fun onActivated() {
		super.onActivated()
		animation?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		animation?.refDec()
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val animation = animation ?: return
		val w = explicitWidth ?: animation.frames.firstOrNull()?.naturalWidth ?: 0f
		val h = explicitHeight ?: animation.frames.firstOrNull()?.naturalHeight ?: 0f
		out.set(w, h)
	}

	override fun updateVerticesGlobal() {
		super.updateVerticesGlobal()
		val animation = animation ?: return
		if (width <= 0f || height <= 0f) return
		animation.frames.forEach2 {
			it.updateGlobalVertices(width, height, transformGlobal, colorTintGlobal)
		}
	}

	override fun draw() {
		animation?.frames?.getOrNull(currentFrame - startFrame)?.render()
	}

	override fun clear() {
		loaderJob = null
		cacheSet = null
		explicitAnimation = null
		invalidateLayout()
	}

	override fun dispose() {
		clear()
		super.dispose()
	}
}

data class LoadedAnimation(
		val textures: List<Texture>,
		val frames: List<BasicRenderable>
) {
	fun refDec() {
		for (i in 0..textures.lastIndex) {
			textures[i].refDec()
		}
	}

	fun refInc() {
		for (i in 0..textures.lastIndex) {
			textures[i].refInc()
		}
	}
}

suspend fun Context.loadSpriteAnimation(atlasPath: String, regionName: String, cacheSet: CacheSet = cacheSet()): LoadedAnimation {
	val atlasData = loadAndCacheJsonAsync(TextureAtlasData.serializer(), atlasPath, cacheSet).await()
	val regions = ArrayList<Pair<AtlasRegionData, AtlasPageData>?>()
	for (page in atlasData.pages) {
		for (region in page.regions) {
			val index = region.name.calculateFrameIndex(regionName)
			if (index >= 0) {
				regions.fill(index + 1) { null }
				regions[index] = region to page
			}
		}
	}
	val textures = ArrayList<Texture>()
	val frames = ArrayList<Atlas>()
	for (i in 0..regions.lastIndex) {
		val (regionData, page) = regions[i] ?: continue
		val texture = loadAndCacheAtlasPage(atlasPath, page, cacheSet)
		if (!textures.contains(texture)) textures.add(texture)
		val atlas = Atlas(inject(CachedGl20))
		atlas.region(AtlasRegion(texture, regionData))
		frames.add(atlas)
	}
	return LoadedAnimation(textures, frames)
}

/**
 * If the region name matches, returns the index of the frame. Otherwise, returns -1
 */
private fun String.calculateFrameIndex(name: String): Int {
	if (this.startsWith(name)) {
		if (this.length == name.length) return 0
		if (this[name.length] == '.') return 0
		if (this[name.length] == '_') {
			var lastIndex = lastIndexOf('.')
			if (lastIndex == -1)
				lastIndex = length
			return substring(name.length + 1, lastIndex).toIntOrNull() ?: -1
		}
	}
	return -1
}

inline fun Context.spriteAnimation(atlasPath: String, regionName: String, init: ComponentInit<SpriteAnimation> = {}): SpriteAnimation  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = SpriteAnimation(this)
	s.animation(atlasPath, regionName)
	s.init()
	return s
}

inline fun Context.spriteAnimation(init: ComponentInit<SpriteAnimation> = {}): SpriteAnimation  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = SpriteAnimation(this)
	s.init()
	return s
}
