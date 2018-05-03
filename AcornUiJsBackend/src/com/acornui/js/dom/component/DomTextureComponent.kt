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

package com.acornui.js.dom.component

import com.acornui.async.then
import com.acornui.component.TextureComponent
import com.acornui.component.UiComponentImpl
import com.acornui.component.ValidationFlags
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.CachedGroup
import com.acornui.core.assets.cachedGroup
import com.acornui.core.assets.loadAndCache
import com.acornui.core.di.Owned
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.js.dom.DomTexture
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import org.w3c.dom.HTMLDivElement
import kotlin.browser.document

// TODO: Support rotate
open class DomTextureComponent(
		owner: Owned,
		native: DomComponent = DomComponent("div")
) : UiComponentImpl(owner, native), TextureComponent {

	/**
	 * Either represents uv values, or pixel coordinates, depending on _isUv
	 * left, top, right, bottom
	 */
	protected var _region: FloatArray = floatArrayOf(0f, 0f, 1f, 1f)

	/**
	 * True if the region represents 0-1 uv coordinates.
	 * False if the region represents pixel coordinates.
	 */
	protected var _isUv: Boolean = true

	protected var _isRotated: Boolean = false

	private var _texture: DomTexture? = null

	private var imageElement: HTMLDivElement = document.createElement("div") as HTMLDivElement

	override var isRotated: Boolean
		get() = _isRotated
		set(value) {
			if (_isRotated == value) return
			_isRotated = value
			if (value) throw UnsupportedOperationException("Currently the DOM backend does not support texture rotation. Change the packSettings to not allow rotation.")
			invalidate(ValidationFlags.PROPERTIES)
		}

	init {
		imageElement.style.apply {
			transformOrigin = "0 0"
			overflowX = "hidden"
			overflowY = "hidden"
		}
		native.element.appendChild(imageElement)
	}

	private var cached: CachedGroup? = null

	private var _path: String? = null
	override final var path: String?
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
	 * Dom side does not support blend modes.
	 */
	override var blendMode: BlendMode
		get() = BlendMode.NORMAL
		set(value) {}

	override var texture: Texture?
		get() = _texture
		set(value) {
			path = null
			_setTexture(value)
		}

	protected fun _setTexture(value: Texture?) {
		value as DomTexture?
		if (_texture == value) return
		val oldTexture = _texture
		if (isActive) {
			oldTexture?.refDec()
		}
		_texture = value
		if (isActive) {
			_texture?.refInc()
		}
		invalidate(ValidationFlags.PROPERTIES)
	}

	override fun setUv(u: Float, v: Float, u2: Float, v2: Float) {
		_region[0] = u
		_region[1] = v
		_region[2] = u2
		_region[3] = v2
		_isUv = true
		invalidate(ValidationFlags.PROPERTIES)
	}

	override fun setRegion(x: Float, y: Float, width: Float, height: Float) {
		_region[0] = x
		_region[1] = y
		_region[2] = width + x
		_region[3] = height + y
		_isUv = false
		invalidate(ValidationFlags.PROPERTIES)
	}

	override fun onActivated() {
		super.onActivated()
		_texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		_texture?.refDec()
	}

	private var regionX: Float = 0f
	private var regionY: Float = 0f
	private var regionW: Float = 0f
	private var regionH: Float = 0f

	override fun updateProperties() {
		if (_texture == null) return
		val t = _texture!!
		if (_isUv) {
			regionX = _region[0] * t.width
			regionY = _region[1] * t.height
			regionW = MathUtils.abs(_region[2] - _region[0]) * t.width
			regionH = MathUtils.abs(_region[3] - _region[1]) * t.height
		} else {
			regionX = _region[0]
			regionY = _region[1]
			regionW = MathUtils.abs(_region[2] - _region[0])
			regionH = MathUtils.abs(_region[3] - _region[1])
		}

		val url = _texture!!.src
		imageElement.style.apply {
			background = "url($url) -${regionX}px -${regionY}px"
			width = "${regionW}px"
			height = "${regionH}px"
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (_texture == null) return

		if (_isRotated) {
			out.width = explicitWidth ?: regionH
			out.height = explicitHeight ?: regionW
		} else {
			out.width = explicitWidth ?: regionW
			out.height = explicitHeight ?: regionH
		}
		imageElement.style.transform = "scale(${out.width / regionW}, ${out.height / regionH})"
	}

	override fun dispose() {
		path = null
		super.dispose()
	}
}