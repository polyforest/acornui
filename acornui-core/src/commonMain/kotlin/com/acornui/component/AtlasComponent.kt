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

@file:Suppress("unused")

package com.acornui.component

import kotlinx.coroutines.Deferred
import com.acornui.async.catch
import com.acornui.async.then
import com.acornui.asset.CachedGroup
import com.acornui.asset.cachedGroup
import com.acornui.async.globalAsync
import com.acornui.di.Owned
import com.acornui.di.notDisposed
import com.acornui.graphic.*
import com.acornui.logging.Log
import com.acornui.recycle.Clearable

/**
 * A UiComponent that draws a region from a texture atlas.
 * This component will create a NinePatchComponent or a TextureComponent, and apply all virtual padding as defined by
 * the loaded AtlasRegionData.
 *
 * @author nbilyk
 */
open class AtlasComponent(owner: Owned) : RenderableComponent<Atlas>(owner), Clearable {

	var region: AtlasRegionData? = null
		private set

	private var texture: Texture? = null

	override val renderable = Atlas(glState)

	private var group: CachedGroup? = null

	/**
	 * Sets the region of the atlas component.
	 * @param atlasPath The atlas json file.
	 * @param regionName The name of the region within the atlas.
	 *
	 * This load can be canceled using [clear].
	 */
	fun setRegion(atlasPath: String, regionName: String, warnOnNotFound: Boolean = true): Deferred<LoadedAtlasRegion> {
		clear()
		this.group = cachedGroup()
		return globalAsync { loadAndCacheAtlasRegion(atlasPath, regionName, group!!) } then notDisposed {
			loadedRegion ->
			setRegionAndTexture(loadedRegion.texture, loadedRegion.region)
		} catch {
			if (warnOnNotFound)
				Log.warn("Region \"$regionName\" not found in atlas \"$atlasPath\".")
		}
	}

	var blendMode: BlendMode
		get() = renderable.blendMode
		set(value) {
			renderable.blendMode = value
		}

	private fun setRegionAndTexture(texture: Texture, region: AtlasRegionData) {
		this.region = region
		val oldTexture = this.texture
		this.texture = texture
		if (isActive) {
			texture.refInc()
			oldTexture?.refDec()
		}
		renderable.setRegionAndTexture(texture, region)
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
		renderable.clear()
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
	a.setRegion(atlasPath, region).catch { throw it }
	a.init()
	return a
}

/**
 * Creates a texture component and uses it as the contents
 */
fun SingleElementContainer<UiComponent>.contentsAtlas(atlasPath: String, region: String): Deferred<LoadedAtlasRegion> {
	return createOrReuseElement { atlas() }.setRegion(atlasPath, region)
}
