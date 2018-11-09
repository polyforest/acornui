package com.acornui.js.window

import com.acornui.core.browser.Location
import com.acornui.core.graphic.PopUpSpecs
import kotlin.browser.window
import org.w3c.dom.Location as DomLocation

class JsLocation(private val location: DomLocation) : Location {

	override val href: String
		get() = location.href
	override val origin: String
		get() = location.origin
	override val protocol: String
		get() = location.protocol
	override val host: String
		get() = location.host
	override val hostname: String
		get() = location.hostname
	override val port: String
		get() = location.port
	override val pathname: String
		get() = location.pathname
	override val search: String
		get() = location.search

	override val hash: String
		get() = location.hash

	override fun reload() {
		location.reload()
	}

	override fun navigateToUrl(url: String, name: String, specs: PopUpSpecs?) {
		window.open(url, name, specs?.toSpecsString() ?: "")
	}
}

//external fun DomLocation.reload(forcedReload: Boolean)