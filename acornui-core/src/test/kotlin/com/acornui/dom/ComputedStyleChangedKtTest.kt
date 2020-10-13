/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.dom

import com.acornui.AppConfig
import com.acornui.component.Stage
import com.acornui.component.div
import com.acornui.signal.once
import com.acornui.system.isNode
import com.acornui.test.runApplicationTest
import kotlinx.browser.window
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.seconds

class ComputedStyleChangedKtTest {

	@Test fun classChanged() = runBrowserTest { resolve, _ ->
		val style = addStyleToHead("""
.red {
  color: red;
}
		""")

		val c = +div()
		val computed = window.getComputedStyle(c.dom)
		c.computedStyleChanged("color").once {
			assertEquals("rgb(255, 0, 0)", computed.getPropertyValue("color"))
			style.remove()
			resolve()
		}
		c.dom.className = "red"
	}

	@Test fun parentClassChanged() = runBrowserTest { resolve, _ ->
		val style = addStyleToHead("""
.red {
  color: red;
}
		""")

		val p = +div()
		val c = p.addElement(div())
		val computed = window.getComputedStyle(c.dom)
		c.computedStyleChanged("color").once {
			assertEquals("rgb(255, 0, 0)", computed.getPropertyValue("color"))
			style.remove()
			resolve()
		}
		p.dom.className = "red"
	}

	@Test fun classListChanged() = runBrowserTest { resolve, _ ->
		val style = addStyleToHead("""
.red {
  color: rgb(255, 0, 0);
}

.blue {
  color: rgb(0, 0, 255);
}
		""")

		val c = +div()
		val computed = window.getComputedStyle(c.dom)
		c.computedStyleChanged("color").once {
			assertEquals("rgb(255, 0, 0)", computed.getPropertyValue("color"))

			c.computedStyleChanged("color").once {
				assertEquals("rgb(0, 0, 255)", computed.getPropertyValue("color"))
				style.remove()
				resolve()
			}
			c.dom.classList.add("blue")
		}
		c.dom.classList.add("red")
	}

	@Test fun styleElementAdded() = runBrowserTest { resolve, reject ->
		val c = +div()
		c.dom.classList.add("green")
		val computed = window.getComputedStyle(c.dom)
		val styleElement = styleElement {
			innerHTML = """
			.green {
				color: rgb(0, 255, 0);
			}
		"""
		}
		c.computedStyleChanged("color").once {
			assertEquals("rgb(0, 255, 0)", computed.getPropertyValue("color"))
			styleElement.remove()
			resolve()
		}

		head.add(styleElement)
	}

	@Test fun styleElementChanged() = runBrowserTest { resolve, reject ->
		val c = +div()
		c.dom.classList.add("greenToYellow")
		val computed = window.getComputedStyle(c.dom)
		val styleElement = addStyleToHead("""
			.greenToYellow {
				color: rgb(0, 255, 0);
			}
		""")
		c.computedStyleChanged("color").once {
			assertEquals("rgb(255, 255, 0)", computed.getPropertyValue("color"))
			styleElement.remove()
			resolve()
		}

		println("Setting inner html on style element")
		styleElement.innerHTML = """
			.greenToYellow {
				color: rgb(255, 255, 0);
			}
		"""
	}

	@Test fun stylePropertyChanged() = runBrowserTest { resolve, reject ->
		val c = +div()
		val computed = window.getComputedStyle(c.dom)
		c.computedStyleChanged("color").once {
			assertEquals("rgb(0, 255, 0)", computed.getPropertyValue("color"))
			resolve()
		}
		c.style.setProperty("color", "rgb(0, 255, 0)")
	}
}

private fun runBrowserTest(
	appConfig: AppConfig = AppConfig(),
	timeout: Duration = 10.seconds,
	block: Stage.(resolve: () -> Unit, reject: (e: Throwable) -> Unit) -> Unit
): Promise<Unit> = runApplicationTest(appConfig, timeout) { resolve: () -> Unit, reject: (e: Throwable) -> Unit ->
	if (isNode) resolve()
	else {
		block(resolve, reject)
	}
}