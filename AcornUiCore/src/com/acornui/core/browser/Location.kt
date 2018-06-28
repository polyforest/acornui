package com.acornui.core.browser

import com.acornui.browser.UrlParams
import com.acornui.browser.UrlParamsImpl
import com.acornui.core.graphics.PopUpSpecs


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
		get() = UrlParamsImpl(if (search.isEmpty()) "" else search.substring(1))

	/**
	 * Containing a '#' followed by the fragment identifier of the URL.
	 */
	val hash: String

	fun reload()

	fun navigateToUrl(url: String) = navigateToUrl(url, "_self", null)
	fun navigateToUrl(url: String, target: String) = navigateToUrl(url, target, null)
	fun navigateToUrl(url: String, name: String, specs: PopUpSpecs?)

}