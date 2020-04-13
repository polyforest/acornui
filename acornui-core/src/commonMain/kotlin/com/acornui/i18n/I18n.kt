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

import com.acornui.Disposable
import com.acornui.asset.cacheSet
import com.acornui.asset.loadText
import com.acornui.collection.stringMapOf
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.observe.Observable
import com.acornui.observe.bind
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.signal.bind
import com.acornui.system.userInfo
import com.acornui.text.PropertiesParser
import kotlinx.coroutines.launch

interface I18n {

	/**
	 * Returns a localization bundle for the given bundle name.
	 * This bundle will be for the [com.acornui.system.UserInfo.currentLocale] locale chain.
	 */
	fun getBundle(bundleName: String): I18nBundleRo

	/**
	 * Returns a localization bundle for the given bundle name and locales.
	 * Use this if you wish to get localization values for a specific locale chain, as opposed to the current locale.
	 * @param locales A prioritized list of the locales to which the bundle should be bound.
	 */
	fun getBundle(locales: List<Locale>, bundleName: String): I18nBundleRo

	companion object : Context.Key<I18n> {

		override val factory = dependencyFactory {
			I18nImpl(it)
		}
	}
}

interface I18nBundleRo : Observable {

	override val changed: Signal<(I18nBundleRo) -> Unit>

	/**
	 * Retrieves the i18n string with the given key for this bundle and current locale.
	 * This should typically only be called within an i18n binding callback.
	 * @see i18n
	 */
	operator fun get(key: String): String?

}

fun I18nBundleRo.getOrElse(key: String, default: String = "") = get(key) ?: default

class I18nImpl(owner: Context) : ContextImpl(owner), I18n {

	private val cacheSet = cacheSet()

	/**
	 * A key to represent the bundles that change depending on the user's current locale.
	 */
	private val userCurrentKey = emptyList<Locale>()

	/**
	 * Locale Key -> Bundle Name -> Property Key -> Value
	 */
	private val bundleValues = HashMap<Locale, MutableMap<String, Map<String, String>>>()

	private val bundles = HashMap<List<Locale>, MutableMap<String, I18nBundleImpl>>()

	private val currentLocaleBinding: Disposable

	private val userCurrentBundles: Map<String, I18nBundleImpl>
		get() = bundles.getOrPut(userCurrentKey) { stringMapOf() }

	init {
		currentLocaleBinding = userInfo.currentLocale.bind {
			for (bundle in userCurrentBundles.values) {
				bundle.notifyChanged()
			}
		}
	}

	override fun getBundle(bundleName: String): I18nBundleRo = getBundleInternal(bundleName)

	override fun getBundle(locales: List<Locale>, bundleName: String): I18nBundleRo = getBundleInternal(locales, bundleName)

	private fun getBundleInternal(bundleName: String): I18nBundleImpl = getBundleInternal(userCurrentKey, bundleName)
	private fun getBundleInternal(locales: List<Locale>, bundleName: String): I18nBundleImpl {
		return bundles.getOrPut(locales) { stringMapOf() }.getOrPut(bundleName) {
			loadBundle(locales, bundleName)
			I18nBundleImpl(this, locales, bundleName)
		}
	}

	/**
	 * Sets the bundle values. This will trigger [I18nBundleRo.changed] signals on the bundles with the same name.
	 */
	private fun setBundleValues(locale: Locale, bundleName: String, values: Map<String, String>) {
		val bundlesForLocale = bundleValues.getOrPut(locale) { HashMap() }
		if (bundlesForLocale[bundleName] == values) return // No-op
		bundlesForLocale[bundleName] = values
		// Notify bundles containing a matching locale there were changes.
		getBundleInternal(bundleName).notifyChanged()
		for (entry in bundles) {
			if (entry.key.contains(locale))
				entry.value[bundleName]?.notifyChanged()
		}
	}

	fun getString(locales: List<Locale>, bundleName: String, key: String): String? {
		for (locale in locales) {
			val str = bundleValues[locale]?.get(bundleName)?.get(key)
			if (str != null) return str
		}
		return bundleValues.values.firstOrNull()?.get(bundleName)?.get(key)
	}

	fun getString(bundleName: String, key: String): String? =
			getString(userInfo.currentLocale.value, bundleName, key)


	private fun loadBundle(locales: List<Locale>, bundleName: String) {
		val path2 = manifestPath.replace("{bundleName}", bundleName)
		launch {
			val availableLocales = cacheSet.getOrPutAsync(path2) {
				loadText(path2).split(",").map { Locale(it.trim()) }
			}.await()

			// Find the best available locale to load.
			val matchedLocale: Locale? = locales.find { availableLocales.contains(it) } ?: availableLocales.firstOrNull()

			if (matchedLocale != null) {
				val path = i18nPath.replace("{locale}", matchedLocale.value).replace("{bundleName}", bundleName)
				val values = cacheSet.getOrPutAsync(path) {
					PropertiesParser.parse(loadText(path))
				}.await()
				setBundleValues(matchedLocale, bundleName, values)
			}
		}
	}

	override fun dispose() {
		super.dispose()
		currentLocaleBinding.dispose()
	}

	companion object {

		/**
		 * The tokenized path for property files for a specific locale.
		 */
		var i18nPath = "assets/res/{bundleName}_{locale}.properties"

		/**
		 * The tokenized path to the
		 */
		var manifestPath = "assets/res/{bundleName}.txt"
	}
}

class I18nBundleImpl(private val i18n: I18nImpl, private val locales: List<Locale>, private val bundleName: String) : I18nBundleRo, Disposable {

	private val _changed = Signal1<I18nBundleRo>()
	override val changed = _changed.asRo()

	fun notifyChanged() {
		_changed.dispatch(this)
	}

	override fun get(key: String): String? {
		return if (locales.isEmpty()) i18n.getString(bundleName, key)
		else i18n.getString(locales, bundleName, key)
	}

	override fun dispose() {
		_changed.dispose()
	}
}