/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.math

import com.acornui.collection.scl


/**
 * Takes a linear value in the range of 0-1 and outputs an interpolated value.
 */
interface Interpolation {

	/**
	 * @param alpha Between 0f and 1f
	 * @return The interpolated value.
	 */
	fun apply(alpha: Float): Float

	/**
	 * Calculates the interpolation and applies that to the range [start, end]
	 * @param start
	 * @param end
	 * @param alpha Alpha value between 0f and 1f.
	 */
	fun apply(start: Float, end: Float, alpha: Float): Float {
		return start + (end - start) * apply(alpha)
	}
}

class Constant(private val value: Float) : Interpolation {

	override fun apply(alpha: Float): Float {
		return value
	}
}

class Pow(private val power: Int) : Interpolation {

	override fun apply(alpha: Float): Float {
		if (alpha <= 0.5f) return MathUtils.pow((alpha * 2f), power.toFloat()) / 2f
		return MathUtils.pow(((alpha - 1f) * 2f), power.toFloat()) / (if (power % 2 == 0) -2f else 2f) + 1f
	}
}

class PowIn(private val power: Int) : Interpolation {

	override fun apply(alpha: Float): Float {
		return MathUtils.pow(alpha, power.toFloat())
	}
}

class PowOut(private val power: Int) : Interpolation {

	override fun apply(alpha: Float): Float {
		return MathUtils.pow((alpha - 1f), power.toFloat()) * (if (power % 2 == 0) -1f else 1f) + 1f
	}
}

//

open class Exp(val value: Float, val power: Float) : Interpolation {

	val min: Float = MathUtils.pow(value, -power)
	val scale: Float

	init {
		scale = 1f / (1f - min)
	}

	override fun apply(alpha: Float): Float {
		if (alpha <= 0.5f) return (MathUtils.pow(value, power * (alpha * 2f - 1f)) - min) * scale / 2f
		return (2f - (MathUtils.pow(value, -power * (alpha * 2f - 1f)) - min) * scale) / 2f
	}
}

class ExpIn(value: Float, power: Float) : Exp(value, power) {

	override fun apply(alpha: Float): Float {
		return (MathUtils.pow(value, power * (alpha - 1f)) - min) * scale
	}
}

class ExpOut(value: Float, power: Float) : Exp(value, power) {

	override fun apply(alpha: Float): Float {
		return 1f - (MathUtils.pow(value, (-power * alpha)) - min) * scale
	}
}

//

open class Elastic(val value: Float, val power: Float, bounces: Int, val scale: Float) : Interpolation {
	val bounces: Float

	init {
		this.bounces = bounces * PI * (if (bounces % 2 == 0) 1f else -1f)
	}

	override fun apply(alpha: Float): Float {
		var a = alpha
		if (a <= 0.5f) {
			a *= 2f
			return MathUtils.pow(value, (power * (a - 1f))) * MathUtils.sin(a * bounces) * scale / 2f
		}
		a = 1f - a
		a *= 2f
		return 1f - MathUtils.pow(value, (power * (a - 1f))) * MathUtils.sin((a) * bounces) * scale / 2f
	}
}

// TODO: broken!
class ElasticIn(value: Float, power: Float, bounces: Int, scale: Float) : Elastic(value, power, bounces, scale) {

	override fun apply(alpha: Float): Float {
		return MathUtils.pow(value, (power * (alpha - 1f))) * MathUtils.sin(alpha * bounces) * scale
	}
}

class ElasticOut(value: Float, power: Float, bounces: Int, scale: Float) : Elastic(value, power, bounces, scale) {

	override fun apply(alpha: Float): Float {
		var a = alpha
		a = 1f - a
		return (1f - MathUtils.pow(value, (power * (a - 1f))) * MathUtils.sin(a * bounces) * scale)
	}
}

open class Swing(scale: Float) : Interpolation {

	private val scale: Float

	init {
		this.scale = scale * 2f
	}

