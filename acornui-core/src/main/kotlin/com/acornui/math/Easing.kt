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

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.acornui.math

import com.acornui.collection.getInsertionIndex
import com.acornui.collection.scl
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


/**
 * Takes a linear value in the range of 0-1 and outputs an interpolated value.
 * If using an interpolation with serialization, the interpolation must be registered with [Easing].
 */
@Serializable(with = InterpolationSerializer::class)
interface Interpolation {

	/**
	 * @param alpha Between 0.0 and 1.0
	 * @return The interpolated value.
	 */
	fun apply(alpha: Double): Double

	/**
	 * Calculates the interpolation and applies that to the range [start, end]
	 * @param start
	 * @param end
	 * @param alpha Alpha value between 0.0 and 1.0.
	 */
	fun apply(start: Double, end: Double, alpha: Double): Double {
		return start + (end - start) * apply(alpha)
	}
}

class Constant(private val value: Double) : Interpolation {

	override fun apply(alpha: Double): Double {
		return value
	}
}

class Pow(private val power: Int) : Interpolation {

	override fun apply(alpha: Double): Double {
		if (alpha <= 0.5) return (alpha * 2.0).pow(power.toDouble()) * 0.5
		return ((alpha - 1.0) * 2.0).pow(power.toDouble()) / (if (power % 2 == 0) -2.0 else 2.0) + 1.0
	}
}

class PowIn(private val power: Int) : Interpolation {

	override fun apply(alpha: Double): Double {
		return alpha.pow(power.toDouble())
	}
}

class PowOut(private val power: Int) : Interpolation {

	override fun apply(alpha: Double): Double {
		return (alpha - 1.0).pow(power.toDouble()) * (if (power % 2 == 0) -1.0 else 1.0) + 1.0
	}
}

//

open class Exp(val value: Double, val power: Double) : Interpolation {

	val min: Double = value.pow(-power)
	val scale: Double

	init {
		scale = 1.0 / (1.0 - min)
	}

	override fun apply(alpha: Double): Double {
		if (alpha <= 0.5) return (value.pow(power * (alpha * 2.0 - 1.0)) - min) * scale * 0.5
		return (2.0 - (value.pow(-power * (alpha * 2.0 - 1.0)) - min) * scale) * 0.5
	}
}

class ExpIn(value: Double, power: Double) : Exp(value, power) {

	override fun apply(alpha: Double): Double {
		return (value.pow(power * (alpha - 1.0)) - min) * scale
	}
}

class ExpOut(value: Double, power: Double) : Exp(value, power) {

	override fun apply(alpha: Double): Double {
		return 1.0 - (value.pow((-power * alpha)) - min) * scale
	}
}

//

open class Elastic(val value: Double, val power: Double, bounces: Int, val scale: Double) : Interpolation {

	val bounces: Double = bounces * PI * (if (bounces % 2 == 0) 1.0 else -1.0)

	override fun apply(alpha: Double): Double {
		var a = alpha
		if (a <= 0.5) {
			a *= 2.0
			return value.pow((power * (a - 1.0))) * sin(a * bounces) * scale * 0.5
		}
		a = 1.0 - a
		a *= 2.0
		return 1.0 - value.pow((power * (a - 1.0))) * sin((a) * bounces) * scale * 0.5
	}
}

// TODO: broken!
class ElasticIn(value: Double, power: Double, bounces: Int, scale: Double) : Elastic(value, power, bounces, scale) {

	override fun apply(alpha: Double): Double {
		return value.pow((power * (alpha - 1.0))) * sin(alpha * bounces) * scale
	}
}

class ElasticOut(value: Double, power: Double, bounces: Int, scale: Double) : Elastic(value, power, bounces, scale) {

	override fun apply(alpha: Double): Double {
		var a = alpha
		a = 1.0 - a
		return (1.0 - value.pow((power * (a - 1.0))) * sin(a * bounces) * scale)
	}
}

open class Swing(scale: Double) : Interpolation {

	private val scale: Double = scale * 2.0

	override fun apply(alpha: Double): Double {
		var a = alpha
		if (a <= 0.5) {
			a *= 2.0
			return a * a * ((scale + 1.0) * a - scale) * 0.5
		}
		a--
		a *= 2.0
		return a * a * ((scale + 1.0) * a + scale) * 0.5 + 1.0
	}
}

open class SwingOut(private val scale: Double) : Interpolation {

	override fun apply(alpha: Double): Double {
		var a = alpha
		a--
		return a * a * ((scale + 1.0) * a + scale) + 1.0
	}
}

open class SwingIn(private val scale: Double) : Interpolation {

