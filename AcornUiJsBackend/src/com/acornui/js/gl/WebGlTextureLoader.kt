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

package com.acornui.js.gl

import com.acornui.async.Deferred
import com.acornui.async.Promise
import com.acornui.core.Bandwidth
import com.acornui.core.assets.AssetLoader
import com.acornui.core.assets.AssetType
import com.acornui.core.graphics.Texture
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import org.w3c.dom.url.URL
import kotlin.browser.window

/**
 * An asset loader for textures (images).
 * @author nbilyk
 */
class WebGlTextureLoader(
		override val path: String,
		override val estimatedBytesTotal: Int,
		private val gl: Gl20,
		private val glState: GlState
) : AssetLoader<Texture> {

	override val type = AssetType.TEXTURE

	override val secondsLoaded: Float
		get() = 0f
	override val secondsTotal: Float
		get() = estimatedBytesTotal * Bandwidth.downBpsInv

	private val work: Deferred<Texture> = object : Promise<Texture>() {
		init {
			val jsTexture = WebGlTexture(gl, glState)
			if (path.startsWith("http", ignoreCase = true) && URL(path).origin !== window.location.origin) {
				println("Setting cross origin for $path")
				jsTexture.image.crossOrigin = ""
			}
			jsTexture.image.src = path

			jsTexture.image.onload = {
				success(jsTexture)
			}
			jsTexture.image.onerror = {
				msg, url, lineNo, columnNo, error ->
				fail(Exception(msg?.toString() ?: "Unknown Error"))
			}
		}
	}

	override val status: Deferred.Status
		get() = work.status
	override val result: Texture
		get() = work.result
	override val error: Throwable
		get() = work.error

	override suspend fun await(): Texture = work.await()

	override fun cancel() {
	}

}