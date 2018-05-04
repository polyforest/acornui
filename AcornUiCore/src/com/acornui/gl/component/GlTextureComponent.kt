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

package com.acornui.gl.component

import com.acornui.async.then
import com.acornui.component.TextureComponent
import com.acornui.component.UiComponentImpl
import com.acornui.component.ValidationFlags
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.CachedGroup
import com.acornui.core.assets.cachedGroup
import com.acornui.core.assets.loadAndCache
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.gl.core.GlState
import com.acornui.math.Bounds
import com.acornui.math.MinMaxRo

/**
 * A UiComponent representing a single Texture.
 *
 * Example:
 * val textureComponent = TextureComponent()
 * textureComponent.path("images/image128.jpg")
 * D.stage.addChild(textureComponent)
 *
 * @author nbilyk
 */
open class GlTextureComponent(owner: Owned) : UiComponentImpl(owner), TextureComponent {

	private val glState = inject(GlState)

	private val sprite = Sprite()

	init {
		validation.addNode(1 shl 16, ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM or ValidationFlags.CONCATENATED_TRANSFORM) { validateVertices() }
	}

	constructor (owner: Owned, path: String) : this(owner) {
		this.path = path
	}

	constructor (owner: Owned, texture: Texture) : this(owner) {
		this.texture = texture
	}

	private var cached: CachedGroup? = null

	private var _path: String? = null
	final override var path: String?
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

	override var texture: Texture?
		get() = sprite.texture
		set(value) {
			path = null
			_setTexture(value)
		}

	protected open fun _setTexture(value: Texture?) {
		if (sprite.texture == value) return
		val oldTexture = sprite.texture
		if (isActive) {
			oldTexture?.refDec()
		}
		sprite.texture = value
		if (isActive) {
			sprite.texture?.refInc()
		}
		invalidate(ValidationFlags.LAYOUT)
	}

	override var isRotated: Boolean
		get() = sprite.isRotated
		set(value) {
			if (sprite.isRotated == value) return
			sprite.isRotated = value
			invalidate(ValidationFlags.LAYOUT)
		}

	override var blendMode: BlendMode
		get() = sprite.blendMode
		set(value) {
			if (sprite.blendMode == value) return
			sprite.blendMode = value
			window.requestRender()
		}

	override fun onActivated() {
		super.onActivated()
		sprite.texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		sprite.texture?.refDec()
	}

	override fun setUv(u: Float, v: Float, u2: Float, v2: Float) {
		sprite.setUv(u, v, u2, v2)
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun setRegion(x: Float, y: Float, width: Float, height: Float) {
		sprite.setRegion(x, y, width, height)
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		sprite.updateUv()
		out.width = explicitWidth ?: sprite.naturalWidth
		out.height = explicitHeight ?: sprite.naturalHeight
	}

	private fun validateVertices() {
		sprite.updateWorldVertices(concatenatedTransform, width, height, z = 0f)
	}

	override fun draw(viewport: MinMaxRo) {
		glState.camera(camera)
		sprite.draw(glState, concatenatedColorTint)
	}

	override fun dispose() {
		path = null
		super.dispose()
	}
}