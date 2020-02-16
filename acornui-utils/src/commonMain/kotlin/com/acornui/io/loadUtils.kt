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

import com.acornui.browser.UrlParams
import com.acornui.browser.toUrlParams
import com.acornui.recycle.Clearable
import kotlin.time.Duration

// TODO: make serializable

/**
 * A model with the necessary information to make an http request.
 * This is also used for file requests.
 */
data class UrlRequestData(

		val url: String = "",

		val method: String = UrlRequestMethod.GET,

		val headers: Map<String, String> = HashMap(),

		val user: String? = null,

		val password: String? = null,

		val formData: MultipartFormDataRo? = null,

		val variables: UrlParams? = null,

		var body: String? = null,

		/**
		 * The number of seconds a request can take before automatically being terminated.
		 */
		val timeout: Float = defaultRequestTimeout
) {

	fun toUrlStr(): String {
		return if (method == UrlRequestMethod.GET && variables != null)
			url + "?" + variables!!.toQueryString() else url
	}

	companion object {

		/**
		 * When new requests are made, this will be the default timeout unless specified explicitly.
		 */
		var defaultRequestTimeout: Float = 30f
	}
}

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
	val GET: String = "GET"
	val POST: String = "POST"
	val PUT: String = "PUT"
	val DELETE: String = "DELETE"
}

open class ResponseException(val status: Short, message: String?, val detail: String) : Throwable(message) {

	override fun toString(): String {
		return "ResponseException(status=$status, message=$message, detail=$detail)"
	}
}

interface MultipartFormDataRo {
	val items: List<FormDataItem>
}

class MultipartFormData : Clearable, MultipartFormDataRo {

	private val _items = ArrayList<FormDataItem>()
	override val items: List<FormDataItem>
		get() = _items

	fun append(name: String, value: NativeReadByteBuffer, filename: String? = null) {
		_items.add(ByteArrayFormItem(name, value, filename))
	}

	fun append(name: String, value: String) {
		_items.add(StringFormItem(name, value))
	}

	override fun clear() {
		_items.clear()
	}
}

/**
 * A marker interface for items that can be in the list of [MultipartFormData.items]
 */
interface FormDataItem {
	val name: String
}

class ByteArrayFormItem(
		override val name: String,
		val value: NativeReadByteBuffer,
		val filename: String?
) : FormDataItem

class StringFormItem(
		override val name: String,
		val value: String
) : FormDataItem

interface Loader<out T> {

	val defaultInitialTimeEstimate: Duration

	suspend fun load(requestData: UrlRequestData,
					 progressReporter: ProgressReporter,
					 initialTimeEstimate: Duration = defaultInitialTimeEstimate
	): T
}

expect class TextLoader() : Loader<String>
expect class BinaryLoader() : Loader<ReadByteBuffer>