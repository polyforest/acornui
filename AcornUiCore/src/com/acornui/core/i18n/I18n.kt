package com.acornui.core.i18n

import com.acornui.action.Decorator
import com.acornui.async.then
import com.acornui.collection.firstOrNull2
import com.acornui.collection.stringMapOf
import com.acornui.core.Disposable
import com.acornui.core.assets.*
import com.acornui.core.di.DKey
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.io.file.Files
import com.acornui.core.removeBackslashes
import com.acornui.core.replace2
import com.acornui.core.userInfo
import com.acornui.observe.Observable
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.string.StringParser

interface I18n {

	/**
	 * Returns a localization bundle for the given bundle name.
	 * This bundle will be for the [com.acornui.core.UserInfo.currentLocale] locale chain.
	 */
	fun getBundle(bundleName: String): I18nBundleRo

	/**
	 * Returns a localization bundle for the given bundle name and locales.
	 * Use this if you wish to get localization values for a specific locale chain, as opposed to the current locale.
	 * @param locales A prioritized list of the locales to which the bundle should be bound.
	 */
	fun getBundle(locales: List<Locale>, bundleName: String): I18nBundleRo

	/**
	 * Sets the bundle values. This will trigger [I18nBundleRo.changed] signals on the bundles with the same name.
	 */
	fun setBundleValues(locale: Locale, bundleName: String, values: Map<String, String>)

	companion object : DKey<I18n> {

		val UNDEFINED: Locale = Locale("und")
	}
}

interface I18nBundleRo : Observable {

	/**
	 * Retrieves the i18n string with the given key for this bundle and current locale.
	 * This should typically only be called within an [i18n] callback.
	 */
	operator fun get(key: String): String?

}

fun I18nBundleRo.getOrElse(key: String, default: String = "???") = get(key) ?: default

class I18nImpl : I18n, Disposable {

	private val currentKey = emptyList<Locale>()

	/**
	 * Locale Key -> Bundle Name -> Property Key -> Value
	 */
	private val _bundleValues = HashMap<Locale, MutableMap<String, Map<String, String>>>()
	private val _bundles = HashMap<List<Locale>, MutableMap<String, I18nBundleImpl>>()

	private val currentLocaleBinding: Disposable

	init {
		_bundles[currentKey] = HashMap()

		currentLocaleBinding = userInfo.currentLocale.bind {
			val defaultBundles = _bundles[currentKey]!!
			for (bundle in defaultBundles.values) {
				bundle.notifyChanged()
			}
		}
	}

	override fun getBundle(bundleName: String): I18nBundleRo {
		val bundles = _bundles[currentKey]!!
		if (!bundles.contains(bundleName)) {
			bundles[bundleName] = I18nBundleImpl(this, null, bundleName)
		}
		return bundles[bundleName]!!
	}

	override fun getBundle(locales: List<Locale>, bundleName: String): I18nBundleRo {
		if (!_bundles.contains(locales)) {
			_bundles[locales] = HashMap()
		}
		val bundles = _bundles[locales]!!
		if (!bundles.contains(bundleName)) {
			bundles[bundleName] = I18nBundleImpl(this, locales, bundleName)
		}
		return bundles[bundleName]!!
	}

	override fun setBundleValues(locale: Locale, bundleName: String, values: Map<String, String>) {
		if (!_bundleValues.contains(locale))
			_bundleValues[locale] = HashMap()
		if (_bundleValues[locale]!![bundleName] === values)
			return
		_bundleValues[locale]!![bundleName] = values
		_bundles[currentKey]?.get(bundleName)?.notifyChanged()
	}

	fun getString(locales: List<Locale>, bundleName: String, key: String): String? {
		for (i in 0..locales.lastIndex) {
			val str = _bundleValues[locales[i]]?.get(bundleName)?.get(key)
			if (str != null) return str
		}
		return _bundleValues[I18n.UNDEFINED]?.get(bundleName)?.get(key)
	}

	fun getString(bundleName: String, key: String): String? {
		return getString(userInfo.currentLocale.value, bundleName, key)
	}

	override fun dispose() {
		currentLocaleBinding.dispose()
	}
}

class I18nBundleImpl(private val i18n: I18nImpl, private val locales: List<Locale>?, private val bundleName: String) : I18nBundleRo, Disposable {

	private val _changed = Signal1<I18nBundleRo>()
	override val changed: Signal<(I18nBundleRo) -> Unit>
		get() = _changed

	fun notifyChanged() {
		_changed.dispatch(this)
	}

	override fun get(key: String): String? {
		return if (locales == null) i18n.getString(bundleName, key)
		else i18n.getString(locales, bundleName, key)
	}

	override fun dispose() {
		_changed.dispose()
	}
}

/**
 * The tokenized path for property files for a specific locale.
 */
var i18nPath = "assets/res/{bundleName}_{locale}.properties"

/**
 * The tokenized path for the fallback property files. This will be used if there was no matching locale.
 */
