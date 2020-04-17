package com.acornui.i18n

import com.acornui.logging.Log
import com.acornui.text.getMonths
import kotlinx.serialization.*

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

	override val descriptor: SerialDescriptor = PrimitiveDescriptor("Locale", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): Locale {
		return Locale(decoder.decodeString())
	}

	override fun serialize(encoder: Encoder, value: Locale) {
		encoder.encodeString(value.value)
	}
}
