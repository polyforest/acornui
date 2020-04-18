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

package com.acornui.i18n

import com.acornui.Disposable
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.own
import com.acornui.logging.Log
import com.acornui.observe.bind
import com.acornui.system.userInfo
import kotlinx.coroutines.launch


/**
 * Sets the default locale bundle to the given value.
 * This should be set before any [string] bindings are made, the bundle name is not expected to change.
 */
fun ContextImpl.i18nBundle(bundleName: String) {
	dependencies += I18N_BUNDLE to bundleName
}

/**
 * The dependency key for getting the default locale bundle.
 * This can be set with [ContextImpl.i18nBundle].
 */
val I18N_BUNDLE = object : Context.Key<String> {}

val Context.i18nBundleName: String
	get() = injectOptional(I18N_BUNDLE) ?: error("No default locale bundle set, use i18nBundle()")

/**
 * Creates a binding to the user's current Locale
 */
fun Context.i18n(callback: suspend () -> Unit): Disposable {
	return bind(userInfo.currentLocale.changed) {
		launch {
			callback()
		}
	}
}

/**
 * Returns the String associated with the default bundle and current locale chain.
 * @param key The
 * @see i18nBundle
 */
suspend fun Context.string(key: String, bundleName: String = i18nBundleName, localeChain: List<Locale> = userInfo.currentLocale.value): String {
	return inject(I18n).string(localeChain, bundleName, key) ?: "".also { Log.warn("i18n string not found. key='$key' bundle='$bundleName' localeChain='${localeChain.joinToString { it.value }}'") }
}
