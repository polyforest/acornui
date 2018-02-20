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

package com.acornui.core.graphics

import com.acornui.action.Decorator
import com.acornui.async.Deferred
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.CachedGroup
import com.acornui.core.assets.loadAndCache
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.io.file.Files

data class AtlasPageDecorator(val page: AtlasPageData) : Decorator<Texture, Texture> {
	override fun decorate(target: Texture): Texture {
		target.pixelFormat = page.pixelFormat
		target.filterMin = page.filterMin
		target.filterMag = page.filterMag
		target.hasWhitePixel = page.hasWhitePixel
		return target
	}
}

fun Scoped.loadAndCacheAtlasPage(atlasPath: String, page: AtlasPageData, group: CachedGroup): Deferred<Texture> {
	val files = inject(Files)
	val atlasFile = files.getFile(atlasPath) ?: throw Exception("File not found: $atlasPath")
	val textureFile = atlasFile.siblingFile(page.texturePath)
			?: throw Exception("File not found: ${page.texturePath} relative to: ${atlasFile.parent?.path}")

	return loadAndCache(textureFile.path, AssetType.TEXTURE, AtlasPageDecorator(page), group)
}
