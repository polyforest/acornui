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

package com.acornui.math

import com.acornui.recycle.ClearableObjectPool
import com.acornui.recycle.Clearable

/**
 * A read-only interface to a [Bounds] object.
 */
interface BoundsRo : RectangleRo {

	/**
	 * The height until the first line of text's baseline, or the height if there is no text.
	 */
	val baseline: Float

	/**
	 * The baseline + y
	 */
	val baselineY: Float
		get() = baseline + y

	fun copy(width: Float = this.width, height: Float = this.height): Bounds {
		return Bounds(width, height)
	}
}

class Bounds(
		override var x: Float = 0f,
		override var y: Float = 0f,
		override var width: Float = 0f,
		override var height: Float = 0f,
		override var baseline: Float = height
) : Clearable, BoundsRo {

	fun set(v: BoundsRo): Bounds {
		x = v.x
		y = v.y
		width = v.width
		height = v.height
		baseline = v.baseline
		return this
	}

	fun set(width: Float, height: Float, baseline: Float = height): Bounds = set(0f, 0f, width, height, baseline)

	fun set(x: Float, y: Float, width: Float, height: Float, baseline: Float): Bounds {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
		this.baseline = baseline
		return this
	}

	/**
	 * Sets the dimensions to the maximum of the existing or provided dimensions.
	 */
	fun ext(width: Float, height: Float) {
		if (width > this.width) this.width = width
		if (height > this.height) this.height = height
	}

	/**
	 * Sets the dimensions to the maximum of the existing or provided dimensions.
	 */
	fun ext(width: Float, height: Float, baseline: Float) {
		ext(width, height)
		if (baseline > this.baseline) this.baseline = baseline
	}

	override fun isEmpty(): Boolean {
		return width == 0f && height == 0f
	}

	override fun isNotEmpty(): Boolean {
		return !isEmpty()
	}

	override fun clear() {
		x = 0f
		y = 0f
		width = 0f
		height = 0f
		baseline = 0f
	}

	@Deprecated("Use Bounds.free", ReplaceWith("Bounds.free(this)"))
	fun free() {
		pool.free(this)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as BoundsRo
		if (x != other.x) return false
		if (y != other.y) return false
		if (width != other.width) return false
		if (height != other.height) return false
		if (baseline != other.baseline) return false
		return true
	}

	override fun hashCode(): Int {
		var result = width.hashCode()
		result = 31 * result + y.hashCode()
		result = 31 * result + x.hashCode()
		result = 31 * result + height.hashCode()
		result = 31 * result + baseline.hashCode()
		return result
	}

	override fun toString(): String {
		return "Bounds(x=$x, y=$y, width=$width, height=$height, baseline=$baseline)"
	}

	companion object {

		val EMPTY_BOUNDS: BoundsRo = Bounds()

		private val pool = ClearableObjectPool { Bounds() }

		fun obtain(): Bounds = pool.obtain()
		fun free(obj: Bounds) = pool.free(obj)
	}

}