	override fun apply(alpha: Float): Float {
		var a = alpha
		if (a <= 0.5f) {
			a *= 2f
			return a * a * ((scale + 1f) * a - scale) / 2f
		}
		a--
		a *= 2f
		return a * a * ((scale + 1f) * a + scale) / 2f + 1f
	}
}

open class SwingOut(private val scale: Float) : Interpolation {

	override fun apply(alpha: Float): Float {
		var a = alpha
		a--
		return a * a * ((scale + 1f) * a + scale) + 1f
	}
}

open class SwingIn(private val scale: Float) : Interpolation {

	override fun apply(alpha: Float): Float {
		return alpha * alpha * ((scale + 1f) * alpha - scale)
	}
}

//--------------------------------------------
// Interpolation without configuration
//--------------------------------------------

/**
 * Jumps from 0f to 1f without interpolation.
 */
object Stepped : Interpolation {
	override fun apply(alpha: Float): Float {
		return if (alpha == 1f) 1f else 0f
	}
}

object Linear : Interpolation {
	override fun apply(alpha: Float): Float {
		return alpha
	}
}

object Fade : Interpolation {
	override fun apply(alpha: Float): Float {
		return MathUtils.clamp(alpha * alpha * alpha * (alpha * (alpha * 6f - 15f) + 10f), 0f, 1f)
	}
}

object Sine : Interpolation {
	override fun apply(alpha: Float): Float {
		return (1 - MathUtils.cos(alpha * PI)) / 2
	}
}

object SineIn : Interpolation {
	override fun apply(alpha: Float): Float {
		return 1 - MathUtils.cos(alpha * PI / 2)
	}
}

object SineOut : Interpolation {
	override fun apply(alpha: Float): Float {
		return MathUtils.sin(alpha * PI / 2)
	}
}

object Circle : Interpolation {
	override fun apply(alpha: Float): Float {
		var a = MathUtils.clamp(alpha, 0f, 1f)
		if (a <= 0.5f) {
			a *= 2f
			return (1 - MathUtils.sqrt(1 - a * a)) / 2f
		}
		a--
		a *= 2f
		return (MathUtils.sqrt(1f - a * a) + 1f) / 2f
	}
}

object CircleIn : Interpolation {
	override fun apply(alpha: Float): Float {
		val a = MathUtils.clamp(alpha, 0f, 1f)
		return 1 - MathUtils.sqrt((1f - a * a))
	}
}

object CircleOut : Interpolation {
	override fun apply(alpha: Float): Float {
		var a = MathUtils.clamp(alpha, 0f, 1f)
		a--
		return MathUtils.sqrt(1f - a * a)
	}
}

//----------------------------------------
// Wrapper classes
//----------------------------------------

class Reverse(val inner: Interpolation) : Interpolation {
	override fun apply(alpha: Float): Float {
		return 1f - inner.apply(alpha)
	}
}

