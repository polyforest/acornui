package com.acornui.graphic

import com.acornui.recycle.Clearable
import kotlinx.serialization.*
import kotlinx.serialization.internal.FloatSerializer
import kotlinx.serialization.internal.StringDescriptor
import kotlin.math.abs

@Serializable(with = HsvSerializer::class)
interface HsvRo {
	val h: Float
	val s: Float
	val v: Float
	val a: Float

	fun copy(): Hsv {
		return Hsv().set(this)
	}

	fun toRgb(out: Color = Color()): Color {
		out.a = a
		val c = v * s
		val x = c * (1f - abs((h / 60f) % 2f - 1f))
		val m = v - c
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
}

/**
 * Hue saturation value
 */
@Serializable(with = HsvSerializer::class)
class Hsv(
		override var h: Float = 0f,
		override var s: Float = 0f,
		override var v: Float = 0f,
		override var a: Float = 1f
) : HsvRo, Clearable {

	override fun clear() {
		h = 0f
		s = 0f
		v = 0f
		a = 0f
	}

	fun set(other: HsvRo): Hsv {
		h = other.h
		s = other.s
		v = other.v
		a = other.a
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null) return false
		other as HsvRo

		if (h != other.h) return false
		if (s != other.s) return false
		if (v != other.v) return false
		if (a != other.a) return false

		return true
	}

	override fun hashCode(): Int {
		var result = h.hashCode()
		result = 31 * result + s.hashCode()
		result = 31 * result + v.hashCode()
		result = 31 * result + a.hashCode()
		return result
	}
}

@Serializer(forClass = Hsv::class)
object HsvSerializer : KSerializer<Hsv> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("Hsv")
	
	override fun serialize(encoder: Encoder, obj: Hsv) {
		encoder.encodeSerializableValue(FloatSerializer.list, listOf(obj.h, obj.s, obj.v, obj.a))
	}

	override fun deserialize(decoder: Decoder): Hsv {
		val values = decoder.decodeSerializableValue(FloatSerializer.list)
		return Hsv(values[0], values[1], values[2], values[3])
	}
}