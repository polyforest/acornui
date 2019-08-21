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

import com.acornui.graphic.PopUpSpecs


interface Location {

	/**
	 * The entire URL.
	 */
	val href: String

	/**
	 * The url without the querystring or hash
	 */
	val hrefBase: String
		get() = href.split('?')[0].split('#')[0]

	/**
	 * The canonical form of the origin of the specific location.
	 */
	val origin: String

	/**
	 * The protocol scheme of the URL, including the final ':'.
	 */
	val protocol: String

	/**
	 * The hostname, a ':', and the port of the URL.
	 */
	val host: String

	/**
	 * The domain of the URL.
	 */
	val hostname: String

	/**
	 * The port number of the URL.
	 */
	val port: String

	/**
	 * Containing an initial '/' followed by the path of the URL.
	 */
	val pathname: String

	/**
	 * Containing a '?' followed by the parameters of the URL. Also known as "querystring".
	 */
	val search: String
	val searchParams: UrlParams
		get() = (if (search.isEmpty()) "" else search.substring(1)).toUrlParams()

	/**
	 * Containing a '#' followed by the fragment identifier of the URL.
	 */
	val hash: String

	fun reload()

	fun navigateToUrl(url: String) = navigateToUrl(url, "_self", null)
	fun navigateToUrl(url: String, target: String) = navigateToUrl(url, target, null)
	fun navigateToUrl(url: String, name: String, specs: PopUpSpecs?)

}
