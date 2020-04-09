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
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.time.milliseconds
import kotlin.time.seconds

private object Resource {
	val classLoader: ClassLoader? = javaClass.classLoader
}

/**
 * Parses a String into a [URL].
 * If this request represents an absolute path, a [URL] object is returned from its constructor. Otherwise checks the
 * class loader's resources.
 * @param rootPath If this request represents a relative path, this rootPath will be prepended.

 * @return Returns a URL object for reading the resource or null if this string is neither a valid url or a found
 * resource.
 * @see UrlRequestData.isAbsolutePath
 */
fun UrlRequestData.toJavaUrl(rootPath: String): URL? {
	val urlStr = toUrlStr(rootPath)
	return if (isAbsolutePath) {
		try {
			URL(urlStr)
		} catch (e: MalformedURLException) {
			return null
		}
	} else {
		Resource.classLoader?.getResource(urlStr)
	}
}

suspend fun <T> load(
		requestData: UrlRequestData,
		settings: RequestSettings,
		process: suspend (inputStream: InputStream) -> T
): T = withContext(Dispatchers.IO) {
	// TODO: cookies and progress
	val url = requestData.toJavaUrl(settings.rootPath)
	Log.verbose("Load: $url")

	val inputStream: InputStream = if (url != null) {
		// Is a url or a classpath resource.
		val con = url.openConnection()
		con.connectTimeout = settings.connectTimeout.inMilliseconds.toInt()
		if (con is HttpURLConnection) {
			configure(con, requestData)
			con.connectInternal(requestData)
			val status = con.responseCode
			if (status != 200 && status != 304) {
				val errorMsg = con.errorStream?.readTextAndClose() ?: ""
				throw ResponseException(status.toShort(), "$status", errorMsg)
			}
			con.inputStream!!
		} else {
			con.connectInternal(requestData)
		}
		con.inputStream!!
	} else {
		// Is a file
		val urlStr = requestData.toUrlStr(settings.rootPath)
		val file = File(urlStr)
//		_bytesTotal = file.length().toInt()
		if (!file.exists())
			throw ResponseException(404, "File '$urlStr' not found", "")
		FileInputStream(file)
	}
	process(inputStream)
	//.also { _bytesLoaded = bytesTotal }
}

private suspend fun URLConnection.connectInternal(requestData: UrlRequestData) = withContext(Dispatchers.IO) {
	try {
		connect()
	} catch (e: SocketTimeoutException) {
		// Rethrow the error as a ResponseConnectTimeoutException for platform consistency.
		throw ResponseConnectTimeoutException(requestData, connectTimeout.milliseconds)
	}
}

private fun configure(con: HttpURLConnection, requestData: UrlRequestData) {
	if (requestData.user != null) {
		val userPass = "${requestData.user}:${requestData.password}"
		val basicAuth = "Basic " + Base64.getEncoder().encodeToString(userPass.toByteArray())
		con.setRequestProperty("Authorization", basicAuth)
	}
	con.requestMethod = requestData.method
	for ((key, value) in requestData.headers) {
		con.setRequestProperty(key, value)
	}
	if (requestData.method != UrlRequestMethod.GET) {
		when {
			requestData.variables != null -> {
				con.doOutput = true
				con.outputStream.writeTextAndClose(requestData.variables.queryString)
			}
			requestData.formData != null -> {
				con.doOutput = true
				val out = DataOutputStream(con.outputStream)
				for ((i, item) in requestData.formData.items.withIndex()) {
					if (i != 0) out.writeBytes("&")
					out.writeBytes("$item.name=")
					when (item) {
						is ByteArrayFormItem -> out.write(item.value.toByteArray())
						is StringFormItem -> out.writeBytes(item.value)
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

suspend fun loadText(requestData: UrlRequestData, settings: RequestSettings) =
		load(requestData, settings) { inputStream ->
	inputStream.readTextAndClose()
}

actual class TextLoader actual constructor(defaultSettings: RequestSettings) : Loader<String> {

	override val requestSettings: RequestSettings =
			defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 1_000)

	override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): String {
		return loadText(requestData, settings)
	}
}

suspend fun loadBinary(requestData: UrlRequestData, settings: RequestSettings): ReadByteBuffer =
		load(requestData, settings) { inputStream ->
	val byteArray = inputStream.use {
		it.readAllBytes2()
	}
	val buffer = ByteBuffer.wrap(byteArray)
	buffer.order(ByteOrder.LITTLE_ENDIAN)
	JvmByteBuffer(buffer)
}

actual class BinaryLoader actual constructor(defaultSettings: RequestSettings) : Loader<ReadByteBuffer> {

	override val requestSettings: RequestSettings =
			defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 10_000)

	override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): ReadByteBuffer {
		return loadBinary(requestData, settings)
	}
}