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

package com.acornui.signal

import com.acornui.Disposable

@Deprecated("use `once`", ReplaceWith("once(handler)"))
fun <T : Any> Signal<T>.addOnce(handler: (T) -> Unit) = once(handler)

/**
 * Adds a signal and creates a [Disposable] handle that, when invoked, will remove the handler.
 */
@Deprecated("Use invoke", ReplaceWith("invoke(isOnce, null, handler)"), DeprecationLevel.ERROR)
fun <T : Any> Signal<T>.addWithHandle(isOnce: Boolean, handler: (T) -> Unit): Disposable = error("deprecated")

@Deprecated("Use invoke", ReplaceWith("invoke(handler)"), DeprecationLevel.ERROR)
fun <T : Any> Signal<T>.addWithHandle(handler: (T) -> Unit): Disposable = listen(handler)