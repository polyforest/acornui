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

import com.acornui.asset.CacheSet
import com.acornui.asset.cacheSet
import com.acornui.async.launchSupervised
import com.acornui.di.Context
import com.acornui.graphic.*
import com.acornui.recycle.Clearable
import kotlinx.coroutines.Job
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A UiComponent that draws a region from a texture atlas.
 * This component will create a NinePatchComponent or a TextureComponent, and apply all virtual padding as defined by
 * the loaded AtlasRegionData.
 *
 * @author nbilyk
 */
open class AtlasComponent(owner: Context) : RenderableComponent<Atlas>(owner), Clearable {

	override val renderable = Atlas(gl)

	val region: AtlasRegion?
		get() = renderable.region

	val regionData: AtlasRegionData?
		get() = renderable.region?.data

	val texture: Texture?
		get() = renderable.region?.texture

	protected open fun setRegionInternal(value: AtlasRegion?) {
		if (region == value) return
		val oldTexture = region?.texture
		if (isActive) {
			value?.texture?.refInc()
			oldTexture?.refDec()
		}
		renderable.region(value)
		invalidateLayout()
	}

	fun region(value: AtlasRegion?) {
		clear()
		setRegionInternal(value)
	}

	fun region(texture: Texture, data: AtlasRegionData) = region(AtlasRegion(texture, data))

	/**
	 * The [Job] set from loading an atlas from a path.
	 * This may be cancelled to stop loading.
	 */
	var loaderJob: Job? = null
		private set(value) {
			field?.cancel()
			field = value
		}

	private var group: CacheSet? = null
		set(value) {
			field?.dispose()
			field = value
		}

	/**
	 * Sets the region of the atlas component.
	 * @param atlasPath The atlas json file.
	 * @param regionName The name of the region within the atlas.
	 *
	 * @return Returns a supervised [Job] handle representing completion after the region is loaded and set.
	 */
	fun region(atlasPath: String, regionName: String): Job {
		clear()
		group = cacheSet()
		return launchSupervised {
			setRegionInternal(loadAndCacheAtlasRegion(atlasPath, regionName, group!!))
		}.also {
			loaderJob = it
		}
	}

	var blendMode: BlendMode
		get() = renderable.blendMode
		set(value) {
			renderable.blendMode = value
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
		group = null
		setRegionInternal(null)
		renderable.clear()
		invalidateLayout()
	}

	override fun dispose() {
		clear()
		super.dispose()
	}
}

inline fun Context.atlas(init: ComponentInit<AtlasComponent> = {}): AtlasComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val a = AtlasComponent(this)
	a.init()
	return a
}

inline fun Context.atlas(atlasPath: String, region: String, init: ComponentInit<AtlasComponent> = {}): AtlasComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val a = AtlasComponent(this)
	a.region(atlasPath, region)
	a.init()
	return a
}

/**
 * Creates a texture component and uses it as the contents
 */
fun SingleElementContainer<UiComponent>.contentsAtlas(atlasPath: String, region: String): Job {
	return createOrReuseElement { atlas() }.region(atlasPath, region)
}
