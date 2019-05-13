package com.acornui.core

import kotlin.browser.window

/**
 * A flag for enabling various debugging features like debug logging.
 */
actual val debug: Boolean by lazy { (window.location.search.contains(Regex("""[&?]debug=(true|1)"""))) }