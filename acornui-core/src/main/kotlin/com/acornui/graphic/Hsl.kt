package com.acornui.graphic

import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlin.math.abs

/**
 * Hue saturation lightness
 */
@Serializable(with = HslSerializer::class)
data class Hsl(
		val h: Double = 0.0,
		val s: Double = 0.0,
		val l: Double = 0.0,
		val a: Double = 1.0
) {

	fun toRgb(): Color {
		val r: Double
		val g: Double
		val b: Double

		val c = (1.0 - abs(2.0 * l - 1.0)) * s
		val x = c * (1.0 - abs((h / 60.0) % 2.0 - 1.0))
		val m = l - c / 2.0
		when {
			h < 60.0 -> {
				r = c + m
				g = x + m
				b = 0.0 + m
			}
			h < 120.0 -> {
				r = x + m
				g = c + m
				b = 0.0 + m
			}
			h < 180.0 -> {
				r = 0.0 + m
				g = c + m
				b = x + m
			}
			h < 240.0 -> {
				r = 0.0 + m
				g = x + m
				b = c + m
			}
			h < 300.0 -> {
				r = x + m
				g = 0.0 + m
				b = c + m
			}
			else -> {
				r = c + m
				g = 0.0 + m
				b = x + m
			}
		}
		return Color(r, g, b, a)
	}
}

@Serializer(forClass = Hsl::class)
object HslSerializer : KSerializer<Hsl> {

	override val descriptor: SerialDescriptor = PrimitiveDescriptor("Hsl", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: Hsl) {
		encoder.encodeSerializableValue(Double.serializer().list, listOf(value.h, value.s, value.l, value.a))
	}

	override fun deserialize(decoder: Decoder): Hsl {
		val values = decoder.decodeSerializableValue(Double.serializer().list)
		return Hsl(values[0], values[1], values[2], values[3])
	}
}