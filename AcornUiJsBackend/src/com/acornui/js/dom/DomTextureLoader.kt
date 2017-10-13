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

package com.acornui.js.dom

import com.acornui.async.Promise
import com.acornui.async.launch
import com.acornui.core.assets.AssetLoader
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.AssetTypes
import com.acornui.core.graphics.Texture
import com.acornui.core.request.UrlRequestData
import com.acornui.js.io.JsArrayBufferRequest

/**
 * An asset loader for textures (images).
 * @author nbilyk
 */
class DomTextureLoader(override val path: String, override val estimatedBytesTotal: Int) : AssetLoader<Texture> {

	override val type: AssetType<Texture> = AssetTypes.TEXTURE

	private val fileLoader = JsArrayBufferRequest(UrlRequestData(path))

	override val secondsLoaded: Float
		get() = fileLoader.secondsLoaded

	override val secondsTotal: Float
		get() = fileLoader.secondsTotal

	private val work = object : Promise<Texture>() {
		init {
			launch {
				val arrayBuffer = fileLoader.await()

				val jsTexture = DomTexture()
				jsTexture.image.onload = {
					success(jsTexture)
				}
				jsTexture.image.onerror = {
					msg, url, lineNo, columnNo, error ->
					fail(Exception(msg))
				}
				jsTexture.arrayBuffer(arrayBuffer)
			}
		}
	}

	suspend override fun await(): Texture = work.await()

	override fun cancel() {}
}