package com.acornui.i18n

import com.acornui.asset.cacheSet
import com.acornui.asset.loadText
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.logging.Log
import com.acornui.string.PropertiesParser
import com.acornui.formatters.getMonths
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

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

}

/**
 * An object representing a locale tag.
 * In the future, this may be parsed into language, region, variant, etc.
 *
 * @param value The Unicode locale identifier. E.g. en-US
 */
@Serializable(with = LocaleSerializer::class)
data class Locale(val value: String)

val isI18nSupported: Boolean by lazy {
	val r = try {
		val months = getMonths(longFormat = true, locales = listOf(Locale("es")))
		months.firstOrNull() == "enero"
	} catch (e: Throwable) {
		false
	}
	if (!r) Log.warn("i18n is not supported for this platform.")
	r
}

@Serializer(forClass = Locale::class)
object LocaleSerializer : KSerializer<Locale> {

	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): Locale {
		return Locale(decoder.decodeString())
	}

	override fun serialize(encoder: Encoder, value: Locale) {
		encoder.encodeString(value.value)
	}
}

