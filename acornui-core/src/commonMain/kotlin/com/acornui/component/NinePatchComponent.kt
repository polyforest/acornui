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

package com.acornui.component

import com.acornui.async.then
import com.acornui.core.Renderable
import com.acornui.core.asset.AssetType
import com.acornui.core.asset.CachedGroup
import com.acornui.core.asset.cachedGroup
import com.acornui.core.asset.loadAndCache
import com.acornui.core.di.Owned
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Texture
import com.acornui.math.IntRectangleRo

/**
 * @author nbilyk
 */
class NinePatchComponent(owner: Owned) : RenderableComponent<Renderable>(owner) {

	override val renderable: NinePatch = NinePatch(glState)

	private var cached: CachedGroup? = null

	override fun onActivated() {
		super.onActivated()
		renderable.texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		renderable.texture?.refDec()
	}

	private var _path: String? = null
	var path: String?
		get() = _path
		set(value) {
			if (_path == value) return
			cached?.dispose()
			cached = cachedGroup()
			if (value != null) {
				loadAndCache(value, AssetType.TEXTURE, cached!!).then {
					_setTexture(it)
				}
			} else {
				_setTexture(null)
			}
		}

	var texture: Texture?
		get() = renderable.texture
		set(value) {
			if (renderable.texture == value) return
			path = null
			_setTexture(value)
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

	private fun _setTexture(value: Texture?) {
		if (renderable.texture == value) return
		val oldTexture = renderable.texture
		if (isActive) {
			oldTexture?.refDec()
		}
		renderable.texture = value
		if (isActive) {
			renderable.texture?.refInc()
		}
		invalidateLayout()
	}

	fun setRegion(region: IntRectangleRo, isRotated: Boolean) {
		setRegion(region.x.toFloat(), region.y.toFloat(), region.width.toFloat(), region.height.toFloat(), isRotated)
	}

	fun setRegion(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean) {
		renderable.setRegion(x, y, width, height, isRotated)
		invalidateLayout()
	}

	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean) {
		renderable.setRegion(u,  v, u2, v2, isRotated)
		invalidateLayout()
	}

	fun split(splitLeft: Int, splitTop: Int, splitRight: Int, splitBottom: Int) {
		split(splitLeft.toFloat(), splitTop.toFloat(), splitRight.toFloat(), splitBottom.toFloat())
	}

	fun split(splitLeft: Float, splitTop: Float, splitRight: Float, splitBottom: Float) {
		renderable.split(splitLeft, splitTop, splitRight, splitBottom)
		invalidateLayout()
	}

	override fun dispose() {
		path = null
		super.dispose()
	}
}

fun Owned.ninePatch(init: ComponentInit<NinePatchComponent> = {}): NinePatchComponent {
	val ninePatch = NinePatchComponent(this)
	ninePatch.init()
	return ninePatch
}
