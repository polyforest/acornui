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

import com.acornui.async.Deferred
import com.acornui.async.then
import com.acornui.collection.Clearable
import com.acornui.collection.tuple
import com.acornui.component.*
import com.acornui.core.assets.CachedGroup
import com.acornui.core.assets.cachedGroup
import com.acornui.core.assets.loadAndCacheJson
import com.acornui.core.di.Owned
import com.acornui.math.Bounds

/**
 * A UiComponent that draws a region from a texture atlas.
 * This component will create a NinePatchComponent or a TextureComponent, and apply all virtual padding as defined by
 * the loaded AtlasRegionData.
 *
 * @author nbilyk
 */
open class AtlasComponent(owner: Owned) : VertexDrawableComponent(owner), Clearable {

	private var region: AtlasRegionData? = null
	private var texture: Texture? = null

	override val drawable: VertexDrawable?
		get() = _sprite ?: _ninePatch

	private var _sprite: Sprite? = null
	private var _ninePatch: NinePatch? = null

	private var group: CachedGroup? = null

	override fun clear() {
		group?.dispose()
		group = null
		clearRegionAndTexture()
	}

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

	val ninePatch: NinePatch?
		get() = _ninePatch

	val sprite: Sprite?
		get() = _sprite

	private var _blendMode = BlendMode.NORMAL
	var blendMode: BlendMode
		get() = _blendMode
		set(value) {
			_blendMode = value
			_sprite?.blendMode = value
			_ninePatch?.blendMode = value
		}

	private var _useAsBackFace: Boolean = false

	/**
	 * If true, the normal and indices will be reversed.
	 */
	var useAsBackFace: Boolean
		get() = _useAsBackFace
		set(value) {
			_useAsBackFace = value
			_sprite?.useAsBackFace = value
			_ninePatch?.useAsBackFace = value
		}

	private fun clearRegionAndTexture() {
		if (isActive) texture?.refDec()
		texture = null
		region = null
		_ninePatch = null
		_sprite = null
		invalidateLayout()
	}

	private fun setRegionAndTexture(texture: Texture, region: AtlasRegionData) {
		if (region.splits == null) {
			_ninePatch = null
			if (_sprite == null) {
				_sprite = Sprite()
				_sprite?.blendMode = _blendMode
				_sprite?.useAsBackFace = _useAsBackFace
			}
			val t = _sprite!!
			t.setRegion(region.bounds, region.isRotated)
		} else {
			_sprite = null
			if (_ninePatch == null) {
				_ninePatch = NinePatch()
				_ninePatch?.blendMode = _blendMode
				_ninePatch?.useAsBackFace = _useAsBackFace
			}
			val t = _ninePatch!!
			val splits = region.splits
			t.split(
					maxOf(0f, splits[0] - region.padding[0]),
					maxOf(0f, splits[1] - region.padding[1]),
					maxOf(0f, splits[2] - region.padding[2]),
					maxOf(0f, splits[3] - region.padding[3])
			)
			t.setRegion(region.bounds, region.isRotated)
		}
		_sprite?.texture = texture
		_ninePatch?.texture = texture
		this.region = region
		val oldTexture = this.texture
		this.texture = texture
		if (isActive) {
			oldTexture?.refDec()
			texture.refInc()
		}
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

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		drawable?.updateUv()
		val region = region ?: return
		val regionWidth = if (region.isRotated) region.bounds.height else region.bounds.width
		val regionHeight = if (region.isRotated) region.bounds.width else region.bounds.height
		val naturalWidth = region.padding[0] + regionWidth + region.padding[2]
		val naturalHeight = region.padding[1] + regionHeight + region.padding[3]
		out.set(explicitWidth ?: naturalWidth.toFloat(), explicitHeight ?: naturalHeight.toFloat())
	}

	override fun updateVertices() {
		val explicitWidth = explicitWidth
		val explicitHeight = explicitHeight

		val drawable = drawable ?: return
		val region = region ?: return

		val paddingLeft = region.padding[0].toFloat()
		val paddingTop = region.padding[1].toFloat()
		val paddingRight = region.padding[2].toFloat()
		val paddingBottom = region.padding[3].toFloat()


		// Account for scaling with split regions if there are any.
		val splits = region.splits ?: EMPTY_SPLITS
		val unscaledPadLeft = minOf(paddingLeft, splits[0])
		val unscaledPadTop = minOf(paddingTop, splits[1])
		val unscaledPadRight = minOf(paddingRight, splits[2])
		val unscaledPadBottom = minOf(paddingBottom, splits[3])


		val scaledPadLeft = paddingLeft - unscaledPadLeft
		val scaledPadTop = paddingTop - unscaledPadTop
		val scaledPadRight = paddingRight - unscaledPadRight
		val scaledPadBottom = paddingBottom - unscaledPadBottom

		val uH = splits[0] + splits[2]
		val uV = splits[1] + splits[3]
		val sX: Float = if (explicitWidth == null) 1f else (explicitWidth - uH) / (drawable.naturalWidth - uH)
		val sY: Float = if (explicitHeight == null) 1f else (explicitHeight - uV) / (drawable.naturalHeight - uV)

		val totalPadLeft = unscaledPadLeft + scaledPadLeft * sX
		val totalPadTop = unscaledPadTop + scaledPadTop * sY
		val totalPadRight = unscaledPadRight + scaledPadRight * sX
		val totalPadBottom = unscaledPadBottom + scaledPadBottom * sY

		drawable.updateWorldVertices(concatenatedTransform, if (explicitWidth == null) drawable.naturalWidth else explicitWidth - totalPadLeft - totalPadRight,
				if (explicitHeight == null) drawable.naturalHeight else explicitHeight - totalPadBottom - totalPadTop, x = totalPadLeft, y = totalPadTop)

	}

	override fun dispose() {
		clear()
		super.dispose()
	}

	companion object {
		private val EMPTY_SPLITS = listOf(0f, 0f, 0f, 0f)
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