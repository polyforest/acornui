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

import kotlinx.coroutines.CoroutineScope

/**
 * JS doesn't have the equivalent of 'runBlocking', but in order to make the JS and JVM backends as consistent as
 * possible we make the [com.acornui.Application.start] method `suspend`, and then wrap the main method in `runMain`.
 * There should only be one `main` method, with one `runMain` call. JVM backends this will be blocking, JS backends
 * will not.
 *
 * [https://github.com/Kotlin/kotlinx.coroutines/issues/195]
 */
expect fun runMain(block: suspend CoroutineScope.() -> Unit)