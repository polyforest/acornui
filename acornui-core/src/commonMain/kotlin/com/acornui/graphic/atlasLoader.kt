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

package com.acornui.graphic

import com.acornui.asset.CachedGroup
import com.acornui.asset.loadTexture
import com.acornui.di.Context
import com.acornui.io.file.Path
import kotlinx.coroutines.Deferred

suspend fun Context.loadAndCacheAtlasPage(atlasPath: String, page: AtlasPageData, group: CachedGroup): Texture {
	val atlasFile = Path(atlasPath)
	val textureFile = atlasFile.sibling(page.texturePath)
	return group.cacheAsync(textureFile.value) {
		page.configure(loadTexture(textureFile.value))
	}.await()
}

fun Context.loadAndCacheAtlasPageAsync(atlasPath: String, page: AtlasPageData, group: CachedGroup): Deferred<Texture> {
	val atlasFile = Path(atlasPath)
	val textureFile = atlasFile.sibling(page.texturePath)
	return group.cacheAsync(textureFile.value) {
		page.configure(loadTexture(textureFile.value))
	}
}