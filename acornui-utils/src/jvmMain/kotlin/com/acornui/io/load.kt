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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.JarURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

var validSchemes = listOf("http", "https", "ftp", "ftps")

private object Resource {
	val classLoader: ClassLoader? = javaClass.classLoader
}

fun String.toUrl(): URL? {
	val resourceUrl = Resource.classLoader?.getResource(this)
	if (resourceUrl != null) return resourceUrl
	if (!validSchemes.any { startsWith(it, ignoreCase = true) }) return null
	return try {
		URL(this)
	} catch (e: MalformedURLException) {
		return null
	}
}


suspend fun <T> load(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Float,
		process: suspend (inputStream: InputStream) -> T
): T = withContext(Dispatchers.IO) {
	// TODO: cookies, cancellation and progress

	val urlStr = requestData.toUrlStr()
	val url = urlStr.toUrl()

	if (url != null) {
		// Is a url or a classpath resource.
		val con = url.openConnection()
		var result: T? = null
		if (con is HttpURLConnection) {
			configure(con, requestData)
			try {
				con.connect()
				val status = con.responseCode
				if (status == 200 || status == 304) {
					result = process(con.inputStream!!)
				} else {
					val errorMsg = con.errorStream?.readTextAndClose() ?: ""
					throw ResponseException(status.toShort(), "", errorMsg)
				}
			} finally {
				con.disconnect()
			}
		} else if (con is JarURLConnection) {
			con.connect()
			result = process(con.inputStream!!)
		}
		result!!
	} else {
		// Is a file
		val file = File(urlStr)
//		_bytesTotal = file.length().toInt()
		if (!file.exists())
			throw FileNotFoundException(urlStr)
		process(FileInputStream(file)) //.also { _bytesLoaded = bytesTotal }
	}

}

private fun configure(con: HttpURLConnection, requestData: UrlRequestData) {
	if (requestData.user != null) {
		val userPass = "${requestData.user}:${requestData.password}"
		val basicAuth = "Basic " + Base64.getEncoder().encodeToString(userPass.toByteArray())
		con.setRequestProperty("Authorization", basicAuth)
	}
	con.requestMethod = requestData.method
	con.connectTimeout = (requestData.timeout * 1000f).toInt()
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
					when (item) {
						is ByteArrayFormItem -> out.write(item.value.toByteArray())
						is StringFormItem -> out.writeBytes(item.value)
						else -> Log.warn("Unknown form item type $item")
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

suspend fun loadText(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Float = Bandwidth.downBpsInv * 1_000
) = load(requestData, progressReporter, initialTimeEstimate) { inputStream ->
	inputStream.readTextAndClose()
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
): ReadByteBuffer = load(requestData, progressReporter, initialTimeEstimate) { inputStream ->
	val byteArray = inputStream.use {
		it.readAllBytes2()
	}
	val buffer = ByteBuffer.wrap(byteArray)
	buffer.order(ByteOrder.LITTLE_ENDIAN)
	JvmByteBuffer(buffer)
}

actual class BinaryLoader : Loader<ReadByteBuffer> {
	override val defaultInitialTimeEstimate: Float
		get() = Bandwidth.downBpsInv * 10_000

	override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Float): ReadByteBuffer {
		return loadBinary(requestData, progressReporter, initialTimeEstimate)
	}
}