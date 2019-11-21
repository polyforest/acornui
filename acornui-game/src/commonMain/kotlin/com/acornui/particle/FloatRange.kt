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

package com.acornui.particle

import com.acornui.math.Easing
import com.acornui.math.Interpolation
import com.acornui.serialization.*
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class FloatRange(
		val min: Float = 0f,
		val max: Float = 0f,
		val easing: Interpolation = Easing.linear
) {

	fun getValue(): Float {
		return easing.apply(Random.nextFloat()) * (max - min) + min
	}

	companion object {
		val ZERO = FloatRange()
	}
}

object FloatRangeSerializer : From<FloatRange>, To<FloatRange> {

	override fun read(reader: Reader): FloatRange {
		val easingName = reader.string("easing") ?: "linear"

		return FloatRange(
				min = reader.float("min")!!,
				max = reader.float("max")!!,
				easing = Easing.fromStr(easingName)
		)
	}

	override fun FloatRange.write(writer: Writer) {
		writer.float("min", min)
		writer.float("max", max)
		writer.string("easing", Easing.toString(easing))
	}
}
