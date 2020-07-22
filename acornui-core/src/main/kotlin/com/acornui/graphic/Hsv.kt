package com.acornui.graphic

import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlin.math.abs

/**
 * Hue saturation value
 */
@Serializable(with = HsvSerializer::class)
data class Hsv(
		val h: Double = 0.0,
		val s: Double = 0.0,
		val v: Double = 0.0,
		val a: Double = 1.0
) {

	fun toRgb(): Color {
		val r: Double
		val g: Double
		val b: Double
		val c = v * s
		val x = c * (1.0 - abs((h / 60.0) % 2.0 - 1.0))
		val m = v - c
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

@Serializer(forClass = Hsv::class)
object HsvSerializer : KSerializer<Hsv> {

	override val descriptor: SerialDescriptor = PrimitiveDescriptor("Hsv", PrimitiveKind.STRING)
	
	override fun serialize(encoder: Encoder, value: Hsv) {
		encoder.encodeSerializableValue(Double.serializer().list, listOf(value.h, value.s, value.v, value.a))
	}

	override fun deserialize(decoder: Decoder): Hsv {
		val values = decoder.decodeSerializableValue(Double.serializer().list)
		return Hsv(values[0], values[1], values[2], values[3])
	}
}