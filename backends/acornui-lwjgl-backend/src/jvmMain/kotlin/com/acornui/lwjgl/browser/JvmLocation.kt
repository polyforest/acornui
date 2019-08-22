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

package com.acornui.lwjgl.browser

import com.acornui.browser.Location
import com.acornui.graphic.PopUpSpecs
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
