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

import com.acornui.async.catch
import com.acornui.async.then
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.asset.AssetType
import com.acornui.core.asset.cachedGroup
import com.acornui.core.asset.loadAndCache
import com.acornui.core.di.Owned
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo

fun Owned.iconAtlas(init: ComponentInit<AtlasComponent> = {}): AtlasComponent {
	return object : AtlasComponent(this@iconAtlas) {
		init {
			val style = bind(IconStyle())
			watch(style) {
				colorTint = it.iconColor
			}
			init()
		}
	}
}

fun Owned.iconAtlas(atlasPath: String, region: String, init: ComponentInit<AtlasComponent> = {}): AtlasComponent {
	return iconAtlas {
		setRegion(atlasPath, region)
		init()
	}
}

fun Owned.iconImage(imagePath: String, init: ComponentInit<Image> = {}): Image {
	return object : Image(this@iconImage) {
		init {
			element = textureC {
				loadAndCache(imagePath, AssetType.TEXTURE, cachedGroup()).then {
					it.filterMag = TextureMagFilter.LINEAR
					it.filterMin = TextureMinFilter.LINEAR
					texture = it
				} catch(TextureComponent.errorHandler)
			}
			val style = bind(IconStyle())
			watch(style) {
				colorTint = it.iconColor
			}
			init()
		}
	}
}

class IconStyle : StyleBase() {

	override val type = Companion

	var iconColor: ColorRo by prop(Color.WHITE)

	companion object : StyleType<IconStyle>
}
