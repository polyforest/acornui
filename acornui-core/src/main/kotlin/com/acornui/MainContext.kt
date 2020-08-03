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

package com.acornui

import com.acornui.component.ComponentInit
import com.acornui.di.*
import com.acornui.logging.Log
import kotlinx.coroutines.Job
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

class MainContext(
	dependencies: DependencyMap = DependencyMap(),
	coroutineContext: CoroutineContext = Log.uncaughtExceptionHandler + Job()
) : ContextImpl(null, dependencies, coroutineContext, ContextMarker.MAIN)

/**
 * Constructs a new [MainContext].
 * @return Returns Unit, in order to be easily used as an expression body of `main()`.
 */
inline fun mainContext(dependencies: DependencyMap = DependencyMap(), init: ComponentInit<MainContext> = {}) {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	MainContext(dependencies).apply(init)
}

/**
 * Finds the [MainContext] on the owner ancestry.
 */
val Context.mainContext: MainContext
	get() = findOwner { it.marker == ContextMarker.MAIN }!! as MainContext

/**
 * Disposes the main context, thereby disposing all applications.
 */
fun Context.exitMain() {
	findOwner { it.marker == ContextMarker.MAIN }!!.dispose()
}
