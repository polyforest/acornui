/*
 * Copyright 2016 Nicholas Bilyk
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

package com.esotericsoftware.spine.component

import com.acornui.async.Deferred
import com.acornui.async.async
import com.acornui.core.assets.AssetTypes
import com.acornui.core.assets.load
import com.acornui.core.assets.loadJson
import com.acornui.core.di.Scoped
import com.acornui.core.graphics.*
import com.esotericsoftware.spine.data.SkeletonData
import com.esotericsoftware.spine.data.SkeletonDataSerializer


class LoadedSkeleton(
		val skeletonData: SkeletonData,
		val textureAtlasData: TextureAtlasData,

		/**
		 * A map of loaded skins.
		 * This will be a map of the skin name to the loaded skin.
		 */
		val loadedSkins: Map<String, LoadedSkin>
)

class LoadedSkin(

		/**
		 * A map of texture path (relative to the atlas json) to texture objects.
		 * @see getTexture
		 */
		val pageTextures: Map<String, Texture>
) {

	/**
	 * Retrieves the texture for the given atlas page.
	 */
	fun getTexture(page: AtlasPageData): Texture {
		return pageTextures[page.texturePath] ?: throw Exception("Atlas page ${page.texturePath} not found in loaded skeleton.")
	}
}

/**
 * Loads the skeleton from the specified JSON file and texture atlas.
 * @param skins A list of skins to load by name. If this is null, all skins will be loaded.
 */
fun Scoped.loadSkeleton(skeletonDataPath: String, textureAtlasPath: String, skins: List<String>? = null): Deferred<LoadedSkeleton> = async {
	val skeletonDataLoader = loadJson(skeletonDataPath, SkeletonDataSerializer)
	val textureAtlasLoader = loadJson(textureAtlasPath, TextureAtlasDataSerializer)
	val loadedSkins = HashMap<String, LoadedSkin>()

	val skeletonData = skeletonDataLoader.await()
	val textureAtlasData = textureAtlasLoader.await()
	val skinsToLoad = skins ?: skeletonData.skins.keys

	val skinsDirectory = textureAtlasPath.substringBeforeLast("/")
	for (skinName in skinsToLoad) {
		loadedSkins[skinName] = loadSkeletonSkin(skeletonData, textureAtlasData, skinName, skinsDirectory).await()
	}
	LoadedSkeleton(skeletonData, textureAtlasData, loadedSkins)
}

fun Scoped.loadSkeletonSkin(
		skeletonData: SkeletonData,
		textureAtlasData: TextureAtlasData,
		skin: String,
		skinsDirectory: String
): Deferred<LoadedSkin> = async {

	val skinData = skeletonData.findSkin(skin) ?: throw Exception("Could not find skin $skin")
	val pageTextures = HashMap<String, Texture>()
	for (i in skinData.attachments.keys) {
		val (page, _) = textureAtlasData.findRegion(i.attachmentName) ?: throw Exception("Region ${i.attachmentName} not found in atlas.")
		if (!pageTextures.contains(page.texturePath)) {
			val pagePath = "$skinsDirectory/${page.texturePath}"
			pageTextures[page.texturePath] = AtlasPageDecorator(page).decorate(load(pagePath, AssetTypes.TEXTURE).await())
		}
	}
	LoadedSkin(pageTextures)

}