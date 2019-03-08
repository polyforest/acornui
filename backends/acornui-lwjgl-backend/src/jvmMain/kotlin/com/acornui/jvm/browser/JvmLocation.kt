package com.acornui.jvm.browser

import com.acornui.core.browser.Location
import com.acornui.core.graphic.PopUpSpecs
import java.awt.Desktop
import java.net.URI

class JvmLocation : Location {

	override val href: String = ""
	override val origin: String = ""
	override val protocol: String = ""
	override val host: String = ""
	override val hostname: String = ""
	override val port: String = ""
	override val pathname: String = ""
	override val search: String = ""
	override val hash: String = ""

	override fun reload() {
	}

	override fun navigateToUrl(url: String, name: String, specs: PopUpSpecs?) {
		if (Desktop.isDesktopSupported()) {
			val uri = URI(url)
			if (uri.isAbsolute)
				Desktop.getDesktop().browse(uri)
		}
	}
}