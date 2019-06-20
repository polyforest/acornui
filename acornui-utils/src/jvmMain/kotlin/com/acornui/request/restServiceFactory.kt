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

package com.acornui.request


import com.acornui.async.Deferred
import com.acornui.async.async
import com.acornui.async.asyncIo
import com.acornui.io.*
import com.acornui.logging.Log
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


abstract class JvmHttpRequest<out T>(requestData: UrlRequestData) : Request<T> {

	// TODO: Seconds loaded / total.
	override val secondsLoaded = 0f
	override val secondsTotal = 0f

	private var work: Deferred<T>

	init {
		work = asyncIo {
			// TODO: cookies
			val urlStr = if (requestData.method == UrlRequestMethod.GET && requestData.variables != null)
				requestData.url + "?" + requestData.variables!!.toQueryString() else requestData.url
			val url = URL(urlStr)
			val con = url.openConnection() as HttpURLConnection
			configure(con, requestData)

			var error: Throwable? = null
			var result: T? = null
			try {
				con.connect()
				val status = con.responseCode
				if (status == 200 || status == 304) {
					result = process(con.inputStream!!)
				} else {
					val errorMsg = con.errorStream?.readTextAndClose() ?: ""
					error = ResponseException(status.toShort(), "", errorMsg)
				}
			} catch (e: Throwable) {
				error = e
			} finally {
				con.disconnect()
			}
			if (error != null) throw error
			result!!
		}
	}

	private fun configure(con: HttpURLConnection, requestData: UrlRequestData) {
		if (requestData.user != null) {
			val userPass = "${requestData.user}:${requestData.password}"
			val basicAuth = "Basic " + Base64.getEncoder().encodeToString(userPass.toByteArray())
			con.setRequestProperty("Authorization", basicAuth)
		}
		con.requestMethod = requestData.method
		con.connectTimeout = requestData.timeout.toInt()
		for ((key, value) in requestData.headers) {
			con.setRequestProperty(key, value)
		}
		if (requestData.method != UrlRequestMethod.GET) {
			when {
				requestData.variables != null -> {
					con.doOutput = true
					con.outputStream.writeTextAndClose(requestData.variables!!.toQueryString())
				}
				requestData.formData != null -> {
					con.doOutput = true
					val out = DataOutputStream(con.outputStream)
					val items = requestData.formData.items
					for (i in 0..items.lastIndex) {
						val item = items[i]
						if (i != 0) out.writeBytes("&")
						out.writeBytes("$item.name=")
						if (item is ByteArrayFormItem) {
							out.write(item.value.toByteArray())
						} else if (item is StringFormItem) {
							out.writeBytes(item.value)
						} else {
							Log.warn("Unknown form item type $item")
						}
					}

					out.flush()
					out.close()
				}
				requestData.body != null -> {
					con.doOutput = true
					con.outputStream.writeTextAndClose(requestData.body!!)
				}
			}
		}
	}

	override val status: Deferred.Status
		get() = work.status
	override val result: T
		get() = work.result
	override val error: Throwable
		get() = work.error

	override suspend fun await(): T = work.await()

	override fun cancel() {}

	abstract fun process(inputStream: InputStream): T

}

actual fun createTextRequest(requestData: UrlRequestData): Request<String> {
	return object : JvmHttpRequest<String>(requestData) {
		override fun process(inputStream: InputStream): String {
			return inputStream.readTextAndClose()
		}
	}
}

actual fun createBinaryRequest(requestData: UrlRequestData): Request<ReadByteBuffer> {
	return object : JvmHttpRequest<ReadByteBuffer>(requestData) {
		override fun process(inputStream: InputStream): ReadByteBuffer {
			val byteArray = inputStream.use {
				it.readAllBytes2()
			}
			val buffer = ByteBuffer.wrap(byteArray)
			buffer.order(ByteOrder.LITTLE_ENDIAN)
			return JvmByteBuffer(buffer)
		}
	}
}