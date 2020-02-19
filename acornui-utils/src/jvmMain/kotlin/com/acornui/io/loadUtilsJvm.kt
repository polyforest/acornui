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
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.coroutines.*
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

var validSchemes = listOf("http", "https", "ftp", "ftps")

private object Resource {
	val classLoader: ClassLoader? = javaClass.classLoader
}

/**
 * Parses a String into a [URL].
 * If this String starts with a valid url scheme defined in [validSchemes], a [URL] object is returned from its
 * constructor. Otherwise checks the class loader's resources.

 * @return Returns a URL object for reading the resource or null if this string is neither a valid url or a found
 * resource.
 */
fun String.toUrl(): URL? {
	val isValidUrlScheme = validSchemes.any { startsWith(it, ignoreCase = true) }
	return if (isValidUrlScheme) {
		try {
			URL(this)
		} catch (e: MalformedURLException) {
			return null
		}
	} else {
		Resource.classLoader?.getResource(this)
	}
}

suspend fun <T> load(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration,
		connectTimeout: Duration,
		process: suspend (inputStream: InputStream) -> T
): T = withContext(Dispatchers.IO) {
	// TODO: cookies and progress
	val urlStr = requestData.urlStr
	val url = urlStr.toUrl()
	Log.verbose("Load: $url")

	val inputStream: InputStream = if (url != null) {
		// Is a url or a classpath resource.
		val con = url.openConnection()
		con.connectTimeout = connectTimeout.inMilliseconds.toInt()
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
		val file = File(urlStr)
//		_bytesTotal = file.length().toInt()
		if (!file.exists())
			throw FileNotFoundException(urlStr)
		FileInputStream(file)
	}
	process(inputStream)
	//.also { _bytesLoaded = bytesTotal }
}

@UseExperimental(InternalCoroutinesApi::class)
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

suspend fun loadText(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter = GlobalProgressReporter,
		initialTimeEstimate: Duration,
		connectTimeout: Duration
) = load(requestData, progressReporter, initialTimeEstimate, connectTimeout) { inputStream ->
	inputStream.readTextAndClose()
}

actual class TextLoader : Loader<String> {
	override val defaultInitialTimeEstimate: Duration
		get() = Bandwidth.downBpsInv.seconds * 1_000

	override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration, connectTimeout: Duration): String {
		return loadText(requestData, progressReporter, initialTimeEstimate, connectTimeout)
	}
}

suspend fun loadBinary(
		requestData: UrlRequestData,
		progressReporter: ProgressReporter,
		initialTimeEstimate: Duration,
		connectTimeout: Duration
): ReadByteBuffer = load(requestData, progressReporter, initialTimeEstimate, connectTimeout) { inputStream ->
	val byteArray = inputStream.use {
		it.readAllBytes2()
	}
	val buffer = ByteBuffer.wrap(byteArray)
	buffer.order(ByteOrder.LITTLE_ENDIAN)
	JvmByteBuffer(buffer)
}

actual class BinaryLoader : Loader<ReadByteBuffer> {
	override val defaultInitialTimeEstimate: Duration
		get() = Bandwidth.downBpsInv.seconds * 10_000

	override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration, connectTimeout: Duration): ReadByteBuffer {
		return loadBinary(requestData, progressReporter, initialTimeEstimate, connectTimeout)
	}
}