var i18nFallbackPath = "assets/res/{bundleName}.properties"

/**
 * Loads a resource bundle at the given path for the [com.acornui.core.UserInfo.currentLocale]. When the current locale
 * changes, the new bundle will be automatically loaded.
 *
 * @param path The path to the properties file to load.
 * This may have the tokens:
 * {locale} which will be replaced by [com.acornui.core.UserInfo.currentLocale].
 * {bundleName} which will be replaced by [bundleName].
 * @param defaultPath The path to the default properties to load. The default properties is used as a back-up if the
 * locale isn't found.
 *
 * This method loads the bundle for the current locale, as set by [com.acornui.core.UserInfo.currentLocale]. When the
 * current locale changes, a new load will take place.
 *
 * This may be called multiple times safely without re-loading the data.
 * To retrieve the bundle, use `i18n.getBundle(bundleName)`
 *
 * @return Returns a disposer that can stop watching the current locale for changes and disposes the cached property
 * files.
 *
 * @see I18n.getBundle
 */
fun Scoped.loadBundle(
		bundleName: String,
		path: String = i18nPath,
		defaultPath: String = i18nFallbackPath
): Disposable {
	val cachedGroup = cachedGroup()
	_loadBundle(I18n.UNDEFINED, bundleName, path, defaultPath, cachedGroup)
	val localeBinding = userInfo.currentLocale.bind {
		for (locale in it) {
			_loadBundle(locale, bundleName, path, defaultPath, cachedGroup)
		}
	}
	return object : Disposable {
		override fun dispose() {
			localeBinding.dispose()
			cachedGroup.dispose()
		}
	}
}

/**
 * Loads a resource bundle for the specified locale and bundle. Unlike [loadBundle], this will not be bound to
 * [com.acornui.core.UserInfo.currentLocale].
 */
fun Scoped.loadBundleForLocale(locale: Locale, bundleName: String, path: String = i18nPath): I18nBundleRo {
	return loadBundleForLocale(listOf(locale), bundleName, path)
}

/**
 * Loads a resource bundle for the specified locale chain and bundle. Unlike [loadBundle], this will not be bound to
 * [com.acornui.core.UserInfo.currentLocale].
 */
fun Scoped.loadBundleForLocale(locales: List<Locale>, bundleName: String, path: String = i18nPath): I18nBundleRo {
	val i18n = inject(I18n)
	for (locale in locales) {
		val path2 = path.replace2("{locale}", locale.value).replace2("{bundleName}", bundleName)
		load(path2, AssetType.TEXT).then {
			i18n.setBundleValues(locale, bundleName, PropertiesDecorator.decorate(it))
		}
	}
	return i18n.getBundle(locales, bundleName)
}

private fun Scoped._loadBundle(locale: Locale, bundleName: String, path: String, fallbackPath: String, cachedGroup: CachedGroup) {
	val i18n = inject(I18n)
	val files = inject(Files)
	for (localeToken in locale.toPathStrings()) {
		val path2 = if (locale == I18n.UNDEFINED) {
			fallbackPath.replace2("{bundleName}", bundleName)
		} else {
			path.replace2("{locale}", localeToken).replace2("{bundleName}", bundleName)
		}

		// Only try to load the locale if we know it to exist.
		if (files.getFile(path2) != null) {
			loadAndCache(path2, AssetType.TEXT, PropertiesDecorator, cachedGroup).then {
				i18n.setBundleValues(locale, bundleName, it)
			}
			break
		}
	}
}

/**
 * Provides a list of path tokens to try.  E.g. en-US, en_US
 */
private fun Locale.toPathStrings(): List<String> {
	return listOf(value, value.replace("-", "_"))
}

object PropertiesDecorator : Decorator<String, Map<String, String>> {

	override fun decorate(target: String): Map<String, String> {
		val map = stringMapOf<String>()
		val parser = StringParser(target)
		while (parser.hasNext) {
			val line = parser.readLine().trimStart()
			if (line.startsWith('#') || line.startsWith('!')) {
				continue // Comment
			}
			val separatorIndex = line.indexOf('=')
			if (separatorIndex == -1) continue
			val key = line.substring(0, separatorIndex).trim()
			var value = line.substring(separatorIndex + 1)

			while (value.endsWith('\\')) {
				value = value.substring(0, value.length - 1) + '\n' + parser.readLine()
			}
			map[key] = removeBackslashes(value)
		}
		return map
	}
}

/**
 *
 * This will iterate over the [com.acornui.core.UserInfo.currentLocale] list, returning the first locale that is
 * contained in the provided [supported] list. Or null if there is no match.
 */
fun chooseLocale(supported: List<Locale>): Locale? {
	return userInfo.currentLocale.value.firstOrNull2 { it: Locale -> supported.contains(it) }
}


/**
 * An object representing a locale key.
 * In the future, this may be parsed into lang, region, variant, etc.
 *
 * @param value The locale key. E.g. en-US
 */
data class Locale(val value: String)