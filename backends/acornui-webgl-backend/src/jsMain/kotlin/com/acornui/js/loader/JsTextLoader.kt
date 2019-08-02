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

package com.acornui.js.loader

/**
 * @author nbilyk
 */
import com.acornui.async.Deferred
import com.acornui.asset.AssetLoader
import com.acornui.asset.AssetType
import com.acornui.request.Bandwidth
import com.acornui.request.JsTextRequest
import com.acornui.request.Request
import com.acornui.request.UrlRequestData

/**
 * An asset loader for text.
 * @author nbilyk
 */
class JsTextLoader(
		override val path: String,
		private val estimatedBytesTotal: Int = 0,
		private val request: Request<String> = JsTextRequest(UrlRequestData(path))
) : AssetLoader<String> {

	override val type: AssetType<String> = AssetType.TEXT

	override val secondsLoaded: Float
		get() = request.secondsLoaded

	override val secondsTotal: Float
		get() = if (request.secondsTotal <= 0f) estimatedBytesTotal * Bandwidth.downBpsInv else request.secondsTotal

	override val status: Deferred.Status
		get() = request.status
	override val result: String
		get() = request.result
	override val error: Throwable
		get() = request.error

	override suspend fun await(): String = request.await()

	override fun cancel() = request.cancel()
}
