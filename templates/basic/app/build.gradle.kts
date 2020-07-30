plugins {
	kotlin("js")
	kotlin("plugin.serialization")
	id("com.acornui.js")
}

kotlin.js.browser.binaries.executable()
