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

package com.acornui.browser

import kotlinx.browser.window

@Deprecated("Use window.location directly")
object Location {

	/**
	 * The entire URL.
	 */
	@Deprecated("", ReplaceWith("window.location.href"))
	val href: String
		get() = window.location.href

	/**
	 * The url without the querystring or hash
	 */
	val hrefBase: String
		get() = href.split('?')[0].split('#')[0]

	/**
	 * The canonical form of the origin of the specific location.
	 */
	@Deprecated("", ReplaceWith("window.location.origin"))
	val origin: String
		get() = window.location.origin

	/**
	 * The protocol scheme of the URL, including the final ':'.
	 */
	@Deprecated("", ReplaceWith("window.location.protocol"))
	val protocol: String
		get() = window.location.protocol

	/**
	 * The hostname, a ':', and the port of the URL.
	 */
	@Deprecated("", ReplaceWith("window.location.host"))
	val host: String
		get() = window.location.host

	/**
	 * The domain of the URL.
	 */
	@Deprecated("", ReplaceWith("window.location.hostname"))
	val hostname: String
		get() = window.location.hostname

	/**
	 * The port number of the URL.
	 */
	@Deprecated("", ReplaceWith("window.location.port"))
	val port: String
		get() = window.location.port

	/**
	 * Containing an initial '/' followed by the path of the URL.
	 */
	@Deprecated("", ReplaceWith("window.location.pathname"))
	val pathname: String
		get() = window.location.pathname

	/**
	 * Containing a '?' followed by the parameters of the URL. Also known as "querystring".
	 */
	@Deprecated("", ReplaceWith("window.location.search"))
	val search: String
		get() = window.location.search

	val searchParams: UrlParams
		get() = (if (search.isEmpty()) "" else search.substring(1)).toUrlParams()

	/**
	 * Containing a '#' followed by the fragment identifier of the URL.
	 */
	@Deprecated("", ReplaceWith("window.location.hash"))
	val hash: String
		get() = window.location.hash

	@Deprecated("", ReplaceWith("window.location.reload()"))
	fun reload() {
		window.location.reload()
	}

	fun navigateToUrl(url: String) = navigateToUrl(url, "_self", null)
	fun navigateToUrl(url: String, target: String) = navigateToUrl(url, target, null)


	@Deprecated("", ReplaceWith("window.location.reload()"))
	fun navigateToUrl(url: String, name: String, specs: PopUpSpecs?) {
		window.open(url, name, specs?.toSpecsString() ?: "")
	}
}

val location: Location
	get() = Location