	override fun apply(alpha: Double): Double {
		return alpha * alpha * ((scale + 1.0) * alpha - scale)
	}
}

//--------------------------------------------
// Interpolation without configuration
//--------------------------------------------

/**
 * Jumps from 0.0 to 1.0 without interpolation.
 */
object Stepped : Interpolation {
	override fun apply(alpha: Double): Double {
		return if (alpha == 1.0) 1.0 else 0.0
	}
}

object Linear : Interpolation {
	override fun apply(alpha: Double): Double {
		return alpha
	}
}

object Fade : Interpolation {
	override fun apply(alpha: Double): Double {
		return com.acornui.math.clamp(alpha * alpha * alpha * (alpha * (alpha * 6.0 - 15.0) + 10.0), 0.0, 1.0)
	}
}

object Sine : Interpolation {
	override fun apply(alpha: Double): Double {
		return (1.0 - cos(alpha * PI)) * 0.5
	}
}

object SineIn : Interpolation {
	override fun apply(alpha: Double): Double {
		return 1.0 - cos(alpha * PI * 0.5)
	}
}

object SineOut : Interpolation {
	override fun apply(alpha: Double): Double {
		return sin(alpha * PI * 0.5)
	}
}

object Circle : Interpolation {
	override fun apply(alpha: Double): Double {
		var a = com.acornui.math.clamp(alpha, 0.0, 1.0)
		return if (a <= 0.5) {
			a *= 2.0
			(1.0 - sqrt(1.0 - a * a)) * 0.5
		} else {
			a--
			a *= 2.0
			(sqrt(1.0 - a * a) + 1.0) * 0.5
		}
	}
}

object CircleInverse : Interpolation {
	override fun apply(alpha: Double): Double {
		var a = com.acornui.math.clamp(alpha, 0.0, 1.0) * 2.0
		return if (a <= 1.0) {
			a--
			sqrt(1.0 - a * a) * 0.5
		} else {
			a--
			-sqrt(1.0 - a * a) * 0.5 + 1.0
		}
	}
}

object CircleIn : Interpolation {
	override fun apply(alpha: Double): Double {
		val a = com.acornui.math.clamp(alpha, 0.0, 1.0)
		return 1.0 - sqrt(1.0 - a * a)
	}
}

object CircleOut : Interpolation {
	override fun apply(alpha: Double): Double {
		var a = com.acornui.math.clamp(alpha, 0.0, 1.0)
		a--
		return sqrt(1.0 - a * a)
	}
}

object Hermite : Interpolation {

	override fun apply(alpha: Double): Double {
		return alpha * alpha * (3.0 - 2.0 * alpha)
	}
}

//----------------------------------------
// Wrapper classes
//----------------------------------------

class Reverse(val inner: Interpolation) : Interpolation {
	override fun apply(alpha: Double): Double {
		return 1.0 - inner.apply(alpha)
	}
}

class ToFro(val inner: Interpolation, val split: Double = 0.5) : Interpolation {
	override fun apply(alpha: Double): Double {
		return if (alpha < split) {
			inner.apply(alpha / split)
		} else {
			inner.apply(1.0 - (alpha - split) / (1.0 - split))
		}
	}
}

/**
 * Applies an inner interpolation, [repetitions] times forward, then reversed.
 * This is equivalent to using [Repeat] with inner [ToFro]
 */
class YoYo(
		val inner: Interpolation,

		/**
		 * The number of times [inner] should repeat between alpha 0-1
		 */
		val repetitions: Double = 1.0
) : Interpolation {
	override fun apply(alpha: Double): Double {
		val a = 2 * alpha * repetitions
		val b = a.toInt()
		return if (b % 2 == 0) {
			inner.apply(a - b)
		} else {
			inner.apply(1.0 - (a - b))
		}
	}
}

/**
 * Repeats the inner interpolation [repetitions] times.
 */
class Repeat(val inner: Interpolation, val repetitions: Double = 1.0) : Interpolation {
	override fun apply(alpha: Double): Double {
		if (alpha >= 1.0) return inner.apply(1.0)
		val a = alpha * repetitions
		val b = a.toInt()
		return inner.apply(a - b)
	}
}

object BasicBounce : Interpolation {
	override fun apply(alpha: Double): Double {
		var a = alpha
		return when {
			a < 1.0 / 2.75 -> {
				7.5625 * a * a
			}
			a < 2.0 / 2.75 -> {
				a -= 1.5 / 2.75
				7.5625 * a * a + 0.75
			}
			a < 2.5 / 2.75 -> {
				a -= 2.25 / 2.75
				7.5625 * a * a + 0.9375
			}
			else -> {
				a -= 2.625 / 2.75
				7.5625 * a * a + 0.984375
			}
		}
	}
}

