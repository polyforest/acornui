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

package com.acornui.js.io

import com.acornui.async.Promise
import com.acornui.core.Bandwidth
import com.acornui.core.di.Injector
import com.acornui.core.request.*
import com.acornui.io.ReadByteBuffer
import com.acornui.io.NativeReadByteBuffer
import com.acornui.logging.Log
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.files.Blob
import org.w3c.xhr.*

abstract class JsHttpRequest<T>(
		requestData: UrlRequestData,
		responseType: XMLHttpRequestResponseType
) : Promise<T>(), Request<T> {

	private var _bytesLoaded = 0
	override val secondsLoaded: Float
		get() = _bytesLoaded * Bandwidth.downBpsInv

	private var _bytesTotal = 0
	override val secondsTotal: Float
		get() {
			return _bytesTotal * Bandwidth.downBpsInv
		}

	private val httpRequest = XMLHttpRequest()

	init {
		httpRequest.onprogress = {
			event ->
			_bytesLoaded = event.loaded
			_bytesTotal = event.total
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
					success(result)
				} else {
					fail(ResponseException(httpRequest.status, url + " " + httpRequest.statusText, httpRequest.response?.toString() ?: ""))
				}
			}
		}

		httpRequest.open(requestData.method, url, async, requestData.user, requestData.password)
		httpRequest.responseType = responseType
		httpRequest.timeout = requestData.timeout.toInt()
		for ((key, value) in requestData.headers) {
			httpRequest.setRequestHeader(key, value)
		}
		if (requestData.method == UrlRequestMethod.GET) {
			httpRequest.send()
		} else {
			if (requestData.variables != null) {
				val data = requestData.variables!!.toQueryString()
				httpRequest.send(data)
			} else if (requestData.formData != null) {
				val formData = FormData()
				for (item in requestData.formData!!.items) {
					if (item is ByteArrayFormItem) {
						formData.append(item.name, Blob(arrayOf(item.value.native)))
					} else if (item is StringFormItem) {
						formData.append(item.name, item.value)
					} else {
						Log.warn("Unknown form item type $item")
					}
				}
				httpRequest.send(formData)
			} else if (requestData.body != null) {
				httpRequest.send(requestData.body!!)
			} else {
				httpRequest.send()
			}
		}
	}

	abstract fun process(httpRequest: XMLHttpRequest): T

	override fun cancel() = httpRequest.abort()

}

class JsArrayBufferRequest(requestData: UrlRequestData) : JsHttpRequest<ArrayBuffer>(requestData, XMLHttpRequestResponseType.ARRAYBUFFER) {
	override fun process(httpRequest: XMLHttpRequest): ArrayBuffer {
		return httpRequest.response as ArrayBuffer
	}
}

class JsBinaryRequest(requestData: UrlRequestData) : JsHttpRequest<NativeReadByteBuffer>(requestData, XMLHttpRequestResponseType.ARRAYBUFFER) {
	override fun process(httpRequest: XMLHttpRequest): NativeReadByteBuffer {
		return JsByteBuffer(Uint8Array(httpRequest.response!! as ArrayBuffer))
	}
}

class JsTextRequest(requestData: UrlRequestData) : JsHttpRequest<String>(requestData, XMLHttpRequestResponseType.TEXT) {
	override fun process(httpRequest: XMLHttpRequest): String {
		return httpRequest.response!! as String
	}
}


object JsRestServiceFactory : RestServiceFactory {
	override fun createTextRequest(injector: Injector, requestData: UrlRequestData): Request<String> {
		return JsTextRequest(requestData)
	}

	override fun createBinaryRequest(injector: Injector, requestData: UrlRequestData): Request<ReadByteBuffer> {
		return JsBinaryRequest(requestData)
	}
}
