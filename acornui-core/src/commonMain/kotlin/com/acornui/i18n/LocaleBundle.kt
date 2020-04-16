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
import com.acornui.signal.addWithHandle


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
	get() = injectOptional(I18N_BUNDLE) ?: error("No default locale bundle set, use localeBundle()")

val Context.i18nBundle: I18nBundleRo
	get() = inject(I18n).getBundle(i18nBundleName)

/**
 * Creates a binding to a resource bundle.
 */
fun Context.i18n(bundleName: String, callback: (bundle: I18nBundleRo) -> Unit) : Disposable {
	val bundle = inject(I18n).getBundle(bundleName)
	return own(bundle.changed.addWithHandle(callback)).also { callback(bundle) }
}

/**
 * Creates a binding to the default resource bundle.
 * @see i18nBundle
 */
fun Context.i18n(callback: (bundle: I18nBundleRo) -> Unit) : Disposable =
		i18n(i18nBundleName, callback)

/**
 * Returns the String associated with the given bundle and locale key.
 */
fun Context.string(bundleName: String, key: String, default: String = ""): String {
	return inject(I18n).getBundle(bundleName).getOrElse(key, default)
}

/**
 * Returns the String associated with the default bundle and locale key.
 * @see i18nBundle
 */
fun Context.string(key: String, default: String = ""): String = string(i18nBundleName, key, default)
