/*
 * Derived from LibGDX by Nicholas Bilyk
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

import com.acornui.core.LONG_MAX_VALUE
import com.acornui.core.LONG_MIN_VALUE

/**
 * This class implements the xorshift128+ algorithm that is a very fast, top-quality 64-bit pseudo-random number
 * generator. The quality of this PRNG is much higher than {@link Random}'s, and its cycle length is
 * 2<sup>128</sup>&nbsp;&minus;&nbsp;1, which is more than enough for any single-thread application. More details and
 * algorithms can be found <a href="http://xorshift.di.unimi.it/">here</a>.
 * <p>
 * Instances of RandomXS128 are not thread-safe.
 *
 * @author Inferno
 * @author davebaol
 */

/**
 * A Random number generator
 *
 * @param seed0 The first half of the internal state of this pseudo-random number generator.
 * @param seed1 The second half of the internal state of this pseudo-random number generator.
 */
open class Random(var seed0: Long = 0, var seed1: Long = 0) {

	init {
		if (seed0 == 0L) {
			setSeed((random() * LONG_MAX_VALUE).toLong())
		} else if (seed1 == 0L) {
			setSeed(seed0)
		}
	}

	companion object {

		/**
		 * Normalization constant for double.
		 */
		private const val NORM_DOUBLE = 1.0 / (1 shl 53).toDouble()

		/**
		 * Normalization constant for float.
		 */
		private const val NORM_FLOAT = 1.0 / (1 shl 24).toDouble()

		private fun murmurHash3(x: Long): Long {
			var xV = x
			xV = xV xor xV.ushr(33)
			xV *= -49064778989728563
			xV = xV xor xV.ushr(33)
			xV *= -4265267296055464877
			xV = xV xor xV.ushr(33)

			return xV
		}
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed {@code long} value from this random number generator's sequence.
	 * Subclasses should override this, as this is used by all other methods.
	 */
	open fun nextLong(): Long {
		var s1 = this.seed0
		val s0 = this.seed1
		this.seed0 = s0
		s1 = s1 xor (s1 shl 23)
		this.seed1 = (s1 xor s0 xor (s1.ushr(17)) xor (s0.ushr(26)))
		return seed1 + s0
	}

	/**
	 * This protected method is final because, contrary to the superclass, it's not used anymore by the other methods.
	 */
	fun next(bits: Int): Int {
		return (nextLong() and ((1 shl bits) - 1).toLong()).toInt()
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed {@code int} value from this random number generator's sequence.
	 * This implementation uses {@link #nextLong()} internally.
	 */
	fun nextInt(): Int {
		return nextLong().toInt()
	}

	/**
	 * Returns a pseudo-random, uniformly distributed {@code int} value between 0 (inclusive) and the specified value (exclusive),
	 * drawn from this random number generator's sequence.
	 * <p>
	 * This implementation uses {@link #nextLong()} internally.
	 * @param n the positive bound on the random number to be returned.
	 * @return the next pseudo-random {@code int} value between {@code 0} (inclusive) and {@code n} (exclusive).
	 */
	fun nextInt(n: Int): Int {
		return nextLong(n.toLong()).toInt()
	}

	/**
	 * Returns a pseudo-random, uniformly distributed {@code long} value between 0 (inclusive) and the specified value
	 * (exclusive), drawn from this random number generator's sequence. The algorithm used to generate the value
	 * guarantees that the result is uniform, provided that the sequence of 64-bit values produced by this generator is.
	 * This implementation uses {@link #nextLong()} internally.
	 * @param n the positive bound on the random number to be returned.
	 * @return the next pseudo-random {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive).
	 */
	fun nextLong(n: Long): Long {
		if (n <= 0) throw IllegalArgumentException("n must be positive")
		while (true) {
			val bits = nextLong().ushr(1)
			val value = bits % n
			if (bits - value + (n - 1) >= 0) return value
		}
	}

	/**
	 * Returns a pseudo-random, uniformly distributed {@code double} value between 0.0 and 1.0from this random number generator's
	 * sequence.
	 *
	 * This implementation uses {@link #nextLong()} internally.
	 */
	fun nextDouble(): Double {
		return (nextLong().ushr(11)).toDouble() * NORM_DOUBLE
	}

	/**
	 * Returns a pseudo-random, uniformly distributed {@code float} value between 0.0 and 1.0 from this random number generator's
	 * sequence.
	 *
	 * This implementation uses {@link #nextLong()} internally.
	 */
	fun nextFloat(): Float {
		return ((nextLong().ushr(40)).toDouble() * NORM_FLOAT).toFloat()
	}

	/**
	 * Returns a pseudo-random, uniformly distributed {@code boolean } value from this random number generator's sequence.
	 * This implementation uses {@link #nextLong()} internally.
	 */
	fun nextBoolean(): Boolean {
		return (nextLong() and 1L) != 0L
	}

	/**
	 * Generates random bytes and places them into a user-supplied byte array. The number of random bytes produced is equal to the
	 * length of the byte array.
	 * This implementation uses {@link #nextLong()} internally.
	 */
	fun nextBytes(bytes: ByteArray) {
		var i = bytes.size
		while (i != 0) {
			var n = minOf(i, 8)
			var bits = nextLong()
			while (n-- != 0) {
				bytes[--i] = bits.toByte()
				bits = bits shr 8
			}
		}
	}

	/**
	 * Sets the internal seed of this generator based on the given {@code long} value.
	 *
	 * The given seed is passed twice through an hash function. This way, if the user passes a small value we avoid the short
	 * irregular transient associated with states having a very small number of bits set.
	 * @param seed a nonzero seed for this generator (if zero, the generator will be seeded with {@link Long#MIN_VALUE}).
	 */
	fun setSeed(seed: Long) {
		val seed0 = murmurHash3(if (seed == 0L) LONG_MIN_VALUE else seed)
		setState(seed0, murmurHash3(seed0))
	}

	/**
	 * Sets the internal state of this generator.
	 * @param seed0 the first part of the internal state
	 * @param seed1 the second part of the internal state
	 */
	fun setState(seed0: Long, seed1: Long) {
		this.seed0 = seed0
		this.seed1 = seed1
	}

}

// TODO - MP: Do all of these need to be expects?
expect fun random(): Double

/**
 * Returns a random number between 0 (inclusive) and the specified value (inclusive).
 */
fun Random.random(range: Int): Int {
	return nextInt(range + 1)
}

/**
 * Returns a random number between start (inclusive) and end (inclusive).
 */
fun Random.random(start: Int, end: Int): Int {
	return start + nextInt(end - start + 1)
}

/**
 * Returns a random number between 0 (inclusive) and the specified value (inclusive).
 */
fun Random.random(range: Long): Long {
	return (nextDouble() * range.toDouble()).toLong()
}

/**
 * Returns a random number between start (inclusive) and end (inclusive).
 */
fun Random.random(start: Long, end: Long): Long {
	return start + (nextDouble() * (end - start).toDouble()).toLong()
}

/**
 * Returns true if a random value between 0 and 1 is less than the specified value.
 */
fun Random.randomBoolean(chance: Float): Boolean {
	return nextFloat() < chance
}

/**
 * Returns a random number between 0 (inclusive) and the specified value (exclusive).
 */
fun Random.random(range: Float): Float {
	return nextFloat() * range
}

/**
 * Returns a random number between start (inclusive) and end (exclusive).
 */
fun Random.random(start: Float, end: Float): Float {
	return start + nextFloat() * (end - start)
}

/**
 * Returns -1 or 1, randomly.
 */
fun Random.randomSign(): Int {
	return 1 or (nextInt() shr 31)
}

/**
 * Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are
 * more likely.
 * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-1, 1, 0)}
 */
fun Random.randomTriangular(): Float {
	return nextFloat() - nextFloat()
}

/**
 * Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive), where values
 * around zero are more likely.
 * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-max, max, 0)}
 * @param max the upper limit
 */
fun Random.randomTriangular(max: Float): Float {
	return (nextFloat() - nextFloat()) * max
}

/**
 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where the
 * `mode` argument defaults to the midpoint between the bounds, giving a symmetric distribution.
 *
 * This method is equivalent of [randomTriangular(min, max, (max - min) * .5f)]
 * @param min the lower limit
 * @param max the upper limit
 */
fun Random.randomTriangular(min: Float, max: Float): Float {
	return randomTriangular(min, max, (max - min) * 0.5f)
}

/**
 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where values
 * around {@code mode} are more likely.
 * @param min the lower limit
 * @param max the upper limit
 * @param mode the point around which the values are more likely
 */
fun Random.randomTriangular(min: Float, max: Float, mode: Float): Float {
	val u = nextFloat()
	val d = max - min
	if (u <= (mode - min) / d) return min + kotlin.math.sqrt(u * d * (mode - min))
	return max - kotlin.math.sqrt((1 - u) * d * (max - mode))
}