/**
 * Parabolic arcs from 0 to [restitution]^i and back, [bounces] number of times.
 */
class BounceInPlace(
		val bounces: Int = 4,
		val restitution: Double = 0.2
) : Interpolation {

	private val decays: DoubleArray
	private val intervals: DoubleArray

	init {
		if (bounces < 1 || bounces > 20) throw Exception("repetitions must be between 1 and 20")
		var r = 1.0
		decays = DoubleArray(bounces) { val prev = r; r *= restitution; prev }

		intervals = DoubleArray(bounces) { sqrt(decays[it]) }
		intervals.scl(1.0 / intervals.sum())
	}

	override fun apply(alpha: Double): Double {
		if (alpha >= 1.0 || alpha <= 0.0) return 0.0

		var currBounce = 0
		var nextAlpha = 0.0
		while (alpha >= nextAlpha && currBounce < bounces) {
			nextAlpha += intervals[currBounce++]
		}
		val decay = decays[currBounce - 1]
		val interval = intervals[currBounce - 1]
		val a = (alpha - (nextAlpha - interval)) / interval

		val b = (2 * a - 1.0)

		val v = decay * (1.0 - b * b)
		return v
	}
}

/**
 * Clamps the inner interpolation to start at [startAlpha] and end at [endAlpha].
 */
class Clamp(val inner: Interpolation, val startAlpha: Double = 0.0, val endAlpha: Double = 1.0) : Interpolation {

	override fun apply(alpha: Double): Double {
		if (alpha <= startAlpha) return 0.0
		if (alpha >= endAlpha) return 1.0
		return inner.apply((alpha - startAlpha) / (endAlpha - startAlpha))
	}

	companion object {

		/**
		 * A timed delay of [delay] seconds.
		 */
		fun delay(innerDuration: Double, inner: Interpolation, delay: Double): Interpolation {
			return if (delay <= 0.0) inner
			else Clamp(inner, delay / (innerDuration + delay))
		}

		/**
		 * A clamp by seconds instead of alpha values.
		 */
		fun clamp(innerDuration: Double, inner: Interpolation, delayStart: Double, delayEnd: Double): Interpolation {
			if (delayStart <= 0.0 && delayEnd <= 0.0) return inner
			val d = innerDuration + delayStart + delayEnd
			return Clamp(inner, delayStart / d, (d - delayEnd) / d)
		}
	}
}

class Bezier(
		private val points: DoubleArray
) : Interpolation {

	init {
		check(points.size >= 6) { "Invalid Bezier path." }
	}

	override fun apply(alpha: Double): Double {
		if (alpha <= 0.0) return 0.0
		if (alpha >= 1.0) return 1.0
		val segmentIndex = points.getInsertionIndex(alpha, stride = 6) - 6
		return getY(segmentIndex, alpha)
	}

	/**
	 * After finding the correct bezier segment, calculate the cubic equation and return the easing value.
	 */
	private fun getY(index: Int, x: Double): Double {
		val points = points
		val aX = points[index]
		val aY = points[index + 1]
		val bX = points[index + 2]
		val bY = points[index + 3]
		val cX = points[index + 4]
		val cY = points[index + 5]
		val dX = points[index + 6]
		val dY = points[index + 7]

		val coeffA: Double = -aX + 3.0 * bX - 3.0 * cX + dX
		val coeffB: Double = 3.0 * aX - 6.0 * bX + 3.0 * cX
		val coeffC: Double = -3.0 * aX + 3.0 * bX
		val coeffD: Double = aX
		
		if (aX < dX) {
			if (x <= aX + 0.001) return aY
			if (x >= dX - 0.001) return dY
		} else {
			if (x >= aX + 0.001) return aY
			if (x <= dX - 0.001) return dY
		}

		com.acornui.math.getCubicRoots(coeffA, coeffB, coeffC, coeffD - x, roots)
		var time: Double? = null
		if (roots.isEmpty())
			time = 0.0
		else if (roots.size == 1)
			time = roots[0]
		else {
			for (i in 0..roots.lastIndex) {
				val root = roots[i]
				if (root > -0.01 && root < 1.01) {
					time = root
					break
				}
			}
		}

		if (time == null)
			return 0.0 // Cubic root within range not found.

		return getSingleValue(time, aY, bY, cY, dY)
	}

	private fun getSingleValue(t: Double, a: Double, b: Double, c: Double, d: Double): Double {
		return (t * t * (d - a) + 3.0 * (1.0 - t) * (t * (c - a) + (1.0 - t) * (b - a))) * t + a
	}
	
	companion object {
		private val roots = arrayListOf<Double>()
	}

}

