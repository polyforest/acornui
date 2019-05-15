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

package com.acornui.async

import kotlin.browser.window

/**
 * If true, it is possible to see which co-routines are stuck and what invoked them.
 *
 * To check active co-routines, pause the IDEA debugger, then Evaluate expression:
 * `com.acornui.async.AsyncKt.activeCoroutinesStr`
 */
actual val debugCoroutines: Boolean by lazy { window.location.search.contains(Regex("""[&?]debugCoroutines=(true|1)""")) }
