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

import com.acornui.action.AbortedException
import com.acornui.action.BasicAction
import com.acornui.action.onFailed
import com.acornui.action.onSuccess
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.AssetTypes
import com.acornui.core.assets.AssetLoader
import com.acornui.core.graphics.Texture
import com.acornui.core.request.ResponseType
import com.acornui.js.io.JsHttpRequest

/**
 * An asset loader for textures (images).
 * @author nbilyk
 */
class DomTextureLoader : BasicAction(), AssetLoader<Texture> {

	override val type: AssetType<Texture> = AssetTypes.TEXTURE

	private var _asset: DomTexture? = null

	override var path: String = ""

	override val result: Texture
		get() = _asset!!

	private var fileLoader: JsHttpRequest? = null

	override var estimatedBytesTotal: Int = 0

	override val secondsLoaded: Float
		get() = fileLoader?.secondsLoaded ?: 0f

	override val secondsTotal: Float
		get() = fileLoader?.secondsTotal ?: 0f

	override fun onInvocation() {
		val fileLoader = JsHttpRequest()
		fileLoader.requestData.responseType = ResponseType.BINARY
		fileLoader.requestData.url = path
		fileLoader.onSuccess {
			val jsTexture = DomTexture()
			jsTexture.onLoad = {
				success()
			}
			jsTexture.arrayBuffer(fileLoader.resultArrayBuffer)
			_asset = jsTexture
		}
		fileLoader.onFailed {
			if (it is AbortedException) cancel(it)
			else fail(it)
		}
		fileLoader()
		this.fileLoader = fileLoader
	}

	override fun onAborted() {
		fileLoader?.abort()
	}

	override fun onReset() {
		_asset = null
	}
}