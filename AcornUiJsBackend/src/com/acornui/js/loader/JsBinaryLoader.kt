/*
 * Copyright 2018 Nicholas Bilyk
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
import com.acornui.core.Bandwidth
import com.acornui.core.assets.AssetLoader
import com.acornui.core.assets.AssetType
import com.acornui.core.request.Request
import com.acornui.core.request.UrlRequestData
import com.acornui.io.ReadNativeByteBuffer
import com.acornui.js.io.JsBinaryRequest

/**
 * An asset loader for text.
 * @author nbilyk
 */
class JsBinaryLoader(
		override val path: String,
		override val estimatedBytesTotal: Int = 0,
		private val request: Request<ReadNativeByteBuffer> = JsBinaryRequest(UrlRequestData(path))
) : AssetLoader<ReadNativeByteBuffer> {

	override val type: AssetType<ReadNativeByteBuffer> = AssetType.BINARY

	override val secondsLoaded: Float
		get() = request.secondsLoaded

	override val secondsTotal: Float
		get() = if (request.secondsTotal <= 0f) estimatedBytesTotal * Bandwidth.downBpsInv else request.secondsTotal

	override val status: Deferred.Status
		get() = request.status
	override val result: ReadNativeByteBuffer
		get() = request.result
	override val error: Throwable
		get() = request.error

	override suspend fun await(): ReadNativeByteBuffer = request.await()

	override fun cancel() = request.cancel()
}