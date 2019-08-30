package com.acornui.graphic

import com.acornui.recycle.Clearable
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.FloatSerializer
import kotlinx.serialization.internal.StringDescriptor
import kotlin.math.abs

/**
 * A read-only representation of an Hsl value.
 */
@Serializable(with = HslSerializer::class)
interface HslRo {
	
	val h: Float
	val s: Float
	val l: Float
	val a: Float

	fun copy(): Hsl {
		return Hsl().set(this)
	}
}

/**
 * Hue saturation lightness
 */
@Serializable(with = HslSerializer::class)
class Hsl(
		override var h: Float = 0f,
		override var s: Float = 0f,
		override var l: Float = 0f,
		override var a: Float = 1f
) : HslRo, Clearable {

	fun toRgb(out: Color): Color {
		out.a = a
		val c = (1f - abs(2f * l - 1f)) * s
		val x = c * (1f - abs((h / 60f) % 2f - 1f))
		val m = l - c / 2f
		if (h < 60f) {
			out.r = c + m
			out.g = x + m
			out.b = 0f + m
		} else if (h < 120f) {
			out.r = x + m
			out.g = c + m
			out.b = 0f + m
		} else if (h < 180f) {
			out.r = 0f + m
			out.g = c + m
			out.b = x + m
		} else if (h < 240f) {
			out.r = 0f + m
			out.g = x + m
			out.b = c + m
		} else if (h < 300f) {
			out.r = x + m
			out.g = 0f + m
			out.b = c + m
		} else {
			out.r = c + m
			out.g = 0f + m
			out.b = x + m
		}
		return out
	}

	override fun clear() {
		h = 0f
		s = 0f
		l = 0f
		a = 0f
	}

	fun set(other: HslRo): Hsl {
		h = other.h
		s = other.s
		l = other.l
		a = other.a
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is HslRo) return false
		if (h != other.h) return false
		if (s != other.s) return false
		if (l != other.l) return false
		if (a != other.a) return false

		return true
	}

	override fun hashCode(): Int {
		var result = h.hashCode()
		result = 31 * result + s.hashCode()
		result = 31 * result + l.hashCode()
		result = 31 * result + a.hashCode()
		return result
	}
}

@Serializer(forClass = Hsl::class)
object HslSerializer : KSerializer<Hsl> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("Hsl")

	override fun serialize(encoder: Encoder, obj: Hsl) {
		encoder.encodeSerializableValue(ArrayListSerializer(FloatSerializer), listOf(obj.h, obj.s, obj.l, obj.a))
	}

	override fun deserialize(decoder: Decoder): Hsl {
		val values = decoder.decodeSerializableValue(ArrayListSerializer(FloatSerializer))
		return Hsl(values[0], values[1], values[2], values[3])
	}
}