object Easing {

	val stepped: Interpolation = Stepped

	val linear: Interpolation = Linear

	val fade: Interpolation = Fade

	val pow2: Interpolation = Pow(2)
	val pow2In: Interpolation = PowIn(2)
	val pow2Out: Interpolation = PowOut(2)

	val pow3: Interpolation = Pow(3)
	val pow3In: Interpolation = PowIn(3)
	val pow3Out: Interpolation = PowOut(3)

	val pow4: Interpolation = Pow(4)
	val pow4In: Interpolation = PowIn(4)
	val pow4Out: Interpolation = PowOut(4)

	val pow5: Interpolation = Pow(5)
	val pow5In: Interpolation = PowIn(5)
	val pow5Out: Interpolation = PowOut(5)

	val exp10: Interpolation = Exp(2.0, 10.0)
	val exp10In: Interpolation = ExpIn(2.0, 10.0)
	val exp10Out: Interpolation = ExpOut(2.0, 10.0)

	val exp5: Interpolation = Exp(2.0, 5.0)
	val exp5In: Interpolation = ExpIn(2.0, 5.0)
	val exp5Out: Interpolation = ExpOut(2.0, 5.0)

	val circle: Interpolation = Circle
	val circleInverse: Interpolation = CircleInverse
	val circleIn: Interpolation = CircleIn
	val circleOut: Interpolation = CircleOut

	val sine: Interpolation = Sine
	val sineIn: Interpolation = SineIn
	val sineOut: Interpolation = SineOut

	val elastic: Interpolation = Elastic(2.0, 10.0, 7, 1.0)
//	val elasticIn: Interpolation = ElasticIn(2.0, 10.0, 7, 1.0)
//	val elasticOut: Interpolation = ElasticOut(2.0, 10.0, 7, 1.0)

	val swing: Interpolation = Swing(1.5)
	val swingIn: Interpolation = SwingIn(2.0)
	val swingOut: Interpolation = SwingOut(2.0)

	val hermite: Interpolation = Hermite

	private val registryInternal = mutableMapOf(
			"stepped" to stepped,
			"linear" to linear,

			"fade" to fade,

			"pow2" to pow2,
			"pow2In" to pow2In,
			"pow2Out" to pow2Out,

			"pow3" to pow3,
			"pow3In" to pow3In,
			"pow3Out" to pow3Out,

			"pow4" to pow4,
			"pow4In" to pow4In,
			"pow4Out" to pow4Out,

			"pow5" to pow5,
			"pow5In" to pow5In,
			"pow5Out" to pow5Out,

			"exp10" to exp10,
			"exp10In" to exp10In,
			"exp10Out" to exp10Out,

			"exp5" to exp5,
			"exp5In" to exp5In,
			"exp5Out" to exp5Out,

			"circle" to circle,
			"circleInverse" to circleInverse,
			"circleIn" to circleIn,
			"circleOut" to circleOut,

			"sine" to sine,
			"sineIn" to sineIn,
			"sineOut" to sineOut,

			"elastic" to elastic,
//			"elasticIn" to elasticIn,
//			"elasticOut" to elasticOut,

			"swing" to swing,
			"swingIn" to swingIn,
			"swingOut" to swingOut,

			"hermite" to hermite
	)

	val registry: Map<String, Interpolation> = registryInternal

	/**
	 * Registers a named interpolation object, for use in serialization.
	 */
	fun registerInterpolation(name: String, value: Interpolation) {
		registryInternal[name] = value
	}

	/**
	 * Returns the interpolation object if there was one registered with the given name.
	 */
	fun fromStrOptional(name: String): Interpolation? {
		return registryInternal[name]
	}

	fun fromStr(name: String): Interpolation {
		return registryInternal[name] ?: error("Interpolation \"$name\" not found.")
	}

	/**
	 * Returns the name of the static interpolation value as it was registered via [registerInterpolation].
	 */
	fun toString(value: Interpolation): String? {
		return registryInternal.entries.firstOrNull { it.value === value }?.key
	}

}

@Serializer(forClass = Interpolation::class)
object InterpolationSerializer : KSerializer<Interpolation> {

	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Interpolation", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): Interpolation {
		return Easing.fromStr(decoder.decodeString())
	}

	override fun serialize(encoder: Encoder, value: Interpolation) {
		encoder.encodeString(Easing.toString(value) ?: error("Interpolation was not registered"))
	}
}