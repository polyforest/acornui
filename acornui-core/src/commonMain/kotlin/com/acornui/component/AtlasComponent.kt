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
import com.acornui.collection.sortedInsertionIndex
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.own
import com.acornui.graphic.*
import com.acornui.logging.Log
import com.acornui.recycle.Clearable
import com.acornui.signal.Signal1
import kotlinx.coroutines.launch
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

	private val _regionChanged = own(Signal1<AtlasRegion?>())

	/**
	 * Dispatched after the atlas region has changed.
	 */
	val regionChanged = _regionChanged.asRo()

	val region: AtlasRegion?
		get() = renderable.region

	val regionData: AtlasRegionData?
		get() = renderable.region?.data

	val texture: Texture?
		get() = renderable.region?.texture

	val dpiStyle = bind(DpiStyle())

	private var loader: AtlasRegionLoader? = null

	private var currentLoadId = 0
	private var currentDensity: Float = -1f

	private var cacheSet: CacheSet? = null
		set(value) {
			field?.dispose()
			field = value
		}

	init {
		watch(dpiStyle) {
			load()
		}
	}

	protected open fun setRegionInternal(value: AtlasRegion?) {
		if (region == value) return
		val oldTexture = region?.texture
		if (isActive) {
			value?.texture?.refInc()
			oldTexture?.refDec()
		}
		renderable.region(value)
		invalidateLayout()
		_regionChanged.dispatch(value)
	}

	/**
	 * Sets the atlas region.
	 * If there were any textures loading, they will be cancelled.
	 * @param value The atlas region to set.
	 * @param scaleX The x pixel density.
	 * @param scaleY The y pixel density.
	 */
	fun region(value: AtlasRegion?, scaleX: Float = 1f, scaleY: Float = 1f) {
		clear()
		setRegionInternal(value)
		renderable.setScaling(scaleX, scaleY)
	}

	/**
	 * Sets the atlas region.
	 * If there were any textures loading, they will be cancelled.
	 */
	fun region(texture: Texture, data: AtlasRegionData) = region(AtlasRegion(texture, data))

	/**
	 * Sets the region of the atlas component.
	 * The current region will be cleared immediately.
	 *
	 * @param atlasPath The atlas json file.
	 * @param regionName The name of the region within the atlas.
	 */
	fun region(atlasPath: String, regionName: String) {
		region(AtlasRegionLoaderImpl(this, mapOf(1f to atlasPath), regionName))
	}

	/**
	 * Sets the region of the atlas component.
	 * The current region will be cleared immediately.
	 *
	 * @param atlasPaths A map of pixel density to atlas json paths.
	 * @param regionName The name of the region within the atlas.
	 */
	fun region(atlasPaths: Map<Float, String>, regionName: String) {
		region(AtlasRegionLoaderImpl(this, atlasPaths, regionName))
	}

	/**
	 * Sets the region of the atlas component.
	 * The current region will be cleared immediately.
	 *
	 * @param loader The atlas region loader. This will be invoked whenever a new pixel density is required.
	 */
	fun region(loader: AtlasRegionLoader) {
		clear()
		cacheSet = cacheSet()
		this.loader = loader
		load()
	}

	private fun load() {
		val loader = loader ?: return
		val cacheSet = cacheSet!!

		val requestedDensity = maxOf(dpiStyle.scaleX, dpiStyle.scaleY)
		val actualDensity = loader.getBestDensity(requestedDensity)
		val scale = scaleIfPastAffordance(actualDensity, requestedDensity, dpiStyle.scalingSnapAffordance)
		renderable.setScaling(dpiStyle.scaleX * scale, dpiStyle.scaleY * scale)

		if (currentDensity == actualDensity) return
		currentDensity = actualDensity
		val loadId = ++currentLoadId
		launch {
			try {
				val region = loader.load(actualDensity, cacheSet)
				if (loadId == currentLoadId) {
					setRegionInternal(region)
				}
			} catch (e: RegionNotFoundException) {
				Log.warn(e.message)
				clear()
			}
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
		cacheSet = null
		currentDensity = -1f
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

inline fun Context.atlas(atlasPath: String, region: String, init: ComponentInit<AtlasComponent> = {}): AtlasComponent =
		atlas(mapOf(1f to atlasPath), region, init)

inline fun Context.atlas(atlasPaths: Map<Float, String>, region: String, init: ComponentInit<AtlasComponent> = {}): AtlasComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val a = AtlasComponent(this)
	a.region(atlasPaths, region)
	a.init()
	return a
}

/**
 * Creates an atlas component and uses it as the contents.
 * If the atlas component already exists, it will be reused.
 */
fun SingleElementContainer<UiComponent>.contentsAtlas(atlasPath: String, region: String) {
	return createOrReuseElement { atlas() }.region(atlasPath, region)
}

/**
 * Creates an atlas component and uses it as the contents.
 * If the atlas component already exists, it will be reused.
 */
fun SingleElementContainer<UiComponent>.contentsAtlas(atlasPaths: Map<Float, String>, region: String) {
	return createOrReuseElement { atlas() }.region(atlasPaths, region)
}

interface AtlasRegionLoader {

	/**
	 * The supported pixel densities available to load.
	 * E.g. If MDPI, XHDPI, and XXHDPI densities are available, listOf(1f, 2f, 3f) should be returned.
	 */
	val densities: List<Float>

	fun getBestDensity(requestedDensity: Float): Float {
		val nearestSupportedIndex = minOf(densities.sortedInsertionIndex(requestedDensity, matchForwards = false), densities.lastIndex)
		return densities[nearestSupportedIndex]
	}

	/**
	 * Loads the atlas region for the given density.
	 *
	 * @throws RegionNotFoundException
	 */
	suspend fun load(density: Float, cacheSet: CacheSet): AtlasRegion
}

/**
 * @param atlasPathMap A map of pixel density to atlas json paths.
 * @param regionName The name of the atlas region.
 */
class AtlasRegionLoaderImpl(
		owner: Context,
		private val atlasPathMap: Map<Float, String>,
		private val regionName: String
) : AtlasRegionLoader, ContextImpl(owner) {

	override val densities: List<Float> = atlasPathMap.keys.toList()

	override suspend fun load(density: Float, cacheSet: CacheSet): AtlasRegion =
			loadAndCacheAtlasRegion(atlasPathMap[density] ?: error("Density $density not found"), regionName, cacheSet)
}