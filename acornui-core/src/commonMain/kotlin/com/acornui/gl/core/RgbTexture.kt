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

package com.acornui.gl.core

import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.RgbData
import com.acornui.core.io.byteBuffer

class RgbTexture(
		gl: Gl20,
		glState: GlState,
		private val _rgbData: RgbData
) : GlTextureBase(gl, glState) {

	override val width: Int
		get() = _rgbData.width

	override val height: Int
		get() = _rgbData.height

	override val rgbData: RgbData
		get() = _rgbData

	override fun uploadTexture() {
		val buffer = byteBuffer(_rgbData.bytes.size)
		for (i in 0.._rgbData.bytes.lastIndex) {
			buffer.put(_rgbData.bytes[i])
		}
		buffer.flip()
		gl.texImage2Db(target.value, 0, pixelFormat.value, _rgbData.width, _rgbData.height, 0, pixelFormat.value, pixelType.value, buffer)
	}
}


fun rgbTexture(gl: Gl20, glState: GlState, rgbData: RgbData, init: RgbTexture.() -> Unit = {}): RgbTexture {
	val r = RgbTexture(gl, glState, rgbData)
	r.init()
	return r
}

fun Scoped.rgbTexture(rgbData: RgbData, init: RgbTexture.() -> Unit = {}): RgbTexture {
	val r = RgbTexture(inject(Gl20), inject(GlState), rgbData)
	r.init()
	return r
}