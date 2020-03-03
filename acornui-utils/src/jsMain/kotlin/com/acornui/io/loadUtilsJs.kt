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

package com.acornui.io

import clearTimeout
import com.acornui.async.Promise
import com.acornui.logging.Log
import com.acornui.system.userInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import kotlinx.coroutines.coroutineScope
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.files.Blob
import org.w3c.xhr.*
import setTimeout
import kotlin.time.seconds

suspend fun <T> load(
		requestData: UrlRequestData,
		responseType: XMLHttpRequestResponseType,
		settings: RequestSettings,
		process: (xhr: XMLHttpRequest) -> T
): T = coroutineScope {
	if (userInfo.isNodeJs && jsTypeOf(XMLHttpRequest) == "undefined") {
		js("""global.XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;""")
	}
	val url = requestData.toUrlStr(settings.rootPath)

	Log.verbose("Load request: url: $url connectTimeout: ${settings.connectTimeout} responseType: $responseType")
	val progress = settings.progressReporter.addChild(ProgressImpl(total = settings.initialTimeEstimate))
	val xhr = XMLHttpRequest()

	val promise = Promise<T> { resolve, reject ->
		var timeoutId = setTimeout({
			Log.verbose("Load connect timeout: $url")
			reject(ResponseConnectTimeoutException(requestData, settings.connectTimeout))
			xhr.abort()
		}, settings.connectTimeout.inMilliseconds.toInt())

		xhr.addEventListener("progress", { event ->
			event as ProgressEvent
			progress.loaded = Bandwidth.downBpsInv.seconds * event.loaded.toDouble()
			progress.total = Bandwidth.downBpsInv.seconds * event.total.toDouble()

			if (timeoutId >= 0 && event.loaded.toDouble() > 0) {
				clearTimeout(timeoutId)
				timeoutId = -1
			}
			Unit
		})

		xhr.addEventListener("error", {
			Log.verbose("Load error: $url ${xhr.readyState} ${xhr.status}")
			reject(ResponseException(
					xhr.status,
					"$url ${xhr.status} ${xhr.statusText}",
					xhr.response?.toString() ?: ""
			))
		})

		xhr.addEventListener("load", {
			clearTimeout(timeoutId)
			Log.verbose("Load complete: $url ${xhr.readyState} ${xhr.status}")
			if (xhr.status == 200.toShort() || xhr.status == 304.toShort()) {
				var result: T? = null
				val success = try {
					result = process(xhr)
					true
				} catch (e: Throwable) {
					reject(e)
					false
				}
				@Suppress("UNCHECKED_CAST")
				if (success)
					resolve(result as T)
			} else {
				reject(ResponseException(
						xhr.status,
						"$url ${xhr.status} ${xhr.statusText}",
						xhr.responseText
				))
			}
		})

		val async = true
		xhr.open(requestData.method, url, async, requestData.user, requestData.password)
		xhr.responseType = responseType

		for ((key, value) in requestData.headers) {
			xhr.setRequestHeader(key, value)
		}
		if (requestData.method == UrlRequestMethod.GET) {
			xhr.send()
		} else {
			when {
				requestData.variables != null -> {
					val data = requestData.variables.queryString
					xhr.send(data)
				}
				requestData.formData != null -> {
					val formData = FormData()
					for (item in requestData.formData.items) {
						when (item) {
							is ByteArrayFormItem -> formData.append(item.name, Blob(arrayOf(item.value.native)))
							is StringFormItem -> formData.append(item.name, item.value)
						}
					}
					xhr.send(formData)
				}
				requestData.body != null -> xhr.send(requestData.body!!)
				else -> xhr.send()
			}
		}
	}
	try {
		promise.await().also {
			settings.progressReporter.removeChild(progress)
		}
	} catch (e: CancellationException) {
		// If the parent coroutine is cancelled.
		Log.verbose("Load cancelled: $url")
		xhr.abort()
		throw e
	}
}

suspend fun loadText(requestData: UrlRequestData, settings: RequestSettings): String {
	return load(requestData, XMLHttpRequestResponseType.TEXT, settings) { httpRequest ->
		httpRequest.responseText
	}
}

actual class TextLoader actual constructor(defaultSettings: RequestSettings) : Loader<String> {

	override val requestSettings: RequestSettings =
			defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 1_000)

	override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): String {
		return loadText(requestData, settings)
	}
}

suspend fun loadBinary(requestData: UrlRequestData, settings: RequestSettings): ReadByteBuffer {
	return load(requestData, XMLHttpRequestResponseType.ARRAYBUFFER, settings) { httpRequest ->
		JsByteBuffer(Uint8Array(httpRequest.response!! as ArrayBuffer))
	}
}

actual class BinaryLoader actual constructor(defaultSettings: RequestSettings) : Loader<ReadByteBuffer> {

	override val requestSettings: RequestSettings =
			defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 10_000)

	override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): ReadByteBuffer {
		return loadBinary(requestData, settings)
	}
}

suspend fun loadArrayBuffer(requestData: UrlRequestData, settings: RequestSettings): ArrayBuffer {
	return load(requestData, XMLHttpRequestResponseType.ARRAYBUFFER, settings) { httpRequest ->
		httpRequest.response as ArrayBuffer
	}
}