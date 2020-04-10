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


import com.acornui.di.Context
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.gl.core.TextureWrapMode
import com.acornui.graphic.Texture
import com.acornui.io.toUrlRequestData
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author nbilyk
 */
class RepeatingTexture(
		owner: Context
) : TextureComponent(owner) {

	var wrapS = TextureWrapMode.REPEAT
	var wrapT = TextureWrapMode.REPEAT
	var filterMin: TextureMinFilter = TextureMinFilter.LINEAR_MIPMAP_LINEAR
	var filterMag: TextureMagFilter = TextureMagFilter.LINEAR

	override fun setTextureInternal(value: Texture?) {
		if (value != null) {
			value.filterMin = filterMin
			value.filterMag = filterMag
			value.wrapS = wrapS
			value.wrapT = wrapT
		}
		super.setTextureInternal(value)
	}

	override fun updateVerticesGlobal() {
		val t = renderable.texture ?: return
		renderable.setUv(0f, 0f, width * renderable.scaleX / t.widthPixels.toFloat(), height * renderable.scaleY / t.heightPixels.toFloat(), false)
		super.updateVerticesGlobal()
	}
}

inline fun Context.repeatingTexture(path: String, init: ComponentInit<RepeatingTexture> = {}): RepeatingTexture  =
		repeatingTexture(TexturePaths(path), init)

inline fun Context.repeatingTexture(paths: Map<Float, String>, init: ComponentInit<RepeatingTexture> = {}): RepeatingTexture  =
		repeatingTexture(TexturePaths(paths.mapValues { it.value.toUrlRequestData() }), init)

inline fun Context.repeatingTexture(paths: TexturePaths, init: ComponentInit<RepeatingTexture> = {}): RepeatingTexture  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val g = RepeatingTexture(this)
	g.texture(paths)
	g.init()
	return g
}
