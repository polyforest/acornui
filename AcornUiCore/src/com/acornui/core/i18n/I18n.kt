package com.acornui.core.i18n

import com.acornui._assert
import com.acornui.action.Decorator
import com.acornui.async.then
import com.acornui.collection.find2
import com.acornui.core.*
import com.acornui.core.assets.AssetTypes
import com.acornui.core.assets.load
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2
import com.acornui.string.StringParser

interface I18n {

	val currentLocaleChanged: Signal<(oldLocale: List<Locale>, newLocale: List<Locale>) -> Unit>

	/**
	 * The locale chain. This is an ordered list of locales to try when binding to resource bundles or formatting.
	 */
	var currentLocales: List<Locale>

	/**
	 * Returns a localization bundle for the given bundle name.
	 * This bundle will be for the [currentLocales], and will notify changes when [currentLocaleChanged] has dispatched.
	 */
	fun getBundle(bundleName: String): I18nBundle

	/**
	 * Returns a localization bundle for the given bundle name and locales.
	 * Use this if you wish to get localization values for a specific locale chain, as opposed to the current locale.
	 * @param locales A prioritized list of the locales to which the bundle should be bound.
	 */
	fun getBundle(locales: List<Locale>, bundleName: String): I18nBundle

	/**
	 * Sets the bundle values. This will trigger [I18nBundle.changed] signals on the bundles with the same name.
	 */
	fun setBundleValues(locale: Locale, bundleName: String, values: Map<String, String>)

	companion object : DKey<I18n> {

		override fun factory(injector: Injector): I18n? {
			val i18n = I18nImpl()
			i18n.currentLocales = userInfo.languages
			return i18n
		}

		val UNDEFINED: Locale = Locale("und")
	}
}

interface I18nBundle {

	/**
	 * Dispatched when the values of this bundle has changed. This may be after [I18n.currentLocales] has changed, or
	 * [I18n.setBundleValues] has been called, providing values for this bundle and locale.
	 */
	val changed: Signal<(I18nBundle) -> Unit>

	/**
	 * Retrieves the i18n string with the given key for this bundle and current locale.
	 * This should typically only be called within an [i18n] callback.
	 */
	operator fun get(key: String): String?

}

class I18nImpl : I18n, Disposable {

	private val currentKey = emptyList<Locale>()

	/**
	 * Locale Key -> Bundle Name -> Property Key -> Value
	 */
	private val _bundleValues = HashMap<Locale, MutableMap<String, Map<String, String>>>()
	private val _bundles = HashMap<List<Locale>, MutableMap<String, I18nBundleImpl>>()

	private val _currentLocaleChanged = Signal2<List<Locale>, List<Locale>>()
	override val currentLocaleChanged: Signal<(oldLocale: List<Locale>, newLocale: List<Locale>) -> Unit>
		get() = _currentLocaleChanged

	private var _currentLocale: List<Locale> = listOf()
	override var currentLocales: List<Locale>
		get() = _currentLocale
		set(value) {
			if (_currentLocale == value) return
			_assert(value.isNotEmpty(), "currentLocales cannot be empty.")
			val oldLocale = _currentLocale
			_currentLocale = value
			val defaultBundles = _bundles[currentKey]!!
			for (bundle in defaultBundles.values) {
				bundle.notifyChanged()
			}
			_currentLocaleChanged.dispatch(oldLocale, value)
		}

	init {
		_bundles[currentKey] = HashMap()
	}

	override fun getBundle(bundleName: String): I18nBundle {
		val bundles = _bundles[currentKey]!!
		if (!bundles.contains(bundleName)) {
			bundles[bundleName] = I18nBundleImpl(this, null, bundleName)
		}
		return bundles[bundleName]!!
	}

	override fun getBundle(locales: List<Locale>, bundleName: String): I18nBundle {
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
		return getString(currentLocales, bundleName, key)
	}

	override fun dispose() {
		_currentLocaleChanged.dispose()
	}
}

class I18nBundleImpl(private val i18n: I18nImpl, private val locales: List<Locale>?, private val bundleName: String) : I18nBundle, Disposable {

	private val _changed = Signal1<I18nBundle>()
	override val changed: Signal<(I18nBundle) -> Unit>
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
 * Loads a resource bundle at the given path for the [I18n.currentLocales]. When the current locale changes, the new
 * bundle will be automatically loaded.
 *
 * @param path The path to the properties file to load.
 * This may have the tokens:
 * {locale} which will be replaced by [I18n.currentLocales].
 * {bundleName} which will be replaced by [bundleName].
 * @param defaultPath The path to the default properties to load. The default properties is used as a back-up if the
 * locale isn't found.
 *
 * This method loads the bundle for the current locale, as set by [I18n.currentLocales]. When the current locale changes,
 * a new load will take place.
 *
 * This may be called multiple times safely without re-loading the data.
 *
 */
fun Scoped.loadBundle(bundleName: String, path: String = i18nPath, defaultPath: String = i18nFallbackPath): I18nBundle {
	val i18n = inject(I18n)

	_loadBundle(I18n.UNDEFINED, bundleName, path, defaultPath)
	for (locale in i18n.currentLocales) {
		_loadBundle(locale, bundleName, path, defaultPath)
	}
	i18n.currentLocaleChanged.add {
		oldLocale, newLocale ->
		for (locale in newLocale) {
			_loadBundle(locale, bundleName, path, defaultPath)
		}
	}
	return i18n.getBundle(bundleName)
}

/**
 * Loads a resource bundle for the specified locale and bundle. Unlike [loadBundle], this will not be bound to
 * [I18n.currentLocales].
 */
fun Scoped.loadBundleForLocale(locale: Locale, bundleName: String, path: String = i18nPath): I18nBundle = loadBundleForLocale(listOf(locale), bundleName, path)

/**
 * Loads a resource bundle for the specified locale chain and bundle. Unlike [loadBundle], this will not be bound to
 * [I18n.currentLocales].
 */
fun Scoped.loadBundleForLocale(locales: List<Locale>, bundleName: String, path: String = i18nPath): I18nBundle {
	val i18n = inject(I18n)
	for (locale in locales) {
		val path2 = path.replace2("{locale}", locale.value).replace2("{bundleName}", bundleName)
		load(path2, AssetTypes.TEXT).then {
			i18n.setBundleValues(locale, bundleName, PropertiesDecorator.decorate(it))
		}
	}
	return i18n.getBundle(locales, bundleName)
}

private fun Scoped._loadBundle(locale: Locale, bundleName: String, path: String, fallbackPath: String) {
	val i18n = inject(I18n)
	val path2 = if (locale == I18n.UNDEFINED) {
		fallbackPath.replace2("{bundleName}", bundleName)
	} else {
		path.replace2("{locale}", locale.value).replace2("{bundleName}", bundleName)
	}
	load(path2, AssetTypes.TEXT).then {
		i18n.setBundleValues(locale, bundleName, PropertiesDecorator.decorate(it))
	}
}

object PropertiesDecorator : Decorator<String, Map<String, String>> {

	override fun decorate(target: String): Map<String, String> {
		val map = HashMap<String, String>()
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
 * This will iterate over the [I18n.currentLocales] list, returning the first locale that is contained in
 * the provided [supported] list. Or null if there is no match.
 */
fun Scoped.chooseLocale(supported: List<Locale>): Locale? {
	val i18n = inject(I18n)
	return i18n.currentLocales.find2 { supported.contains(it) }
}


/**
 * An object representing a locale key.
 * In the future, this may be parsed into lang, region, variant, etc.
 *
 * @param value The locale key. E.g. en-US
 */
data class Locale(val value: String)