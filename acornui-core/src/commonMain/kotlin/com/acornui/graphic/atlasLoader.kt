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
import com.acornui.asset.cacheAsync
import com.acornui.asset.loadTexture
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.io.file.Files
import kotlinx.coroutines.Deferred

fun Scoped.loadAndCacheAtlasPage(atlasPath: String, page: AtlasPageData, group: CachedGroup): Deferred<Texture> {
	val files = inject(Files)
	val atlasFile = files.getFile(atlasPath) ?: throw Exception("File not found: $atlasPath")
	val textureFile = atlasFile.siblingFile(page.texturePath)
			?: throw Exception("File not found: ${page.texturePath} relative to: ${atlasFile.parent?.path}")

	return group.cacheAsync(textureFile.path) {
		page.configure(loadTexture(textureFile.path))
	}
}