package com.acornui.async

import kotlin.browser.window

/**
 * If true, it is possible to see which co-routines are stuck and what invoked them.
 *
 * To check active co-routines, pause the IDEA debugger, then Evaluate expression:
 * `com.acornui.async.AsyncKt.activeCoroutinesStr`
 */
actual val debugCoroutines: Boolean by lazy { window.location.search.contains(Regex("""[&?]debugCoroutines=(true|1)""")) }