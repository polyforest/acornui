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

package com.acornui.i18n

import com.acornui.asset.cacheSet
import com.acornui.asset.loadText
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.text.PropertiesParser

/**
 * Loads localized resources.
 */
interface I18n {

	/**
	 * Loads and returns a localized string for the given Locale chain, resource bundle, and property key.
	 */
	suspend fun string(locales: List<Locale>, bundleName: String, key: String): String?

	companion object : Context.Key<I18n> {

		override val factory = dependencyFactory {
			I18nImpl(it)
		}
	}
}


class I18nImpl(

		owner: Context,

		/**
		 * The tokenized path for property files for a specific locale.
		 */
		private val i18nPath: String = "assets/res/{bundleName}_{locale}.properties",

		/**
		 * The tokenized path to the bundle's supported locales list.
		 */
		private val manifestPath: String = "assets/res/{bundleName}.txt"
) : ContextImpl(owner), I18n {

	private val cacheSet = cacheSet()

	private suspend fun loadBundle(locales: List<Locale>, bundleName: String): Map<String, String>? {
		val finalManifestPath = manifestPath.replace("{bundleName}", bundleName)
		val availableLocales = cacheSet.getOrPutAsync(finalManifestPath) {
			loadText(finalManifestPath).split(",").map { Locale(it.trim()) }
		}.await()

		// Find the best available locale to load.
		val matchedLocale: Locale = locales.find { availableLocales.contains(it) } ?: availableLocales.firstOrNull()
		?: return null

		val path = i18nPath.replace("{locale}", matchedLocale.value).replace("{bundleName}", bundleName)
		return cacheSet.getOrPutAsync(path) {
			PropertiesParser.parse(loadText(path))
		}.await()
	}

	override suspend fun string(locales: List<Locale>, bundleName: String, key: String): String? {
		return loadBundle(locales, bundleName)?.get(key)
	}

	override fun dispose() {
		super.dispose()
		cacheSet.dispose()
	}

}