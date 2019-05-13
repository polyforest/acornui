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

package com.acornui.component

import com.acornui.async.Deferred
import com.acornui.async.async
import com.acornui.async.then
import com.acornui.collection.fill
import com.acornui.core.AppConfig
import com.acornui.core.asset.CachedGroup
import com.acornui.core.asset.cachedGroup
import com.acornui.core.asset.loadAndCacheJson
import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.di.notDisposed
import com.acornui.core.graphic.*
import com.acornui.core.time.onTick
import com.acornui.gl.core.GlState
import com.acornui.graphic.ColorRo
import com.acornui.math.Bounds
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo
import com.acornui.recycle.Clearable


class SpriteAnimation(owner: Owned) : UiComponentImpl(owner), Clearable {

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

	var frameRate: Int = inject(AppConfig).frameRate

	/**
	 * If true, when the animation hits [endFrame] it will loop back to [startFrame]
	 */
	var loops = true

	val tickTime: Float
		get() = 1f / frameRate.toFloat()

	/**
	 * If true, this animation will not progress.
	 */
	var paused: Boolean = false

	private var elapsed: Float = 0f

	var animation: LoadedAnimation? = null
		private set

	init {
		onTick {
			val loadedAnimation = animation
			if (!paused && loadedAnimation != null) {
				val tickTime = tickTime

				elapsed += it
				while (elapsed >= tickTime) {
					// Tick a frame
					elapsed -= tickTime
					val end = if (endFrame < 0) loadedAnimation.frames.lastIndex else endFrame
					if (currentFrame >= end) {
						if (loops)
							currentFrame = startFrame
					} else {
						currentFrame++
					}
					window.requestRender()
				}
			}
		}
	}

	private var group: CachedGroup? = null

	fun setRegion(atlasPath: String, regionName: String) : Deferred<LoadedAnimation> {
		clear()
		group = cachedGroup()
		return loadSpriteAnimation(atlasPath, regionName) then notDisposed {
			loadedAnimation ->
			setAnimation(loadedAnimation)
		}
	}

	override fun clear() {
		group?.dispose()
		group = null
		if (isActive) animation?.refDec()
		animation = null
		invalidateLayout()
	}

	override fun onActivated() {
		super.onActivated()
		animation?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		animation?.refDec()
	}

	fun setAnimation(value: LoadedAnimation) {
		clear()
		animation = value
		if (isActive) value.refInc()
		invalidateLayout()
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val animation = animation ?: return
		var w = 0f
		var h = 0f
		for (i in 0..animation.frames.lastIndex) {
			val r = animation.frames[i]
			if (r.naturalWidth > w) w = r.naturalWidth
			if (r.naturalHeight > h) h = r.naturalHeight
			r.updateVertices(explicitWidth ?: r.naturalWidth, explicitHeight ?: r.naturalHeight)
		}
		out.set(w, h)
	}

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		animation?.frames?.getOrNull(currentFrame - startFrame)?.render(clip, transform, tint)
	}

	override fun dispose() {
		super.dispose()
		group?.dispose()
		group = null
	}
}

data class LoadedAnimation(
		val textures: List<Texture>,
		val frames: List<BasicDrawable>
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

fun Scoped.loadSpriteAnimation(atlasPath: String, regionName: String, group: CachedGroup = cachedGroup()): Deferred<LoadedAnimation> {
	val glState = inject(GlState)
	return async {
		val atlasData = loadAndCacheJson(atlasPath, TextureAtlasDataSerializer, group).await()
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
			val (region, page) = regions[i] ?: continue
			val texture = loadAndCacheAtlasPage(atlasPath, page, group).await()
			if (!textures.contains(texture)) textures.add(texture)
			val atlas = Atlas(glState)
			atlas.setRegionAndTexture(texture, region)
			frames.add(atlas)
		}
		LoadedAnimation(textures, frames)
	}
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

fun Owned.spriteAnimation(atlasPath: String, regionName: String, init: ComponentInit<SpriteAnimation> = {}): SpriteAnimation {
	val s = SpriteAnimation(this)
	s.setRegion(atlasPath, regionName)
	s.init()
	return s
}

fun Owned.spriteAnimation(init: ComponentInit<SpriteAnimation> = {}): SpriteAnimation {
	val s = SpriteAnimation(this)
	s.init()
	return s
}