class ToFro(val inner: Interpolation, val split: Float = 0.5f) : Interpolation {
	override fun apply(alpha: Float): Float {
		if (alpha < split) {
			return inner.apply(alpha / split)
		} else {
			return inner.apply(1f - (alpha - split) / (1f - split))
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
		val repetitions: Float = 1f
) : Interpolation {
	override fun apply(alpha: Float): Float {
		val a = 2 * alpha * repetitions
		val b = a.toInt()
		if (b % 2 == 0) {
			return inner.apply(a - b)
		} else {
			return inner.apply(1f - (a - b))
		}
	}
}

/**
 * Repeats the inner interpolation [repetitions] times.
 */
class Repeat(val inner: Interpolation, val repetitions: Float = 1f) : Interpolation {
	override fun apply(alpha: Float): Float {
		if (alpha >= 1f) return inner.apply(1f)
		val a = alpha * repetitions
		val b = a.toInt()
		return inner.apply(a - b)
	}
}

object BasicBounce : Interpolation {
	override fun apply(alpha: Float): Float {
		var a = alpha
		if (a < 1f / 2.75f) {
			return 7.5625f * a * a
		} else if (a < 2f / 2.75f) {
			a -= 1.5f / 2.75f
			return 7.5625f * a * a + 0.75f
		} else if (a < 2.5f / 2.75f) {
			a -= 2.25f / 2.75f
			return 7.5625f * a * a + 0.9375f
		} else {
			a -= 2.625f / 2.75f
			return 7.5625f * a * a + 0.984375f
		}
	}
}

/**
 * Parabolic arcs from 0 to [restitution]^i and back, [bounces] number of times.
 */
class BounceInPlace(
		val bounces: Int = 4,
		val restitution: Float = 0.2f
) : Interpolation {

	private val decays: FloatArray
	private val intervals: FloatArray

	init {
		if (bounces < 1 || bounces > 20) throw Exception("repetitions must be between 1 and 20")
		var r = 1f
		decays = FloatArray(bounces, { val prev = r; r *= restitution; prev })

		intervals = FloatArray(bounces, { MathUtils.sqrt(decays[it]) })
		intervals.scl(1f / intervals.sum())
	}

	override fun apply(alpha: Float): Float {
		if (alpha >= 1f || alpha <= 0f) return 0f

		var currBounce = 0
		var nextAlpha = 0f
		while (alpha >= nextAlpha && currBounce < bounces) {
			nextAlpha += intervals[currBounce++]
		}
		val decay = decays[currBounce - 1]
		val interval = intervals[currBounce - 1]
		val a = (alpha - (nextAlpha - interval)) / interval

		val b = (2 * a - 1f)

		val v =  decay * (1f - b * b)
		return v
	}
}

/**
 * Clamps the inner interpolation to start at [startAlpha] and end at [endAlpha].
 */
class Clamp(val inner: Interpolation, val startAlpha: Float = 0f, val endAlpha: Float = 1f) : Interpolation {

	override fun apply(alpha: Float): Float {
		if (alpha <= startAlpha) return 0f
		if (alpha >= endAlpha) return 1f
		return inner.apply((alpha - startAlpha) / (endAlpha - startAlpha))
	}

	companion object {

		/**
		 * A timed delay of [delay] seconds.
		 */
		fun delay(innerDuration: Float, inner: Interpolation, delay: Float): Interpolation {
			if (delay <= 0f) return inner
			else return Clamp(inner, delay / (innerDuration + delay))
		}

		/**
		 * A clamp by seconds instead of alpha values.
		 */
		fun clamp(innerDuration: Float, inner: Interpolation, delayStart: Float, delayEnd: Float): Interpolation {
			if (delayStart <= 0f && delayEnd <= 0f) return inner
			val d = innerDuration + delayStart + delayEnd
			return Clamp(inner, delayStart / d, (d - delayEnd) / d)
		}
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

	val exp10: Interpolation = Exp(2f, 10f)
	val exp10In: Interpolation = ExpIn(2f, 10f)
	val exp10Out: Interpolation = ExpOut(2f, 10f)

	val exp5: Interpolation = Exp(2f, 5f)
	val exp5In: Interpolation = ExpIn(2f, 5f)
	val exp5Out: Interpolation = ExpOut(2f, 5f)

	val circle: Interpolation = Circle
	val circleIn: Interpolation = CircleIn
	val circleOut: Interpolation = CircleOut

	val sine: Interpolation = Sine
	val sineIn: Interpolation = SineIn
	val sineOut: Interpolation = SineOut

	val elastic: Interpolation = Elastic(2f, 10f, 7, 1f)
	val elasticIn: Interpolation = ElasticIn(2f, 10f, 7, 1f)
	val elasticOut: Interpolation = ElasticOut(2f, 10f, 7, 1f)

	val swing: Interpolation = Swing(1.5f)
	val swingIn: Interpolation = SwingIn(2f)
	val swingOut: Interpolation = SwingOut(2f)

}