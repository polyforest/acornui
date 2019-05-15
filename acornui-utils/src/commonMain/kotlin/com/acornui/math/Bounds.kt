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
interface BoundsRo {

	val width: Float

	val height: Float

	/**
	 * The height until the first line of text's baseline, or the height if there is no text.
	 */
	val baseline: Float

	fun isEmpty(): Boolean
	fun isNotEmpty(): Boolean

	fun copy(width: Float = this.width, height: Float = this.height): Bounds {
		return Bounds(width, height)
	}
}

class Bounds(
		override var width: Float = 0f,
		override var height: Float = 0f,
		override var baseline: Float = height
) : Clearable, BoundsRo {

	fun set(v: BoundsRo): Bounds {
		width = v.width
		height = v.height
		baseline = v.baseline
		return this
	}

	fun add(wD: Float, hD: Float): Bounds {
		this.width += wD
		this.height += hD
		return this
	}

	fun set(width: Float, height: Float, baseline: Float = height): Bounds {
		this.width = width
		this.height = height
		this.baseline = baseline
		return this
	}

	fun ext(width: Float, height: Float) {
		if (width > this.width) this.width = width
		if (height > this.height) this.height = height
	}

	fun ext(width: Float, height: Float, baseline: Float) {
		ext(width, height)
		if (baseline > this.baseline) this.baseline = baseline
	}

	override fun isEmpty(): Boolean {
		return width == 0f && height == 0f && baseline == 0f
	}

	override fun isNotEmpty(): Boolean {
		return !isEmpty()
	}

	override fun clear() {
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
		if (width != other.width) return false
		if (height != other.height) return false
		if (baseline != other.baseline) return false
		return true
	}

	override fun hashCode(): Int {
		var result = width.hashCode()
		result = 31 * result + height.hashCode()
		result = 31 * result + baseline.hashCode()
		return result
	}

	override fun toString(): String {
		return "Bounds(width=$width, height=$height, baseline=$baseline)"
	}

	companion object {

		val EMPTY_BOUNDS = Bounds()

		private val pool = ClearableObjectPool { Bounds() }

		fun obtain(): Bounds = pool.obtain()
		fun free(obj: Bounds) = pool.free(obj)
	}

}
