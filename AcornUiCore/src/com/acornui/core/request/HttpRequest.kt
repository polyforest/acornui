/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.core.request

import com.acornui.action.Progress
import com.acornui.async.CancelableDeferred
import com.acornui.browser.UrlParams
import com.acornui.collection.Clearable
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.io.ReadByteBuffer
import com.acornui.io.NativeReadByteBuffer

/**
 * A model with the necessary information to make an http request.
 * This is also used for file requests.
 * @author nbilyk
 */
data class UrlRequestData(

		val url: String = "",

		val method: String = UrlRequestMethod.GET,

		val headers: Map<String, String> = HashMap(),

		val user: String? = null,

		val password: String? = null,

		val formData: MultipartFormDataRo? = null,

		var variables: UrlParams? = null,

		var body: String? = null,

		/**
		 * The number of milliseconds a request can take before automatically being terminated.
		 * A value of 0 (which is the default) means there is no timeout.
		 */
		val timeout: Long = 0L
) {
}

/**
 * The possible values for UrlRequest.method
 */
object UrlRequestMethod {
	val GET: String = "GET"
	val POST: String = "POST"
	val PUT: String = "PUT"
	val DELETE: String = "DELETE"
}
interface RestServiceFactory {

	fun createTextRequest(injector: Injector, requestData: UrlRequestData): Request<String>
	fun createBinaryRequest(injector: Injector, requestData: UrlRequestData): Request<ReadByteBuffer>

	companion object : DKey<RestServiceFactory>
}

interface Request<out T>: Progress, CancelableDeferred<T>

fun Scoped.createTextRequest(requestData: UrlRequestData): Request<String> {
	return inject(RestServiceFactory).createTextRequest(injector, requestData)
}

fun Scoped.createBinaryRequest(requestData: UrlRequestData): Request<ReadByteBuffer> {
	return inject(RestServiceFactory).createBinaryRequest(injector, requestData)
}

open class ResponseException(val status: Short, message: String?, val detail: String) : Throwable(message) {

	override fun toString(): String {
		return "ResponseException(status=$status, message=$message)"
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