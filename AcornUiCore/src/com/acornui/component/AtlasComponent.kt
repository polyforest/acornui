/*
 * Copyright 2018 Nicholas Bilyk
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

import com.acornui.async.Deferred
import com.acornui.async.then
import com.acornui.collection.Clearable
import com.acornui.collection.tuple
import com.acornui.core.asset.CachedGroup
import com.acornui.core.asset.cachedGroup
import com.acornui.core.asset.loadAndCacheJson
import com.acornui.core.di.Owned
import com.acornui.core.graphic.*

/**
 * A UiComponent that draws a region from a texture atlas.
 * This component will create a NinePatchComponent or a TextureComponent, and apply all virtual padding as defined by
 * the loaded AtlasRegionData.
 *
 * @author nbilyk
 */
open class AtlasComponent(owner: Owned) : DrawableComponent(owner), Clearable {

	private var _region: AtlasRegionData? = null

	val region: AtlasRegionData?
		get() = _region

	private var texture: Texture? = null

	override val drawable = Atlas()

	private var group: CachedGroup? = null

	/**
	 * Sets the region of the atlas component.
	 * @param atlasPath The atlas json file.
	 * @param regionName The name of the region within the atlas.
	 *
	 * This load can be canceled using [clear].
	 */
	fun setRegion(atlasPath: String, regionName: String): Deferred<Pair<Texture, AtlasRegionData>> {
		clear()
		group = cachedGroup()
		return async {
			val atlasData = loadAndCacheJson(atlasPath, TextureAtlasDataSerializer, group!!).await()
			val (page, region) = atlasData.findRegion(regionName) ?: throw Exception("Region '$regionName' not found in atlas.")
			val texture = loadAndCacheAtlasPage(atlasPath, page, group!!).await()
			texture tuple region
		} then {
			texture, region ->
			setRegionAndTexture(texture, region)
		}
	}

	var blendMode: BlendMode
		get() = drawable.blendMode
		set(value) {
			drawable.blendMode = value
		}

	/**
	 * If true, the normal and indices will be reversed.
	 */
	var useAsBackFace: Boolean
		get() = drawable.useAsBackFace
		set(value) {
			drawable.useAsBackFace = value
		}

	private fun setRegionAndTexture(texture: Texture, region: AtlasRegionData) {
		this._region = region
		val oldTexture = this.texture
		this.texture = texture
		if (isActive) {
			texture.refInc()
			oldTexture?.refDec()
		}
		drawable.setRegionAndTexture(texture, region)
		invalidateLayout()
	}

	override fun onActivated() {
		super.onActivated()
		texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		texture?.refDec()
	}

	override fun clear() {
		group?.dispose()
		group = null
		if (isActive) texture?.refDec()
		texture = null
		drawable.clear()
		invalidateLayout()
	}

	override fun dispose() {
		clear()
		super.dispose()
	}
}

fun Owned.atlas(init: ComponentInit<AtlasComponent> = {}): AtlasComponent {
	val a = AtlasComponent(this)
	a.init()
	return a
}

fun Owned.atlas(atlasPath: String, region: String, init: ComponentInit<AtlasComponent> = {}): AtlasComponent {
	val a = AtlasComponent(this)
	a.setRegion(atlasPath, region)
	a.init()
	return a
}

/**
 * Creates a texture component and uses it as the contents
 */
fun ElementContainer<UiComponent>.contentsAtlas(atlasPath: String, region: String) {
	createOrReuseContents { atlas() }.setRegion(atlasPath, region)
}