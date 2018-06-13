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

import com.acornui.core.di.Owned
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.math.IntRectangleRo
import com.acornui.math.RectangleRo

interface TextureComponent : UiComponent {

	/**
	 * Loads a texture from the given path.
	 */
	var path: String?

	/**
	 * Sets the texture directly, as opposed to loading a Texture from the asset manager.
	 */
	var texture: Texture?

	/**
	 * If true, the texture's region is rotated.
	 */
	val isRotated: Boolean

	/**
	 * Only applicable for GL backends, sets the blend mode for this component.
	 */
	var blendMode: BlendMode

	/**
	 * Sets the region of the texture to display.
	 */
	fun setRegion(region: RectangleRo, isRotated: Boolean = false) {
		setRegion(region.x, region.y, region.width, region.height, isRotated)
	}

	fun setRegion(region: IntRectangleRo, isRotated: Boolean = false) {
		setRegion(region.x.toFloat(), region.y.toFloat(), region.width.toFloat(), region.height.toFloat(), isRotated)
	}

	/**
	 * Sets the UV coordinates of the image to display.
	 */
	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean = false)

	/**
	 * Sets the region of the texture to display.
	 */
	fun setRegion(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean = false)
}

fun Owned.textureC(init: ComponentInit<GlTextureComponent> = {}): GlTextureComponent {
	val textureComponent = GlTextureComponent(this)
	textureComponent.init()
	return textureComponent
}

fun Owned.textureC(path: String, init: ComponentInit<GlTextureComponent> = {}): GlTextureComponent {
	val textureComponent = GlTextureComponent(this)
	textureComponent.path = path
	textureComponent.init()
	return textureComponent
}

fun Owned.textureC(texture: Texture, init: ComponentInit<GlTextureComponent> = {}): GlTextureComponent {
	val textureComponent = GlTextureComponent(this)
	textureComponent.texture = texture
	textureComponent.init()
	return textureComponent
}

