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

@file:Suppress("unused")

package com.acornui.component

import com.acornui.asset.loadTexture
import com.acornui.async.cancellingJobProp
import com.acornui.async.launchSupervised
import com.acornui.di.Context
import com.acornui.graphic.BlendMode
import com.acornui.graphic.TextureRo
import com.acornui.io.UrlRequestData
import com.acornui.io.toUrlRequestData
import com.acornui.math.IntRectangleRo
import com.acornui.recycle.Clearable
import kotlinx.coroutines.Job
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author nbilyk
 */
class NinePatchComponent(owner: Context) : RenderableComponent<BasicRenderable>(owner), Clearable {

	override val renderable: NinePatch = NinePatch(gl)

	override fun onActivated() {
		super.onActivated()
		renderable.texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		renderable.texture?.refDec()
	}

	/**
	 * @return Returns the texture rendered. This will either be the explicit texture set via [texture] or the loaded
	 * texture from [path].
	 */
	val texture: TextureRo?
		get() = renderable.texture

	var explicitTexture: TextureRo? = null
		private set

	/**
	 * The [Job] set from loading a texture from a path.
	 * This may be cancelled to stop loading.
	 */
	var loaderJob: Job? by cancellingJobProp()
		private set

	/**
	 * Sets the texture directly, as opposed to loading a Texture from [path].
	 */
	fun texture(value: TextureRo?) {
		if (explicitTexture == value) return
		clear()
		explicitTexture = value
		setTextureInternal(value)
	}

	/**
	 * Loads the texture from the given url request and sets the texture on completion.
	 * @return Returns the cancellable, supervised [Job] for loading and setting the texture.
	 */
	fun texture(path: String): Job = texture(path.toUrlRequestData())

	fun texture(requestData: UrlRequestData): Job {
		clear()
		loaderJob = launchSupervised {
			setTextureInternal(loadTexture(requestData))
		}
		return loaderJob!!
	}

	val naturalWidth: Float
		get() = renderable.naturalWidth

	val naturalHeight: Float
		get() = renderable.naturalHeight

	var blendMode: BlendMode
		get() = renderable.blendMode
		set(value) {
			renderable.blendMode = value
			window.requestRender()
		}

	val splitLeft: Float
		get() = renderable.splitLeft
	val splitTop: Float
		get() = renderable.splitTop
	val splitRight: Float
		get() = renderable.splitRight
	val splitBottom: Float
		get() = renderable.splitBottom

	private fun setTextureInternal(value: TextureRo?) {
		if (renderable.texture == value) return
		val oldTexture = renderable.texture
		renderable.texture = value
		if (isActive) {
			value?.refInc()
			oldTexture?.refDec()
		}
		invalidateLayout()
	}

	fun region(region: IntRectangleRo, isRotated: Boolean) {
		region(region.x.toFloat(), region.y.toFloat(), region.width.toFloat(), region.height.toFloat(), isRotated)
	}

	fun region(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean) {
		renderable.region(x, y, width, height, isRotated)
		invalidateLayout()
	}

	fun uv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean) {
		renderable.region(u,  v, u2, v2, isRotated)
		invalidateLayout()
	}

	fun split(splitLeft: Int, splitTop: Int, splitRight: Int, splitBottom: Int) {
		split(splitLeft.toFloat(), splitTop.toFloat(), splitRight.toFloat(), splitBottom.toFloat())
	}

	fun split(splitLeft: Float, splitTop: Float, splitRight: Float, splitBottom: Float) {
		renderable.split(splitLeft, splitTop, splitRight, splitBottom)
		invalidateLayout()
	}

	override fun clear() {
		loaderJob = null
		explicitTexture = null
		setTextureInternal(null)
	}

	override fun dispose() {
		clear()
		super.dispose()
	}
}

inline fun Context.ninePatch(init: ComponentInit<NinePatchComponent> = {}): NinePatchComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val ninePatch = NinePatchComponent(this)
	ninePatch.init()
	return ninePatch
}
