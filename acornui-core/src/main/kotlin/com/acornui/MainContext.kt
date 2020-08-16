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

import com.acornui.di.ContextImpl
import com.acornui.di.ContextMarker
import com.acornui.logging.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job

/**
 * Returns a coroutine uncaught exception handler that logs the exception.
 */
val coroutineExceptionLogger = CoroutineExceptionHandler { _, exception ->
	Log.error(exception)
}

/**
 * The root scope.
 * There may be multiple applications per page, but there should be only one main context.
 *
 * To create a dependency shared across all applications, you may either set a custom mainContext before any
 * applications are created with the dependencies provided, or create a dependency factory using the
 * [ContextMarker.MAIN] marker.
 *
 * E.g.:
 * ```
 * interface MainCache {
 *
 *   companion object : Context.Key<MainCache> {
 *     override val factory = dependencyFactory(ContextMarker.MAIN) {
 *	    MainCacheImpl(it)
 *	   }
 *   }
 * }
 * ```
 *
 *
 * NB:
 * This is mutable for the sake of unit tests, or if a custom main context is needed. Typically only one main context
 * should be set per page.
 */
var mainContext = ContextImpl(coroutineContext = coroutineExceptionLogger + Job(), marker = ContextMarker.MAIN)

/**
 * Disposes and recreates the [mainContext].
 */
internal fun resetMainContext() {
	mainContext = ContextImpl(coroutineContext = coroutineExceptionLogger + Job(), marker = ContextMarker.MAIN)
}
