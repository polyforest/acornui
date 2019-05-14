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


import com.acornui.core.di.Owned
import com.acornui.core.graphic.Texture
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.gl.core.TextureWrapMode
import com.acornui.math.Bounds

/**
 * @author nbilyk
 */
class RepeatingTexture(
		owner: Owned
) : TextureComponent(owner) {

	var wrapS = TextureWrapMode.REPEAT
	var wrapT = TextureWrapMode.REPEAT
	var filterMin: TextureMinFilter = TextureMinFilter.LINEAR_MIPMAP_LINEAR
	var filterMag: TextureMagFilter = TextureMagFilter.LINEAR

	override fun _setTexture(value: Texture?) {
		if (value != null) {
			value.filterMin = filterMin
			value.filterMag = filterMag
			value.wrapS = wrapS
			value.wrapT = wrapT
		}
		super._setTexture(value)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val t = texture ?: return
		val tW = t.width.toFloat()
		val tH = t.height.toFloat()
		val w = explicitWidth ?: tW
		val h = explicitHeight ?: tH
		setUv(0f, 0f, w / tW, h / tH)
		super.updateLayout(explicitWidth, explicitHeight, out)
	}
}

fun Owned.repeatingTexture(path: String, init: ComponentInit<RepeatingTexture> = {}): RepeatingTexture {
	val g = RepeatingTexture(this)
	g.init()
	g.path = path
	return g
}
