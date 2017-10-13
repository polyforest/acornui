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

package com.acornui.js.io

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.w3c.dom.Document
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.files.Blob
import org.w3c.xhr.FormData

/**
 * @author nbilyk
 */
//@Deprecated("kotlin now has this")
//external class XMLHttpRequest : EventTarget {
//
//	/**
//	 * Initializes a request.
//	 *
//	 * @param method The HTTP method to use, such as "GET", "POST", "PUT", "DELETE", etc. Ignored for non-HTTP(S) URLs.
//	 * @param url The URL to send the request to.
//	 * @param async An optional boolean parameter, defaulting to true, indicating whether or not to perform the
//	 * operation asynchronously. If this value is false, the send()method does not return until the response is
//	 * received. If true, notification of a completed transaction is provided using event listeners. This must be
//	 * true if the multipart attribute is true, or an exception will be thrown.
//	 * @param user The optional user name to use for authentication purposes; by default, this is an empty string.
//	 * @param password The optional password to use for authentication purposes; by default, this is an empty string.
//	 *
//	 */
//	fun open(method: String, url: String, async: Boolean = definedExternally, user: String = definedExternally, password: String = definedExternally): Unit
//
//	/**
//	 * Overrides the MIME type returned by the server. This may be used, for example, to force a stream to be treated
//	 * and parsed as text/xml, even if the server does not report it as such. This method must be called before send().
//	 */
//	fun overrideMimeType(mime: String): Unit
//
//	/**
//	 * Sends the request. If the request is asynchronous (which is the default), this method returns as soon as the
//	 * request is sent. If the request is synchronous, this method doesn't return until the response has arrived.
//	 *
//	 * Note: Any event listeners you wish to set must be set before calling send().
//	 */
//	fun send(): Unit
//
//	fun send(data: ArrayBuffer): Unit
//
//	fun send(data: ArrayBufferView): Unit
//
//	fun send(data: Blob): Unit
//
//	fun send(data: Document): Unit
//
//	fun send(data: FormData): Unit
//
//	fun send(data: String): Unit
//
//	/**
//	 * Sets the value of an HTTP request header. You must call setRequestHeader() after open(), but before send().
//	 * If this method is called several times with the same header, the values are merged into one single request header.
//	 */
//	fun setRequestHeader(header: String, value: String): Unit
//
//
//	/**
//	 * Aborts the request if it has already been sent.
//	 */
//	fun abort(): Unit
//
//	/**
//	 * Returns all the response headers as a string, or null if no response has been received. Note: For multipart
//	 * requests, this returns the headers from the current part of the request, not from the original channel.
//	 */
//	fun getAllResponseHeaders(): String
//
//	/**
//	 * Returns the string containing the text of the specified header, or null if either the response has not yet been
//	 * received or the header doesn't exist in the response.
//	 */
//	fun getResponseHeader(header: String): String?
//
//	/**
//	 * @see XmlHttpRequestReadyState
//	 */
//	var readyState: Int
//
//	/**
//	 * The response to the request as text, or null if the request was unsuccessful or has not yet been sent.
//	 */
//	var responseText: String?
//
//	/**
//	 * Can be set to change the response type.
//	 * You must set responseType after open(), but before send().
//	 * @see XmlHttpRequestResponseType
//	 */
//	var responseType: String
//
//	var onprogress: (e: ProgressEvent) -> Unit
//
//	/**
//	 * A function that is called whenever the readyState attribute changes. The callback is
//	 * called from the user interface thread.
//	 */
//	var onreadystatechange: (e: Event) -> Unit?
//
//	/**
//	 * The response entity body according to responseType, as an ArrayBuffer, Blob, Document,
//	 * JavaScript object (for "json"), or string. This is null if the request is not complete or was not successful.
//	 */
//	var response: Any?
//
//	/**
//	 * The response to the request as a DOM Document object, or null if the request was unsuccessful, has not yet
//	 * been sent, or cannot be parsed as XML or HTML. The response is parsed as if it were a text/xml stream.
//	 * When the responseType is set to "document" and the request has been made asynchronously, the response is
//	 * parsed as a text/html stream.
//	 *
//	 * Note: If the server doesn't apply the text/xml Content-Type header, you can use overrideMimeType()to force
//	 * XMLHttpRequest to parse it as XML anyway.
//	 */
//	var responseXML: Document?
//
//	/**
//	 * The status of the response to the request. This is the HTTP result code
//	 * (for example, status is 200 for a successful request).
//	 */
//	var status: Int
//
//	/**
//	 * (Read-only) The response string returned by the HTTP server. Unlike status, this includes the entire text of
//	 * the response message ("200 OK", for example).
//	 */
//	var statusText: String
//
//	/**
//	 * The number of milliseconds a request can take before automatically being terminated.
//	 * A value of 0 (which is the default) means there is no timeout.
//	 */
//	var timeout: Long
//
//	/**
//	 * The upload process can be tracked by adding an event listener to upload.
//	 */
//	var upload: EventTarget
//
//	/**
//	 * Indicates whether or not cross-site Access-Control requests should be made using credentials such as cookies
//	 * or authorization headers. The default is false.
//	 *
//	 * Note: This never affects same-site requests.
//	 */
//	var withCredentials: Boolean
//}
//
//object XMLHttpRequestResponseType {
//	val DEFAULT: String = ""
//	val ARRAY_BUFFER: String = "arraybuffer"
//	val BLOB: String = "blob"
//	val DOCUMENT: String = "document"
//	val JSON: String = "json"
//	val TEXT: String = "text"
//}
//
//object XMLHttpRequestMethod {
//	val GET: String = "GET"
//	val POST: String = "POST"
//	val PUT: String = "PUT"
//	val DELETE: String = "DELETE"
//}
//
//object XMLHttpRequestReadyState {
//
//	/**
//	 * open() has not been called yet.
//	 */
//	val UNSENT: Int = 0
//
//	/**
//	 * send() has not been called yet.
//	 */
//	val OPENED: Int = 1
//
//	/**
//	 * send() has been called, and headers and status are available.
//	 */
//	val HEADERS_RECEIVED: Int = 2
//
//	/**
//	 * Downloading; responseText holds partial data.
//	 */
//	val LOADING: Int = 3
//
//	/**
//	 * The operation is complete.
//	 */
//	val DONE: Int = 4
//
//}
//
//external class ProgressEvent(type: String) : Event{
//
//	/**
//	 * Is a Boolean flag indicating if the total work to be done, and the amount of work already done, by the
//	 * underlying process is calculable. In other words, it tells if the progress is measurable or not.
//	 */
//	var lengthComputable: Boolean
//
//	/**
//	 * Is an unsigned long long representing the amount of work already performed by the underlying process.
//	 * The ratio of work done can be calculated with the property and ProgressEvent.total. When downloading a
//	 * resource using HTTP, this only represent the part of the content itself, not headers and other overhead.
//	 */
//	var loaded: Int
//
//	/**
//	 * Is an unsigned long long representing the total amount of work that the underlying process is in the progress
//	 * of performing. When downloading a resource using HTTP, this only represent the content itself, not headers
//	 * and other overhead.
//	 */
//	var total: Int
//}