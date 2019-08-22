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


import com.acornui.logging.Log
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.files.Blob
import org.w3c.xhr.*

suspend fun <T> load(
		requestData: UrlRequestData,
		responseType: XMLHttpRequestResponseType,
		progressReporter: ProgressReporter,
		initialTimeEstimate: Float,
		process: (httpRequest: XMLHttpRequest) -> T

): T {
	// TODO: progressReporter
	val httpRequest = XMLHttpRequest()
	val c = CompletableDeferred<T>()
	httpRequest.onprogress = { event ->
		//			_bytesLoaded = event.loaded
//			_bytesTotal = event.total
		Unit
	}

	val async = true
	val url = if (requestData.method == UrlRequestMethod.GET && requestData.variables != null)
		requestData.url + "?" + requestData.variables!!.toQueryString() else requestData.url

	httpRequest.onreadystatechange = {
		if (httpRequest.readyState == XMLHttpRequest.DONE) {
			httpRequest.onreadystatechange = null
			if (httpRequest.status == 200.toShort() || httpRequest.status == 304.toShort()) {
				val result = process(httpRequest)
				c.complete(result)
			} else {
				c.completeExceptionally(ResponseException(httpRequest.status, url + " " + httpRequest.statusText, httpRequest.response?.toString()
						?: ""))
			}
		}
	}

	httpRequest.open(requestData.method, url, async, requestData.user, requestData.password)
	httpRequest.responseType = responseType
	httpRequest.timeout = (requestData.timeout * 1000f).toInt()
	for ((key, value) in requestData.headers) {
		httpRequest.setRequestHeader(key, value)
	}
	if (requestData.method == UrlRequestMethod.GET) {
		httpRequest.send()
	} else {
		when {
			requestData.variables != null -> {
				val data = requestData.variables!!.toQueryString()
				httpRequest.send(data)
			}
			requestData.formData != null -> {
				val formData = FormData()
				for (item in requestData.formData.items) {
					when (item) {
						is ByteArrayFormItem -> formData.append(item.name, Blob(arrayOf(item.value.native)))
						is StringFormItem -> formData.append(item.name, item.value)
						else -> Log.warn("Unknown form item type $item")
					}
				}
				httpRequest.send(formData)
			}
			requestData.body != null -> httpRequest.send(requestData.body!!)
			else -> httpRequest.send()
		}
	}
	return c.await()
}

suspend fun loadText(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter,
		initialTimeEstimate: Float
): String {
	return load(requestData, XMLHttpRequestResponseType.TEXT, progressReporter, initialTimeEstimate) { httpRequest ->
		httpRequest.response!! as String
	}
}

actual class TextLoader : Loader<String> {
	override val defaultInitialTimeEstimate: Float
		get() = Bandwidth.downBpsInv * 1_000

	override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Float): String {
		return loadText(requestData, progressReporter, initialTimeEstimate)
	}
}

suspend fun loadBinary(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter,
		initialTimeEstimate: Float
): ReadByteBuffer {
	return load(requestData, XMLHttpRequestResponseType.ARRAYBUFFER, progressReporter, initialTimeEstimate) { httpRequest ->
		JsByteBuffer(Uint8Array(httpRequest.response!! as ArrayBuffer))
	}
}

actual class BinaryLoader : Loader<ReadByteBuffer> {
	override val defaultInitialTimeEstimate: Float
		get() = Bandwidth.downBpsInv * 10_000

	override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Float): ReadByteBuffer {
		return loadBinary(requestData, progressReporter, initialTimeEstimate)
	}
}

suspend fun loadArrayBuffer(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter,
		initialTimeEstimate: Float = Bandwidth.downBpsInv * 100_000
): ArrayBuffer {
	return load(requestData, XMLHttpRequestResponseType.ARRAYBUFFER, progressReporter, initialTimeEstimate) { httpRequest ->
		httpRequest.response as ArrayBuffer
	}
}