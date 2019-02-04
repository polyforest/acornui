/*
 * Copyright 2015 Nicholas Bilyk
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
import com.acornui.core.asset.AssetType
import com.acornui.core.asset.CachedGroup
import com.acornui.core.asset.cachedGroup
import com.acornui.core.asset.loadAndCache
import com.acornui.core.di.Owned
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Texture
import com.acornui.math.IntRectangleRo
import com.acornui.math.RectangleRo

/**
 * A UiComponent representing a single Texture.
 * @author nbilyk
 */
open class TextureComponent(owner: Owned) : DrawableComponent(owner) {

	override val drawable: Sprite = Sprite()
	
	/**
	 * If true, the normal and indices will be reversed.
	 */
	var useAsBackFace: Boolean
		get() = drawable.useAsBackFace
		set(value) { 
			drawable.useAsBackFace = value
		}

	val naturalWidth: Float
		get() = drawable.naturalWidth

	val naturalHeight: Float
		get() = drawable.naturalHeight

	var blendMode: BlendMode
		get() = drawable.blendMode
		set(value) {
			drawable.blendMode = value
			window.requestRender()
		}

	constructor (owner: Owned, path: String) : this(owner) {
		this.path = path
	}

	constructor (owner: Owned, texture: Texture) : this(owner) {
		this.texture = texture
	}

	private var cached: CachedGroup? = null

	private var _path: String? = null

	/**
	 * Loads a texture from the given path.
	 */
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

	/**
	 * Sets the texture directly, as opposed to loading a Texture from the asset manager.
	 */
	var texture: Texture?
		get() = drawable.texture
		set(value) {
			path = null
			_setTexture(value)
		}

	protected open fun _setTexture(value: Texture?) {
		if (drawable.texture == value) return
		val oldTexture = drawable.texture
		if (isActive)
			oldTexture?.refDec()
		drawable.texture = value
		if (isActive)
			drawable.texture?.refInc()
		invalidateLayout()
	}

	/**
	 * If true, the texture's region is rotated.
	 */
	val isRotated: Boolean
		get() = drawable.isRotated

	override fun onActivated() {
		super.onActivated()
		drawable.texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		drawable.texture?.refDec()
	}

	/**
	 * Sets the UV coordinates of the image to display.
	 */
	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean = false) {
		drawable.setUv(u, v, u2, v2, isRotated)
		invalidate(ValidationFlags.LAYOUT)
	}

	/**
	 * Sets the region of the texture to display.
	 */
	fun setRegion(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean = false) {
		drawable.setRegion(x, y, width, height, isRotated)
		invalidate(ValidationFlags.LAYOUT)
	}

	/**
	 * Sets the region of the texture to display.
	 */
	fun setRegion(region: RectangleRo, isRotated: Boolean = false) {
		setRegion(region.x, region.y, region.width, region.height, isRotated)
	}

	fun setRegion(region: IntRectangleRo, isRotated: Boolean = false) {
		setRegion(region.x.toFloat(), region.y.toFloat(), region.width.toFloat(), region.height.toFloat(), isRotated)
	}

	override fun dispose() {
		path = null
		super.dispose()
	}
}

fun Owned.textureC(init: ComponentInit<TextureComponent> = {}): TextureComponent {
	val textureComponent = TextureComponent(this)
	textureComponent.init()
	return textureComponent
}

fun Owned.textureC(path: String, init: ComponentInit<TextureComponent> = {}): TextureComponent {
	val textureComponent = TextureComponent(this)
	textureComponent.path = path
	textureComponent.init()
	return textureComponent
}

fun Owned.textureC(texture: Texture, init: ComponentInit<TextureComponent> = {}): TextureComponent {
	val textureComponent = TextureComponent(this)
	textureComponent.texture = texture
	textureComponent.init()
	return textureComponent
}

