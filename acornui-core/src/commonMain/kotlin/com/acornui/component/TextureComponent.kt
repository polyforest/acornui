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

import com.acornui.asset.CachedGroup
import com.acornui.asset.cacheAsync
import com.acornui.asset.cachedGroup
import com.acornui.asset.loadTexture
import com.acornui.async.catch
import com.acornui.async.then
import com.acornui.di.Owned
import com.acornui.graphic.BlendMode
import com.acornui.graphic.Texture
import com.acornui.graphic.TextureRo
import com.acornui.logging.Log
import com.acornui.math.IntRectangleRo
import com.acornui.math.RectangleRo
import com.acornui.recycle.Clearable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A UiComponent representing a single Texture.
 * @author nbilyk
 */
open class TextureComponent(owner: Owned) : RenderableComponent<Sprite>(owner), Clearable {

	override val renderable: Sprite = Sprite(glState)

	/**
	 * Sets the dpi scaling on the Sprite.
	 */
	fun setSpriteScaling(scaleX: Float, scaleY: Float) {
		renderable.setScaling(scaleX, scaleY)
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

	constructor (owner: Owned, path: String) : this(owner) {
		this.path = path
	}

	constructor (owner: Owned, texture: Texture) : this(owner) {
		this.texture = texture
	}

	private var cached: CachedGroup? = null

	/**
	 * Loads a texture from the given path.
	 * When the texture is done loading, it can be accessed via [internalTexture].
	 */
	var path: String? = null
		set(value) {
			if (field == value) return
			field = value
			texture = null
			cached?.dispose()
			cached = null
			if (value != null) {
				cached = cachedGroup()
				cached!!.cacheAsync(value) {
					loadTexture(value)
				}.then {
					setTextureInternal(it)
				} catch(errorHandler)
			}
		}

	/**
	 * Sets the texture directly, as opposed to loading a Texture from [path].
	 * [internalTexture]
	 */
	var texture: Texture? = null
		set(value) {
			field = value
			path = null
			setTextureInternal(value)
		}

	protected open fun setTextureInternal(value: Texture?) {
		if (renderable.texture == value) return
		val oldTexture = renderable.texture
		if (isActive)
			oldTexture?.refDec()
		renderable.texture = value
		if (isActive)
			renderable.texture?.refInc()
		invalidateLayout()
	}

	/**
	 * Returns the texture rendered.
	 */
	val internalTexture: TextureRo?
		get() = renderable.texture

	/**
	 * If true, the texture's region is rotated.
	 */
	val isRotated: Boolean
		get() = renderable.isRotated

	override fun onActivated() {
		super.onActivated()
		renderable.texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		renderable.texture?.refDec()
	}

	/**
	 * Sets the UV coordinates of the image to display.
	 */
	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean = false) {
		renderable.setUv(u, v, u2, v2, isRotated)
		invalidate(ValidationFlags.LAYOUT)
	}

	/**
	 * Sets the region of the texture to display.
	 */
	fun setRegion(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean = false) {
		renderable.setRegion(x, y, width, height, isRotated)
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

	override fun clear() {
		cached?.dispose()
		cached = null
		setTextureInternal(null)
		path = null
		texture = null
	}

	override fun dispose() {
		clear()
		super.dispose()
	}

	companion object {

		/**
		 * The error handler when loading textures via [TextureComponent.path] and the texture is not found.
		 */
		var errorHandler: (e: Throwable) -> Unit = { Log.error(it) }
	}
}

inline fun Owned.textureC(init: ComponentInit<TextureComponent> = {}): TextureComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val textureComponent = TextureComponent(this)
	textureComponent.init()
	return textureComponent
}

inline fun Owned.textureC(path: String, init: ComponentInit<TextureComponent> = {}): TextureComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val textureComponent = TextureComponent(this)
	textureComponent.path = path
	textureComponent.init()
	return textureComponent
}

inline fun Owned.textureC(texture: Texture, init: ComponentInit<TextureComponent> = {}): TextureComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val textureComponent = TextureComponent(this)
	textureComponent.texture = texture
	textureComponent.init()
	return textureComponent
}

