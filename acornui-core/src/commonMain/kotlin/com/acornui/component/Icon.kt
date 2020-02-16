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

import com.acornui.asset.cachedGroup
import com.acornui.asset.loadTexture
import com.acornui.async.launchSupervised
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun Context.iconAtlas(init: ComponentInit<AtlasComponent> = {}): AtlasComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val atlasComponent = IconAtlasComponent(this)
	atlasComponent.init()
	return atlasComponent
}

inline fun Context.iconAtlas(atlasPath: String, region: String, init: ComponentInit<AtlasComponent> = {}): AtlasComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val iconAtlas = iconAtlas {
		region(atlasPath, region)
	}
	iconAtlas.init()
	return iconAtlas
}

fun Context.iconImage(imagePath: String, init: ComponentInit<Image> = {}): Image  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val image = IconImageComponent(this)
	image.element = textureC {
		launchSupervised {
			texture(cachedGroup().cacheAsync(imagePath) {
				loadTexture(imagePath).apply {
					filterMag = TextureMagFilter.LINEAR
					filterMin = TextureMinFilter.LINEAR
				}
			}.await())
		}
	}
	image.init()
	return image
}

class IconStyle : StyleBase() {

	override val type = Companion

	var iconColor: ColorRo by prop(Color.WHITE)

	companion object : StyleType<IconStyle>
}

class IconAtlasComponent(owner: Context) : AtlasComponent(owner) {
	init {
		val style = bind(IconStyle())
		validation.addDependencies(ValidationFlags.COLOR_TINT, newDependencies = ValidationFlags.STYLES)
		watch(style) {
			colorTint = it.iconColor
		}
	}
}

class IconImageComponent(owner: Context) : Image(owner) {
	init {
		val style = bind(IconStyle())
		validation.addDependencies(ValidationFlags.COLOR_TINT, newDependencies = ValidationFlags.STYLES)
		watch(style) {
			colorTint = it.iconColor
		}
	}
}