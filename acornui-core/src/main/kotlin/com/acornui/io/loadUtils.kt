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

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.acornui.io

import com.acornui.browser.UrlParams
import com.acornui.browser.toUrlParams
import kotlinx.serialization.Serializable
import org.w3c.files.Blob
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * A model with the necessary information to make a request.
 */
data class UrlRequestData(

		val url: String = "",

		val method: String = UrlRequestMethod.GET,

		val headers: Map<String, String> = HashMap(),

		val user: String? = null,

		val password: String? = null,

		val formData: MultipartFormData? = null,

		val variables: UrlParams? = null,

		var body: String? = null
)

private val absolutePathRegex = Regex("""^([a-z0-9]*:|.{0})//.*${'$'}""", RegexOption.IGNORE_CASE)

/**
 * Returns true if the requested path starts with a scheme. (e.g. https://, http://, ftp://)
 * Note - This is case insensitive and does not validate if it's a known scheme.
 */
val UrlRequestData.isAbsolutePath: Boolean
	get() = url.matches(absolutePathRegex)

/**
 * @return Returns `!isAbsolutePath`
 */
val UrlRequestData.isRelativePath: Boolean
	get() = !isAbsolutePath

fun UrlRequestData.toUrlStr(rootPath: String): String {
	val prependedUrl = if (isRelativePath) rootPath + url else url
	return if (method == UrlRequestMethod.GET && variables != null)
		prependedUrl + "?" + variables.queryString else prependedUrl
}

/**
 * Parses a string into [UrlRequestData]
 */
fun String.toUrlRequestData(): UrlRequestData {
	val qIndex = indexOf("?")
	if (qIndex == -1) return UrlRequestData(this)
	val urlStr = substring(0, qIndex)
	val queryStr = substring(qIndex + 1)
	return UrlRequestData(urlStr, variables = queryStr.toUrlParams())
}

/**
 * The possible values for [UrlRequestData.method].
 */
object UrlRequestMethod {
	const val GET: String = "GET"
	const val POST: String = "POST"
	const val PUT: String = "PUT"
	const val DELETE: String = "DELETE"
}

open class ResponseConnectTimeoutException(
		val requestData: UrlRequestData,
		val connectTimeout: Duration
) : Throwable("The request ${requestData.url} timed out after $connectTimeout")

open class ResponseException(val status: Short, message: String?, val detail: String) : Throwable(message) {

	override fun toString(): String {
		return "ResponseException(status=$status, message=$message, detail=$detail)"
	}
}

data class MultipartFormData(val items: List<FormDataItem>)

/**
 * A marker interface for items that can be in the list of [MultipartFormData.items]
 */
sealed class FormDataItem {
	abstract val name: String
}

class ByteArrayFormItem(
	override val name: String,
	val value: Blob,
	val filename: String?
) : FormDataItem()

class StringFormItem(
		override val name: String,
		val value: String
) : FormDataItem()

interface Loader<out T> {

	/**
	 * Default request settings.
	 */
	val requestSettings: RequestSettings

	/**
	 * Begins loading the given request.
	 *
	 * @param settings Configuration for the request. To change settings, use [RequestSettings.copy] on the
	 * [requestSettings] object.
	 */
	suspend fun load(requestData: UrlRequestData, settings: RequestSettings = requestSettings): T
}

suspend fun <T> Loader<T>.load(path: String, settings: RequestSettings = requestSettings): T = load(path.toUrlRequestData(), settings)

data class RequestSettings(

		/**
		 * If the request is to a relative path, this value will be prepended.
		 */
		val rootPath: String = "",

		/**
		 * The progress reporter allows the loader to report its progress.
		 */
		val progressReporter: ProgressReporter = ProgressReporterImpl(),

		/**
		 * Before a connection has been made, a guess can be made for how long the request will take. This will
		 * affect the total values given by the progress reporter until the actual estimate is calculated.
		 */
		val initialTimeEstimate: Duration = 1.seconds,

		/**
		 * If the connection hasn't been made within this timespan, a [com.acornui.io.ResponseConnectTimeoutException]
		 * will be thrown.
		 * This is just the timeout for the connection, not the total request. To add a read timeout, wrap your request
		 * in [com.acornui.async.withTimeout].
		 */
		val connectTimeout: Duration = 30.seconds
)