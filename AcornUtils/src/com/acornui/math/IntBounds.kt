/*
 * Copyright 2018 Nicholas Bilyk
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

import com.acornui.collection.ClearableObjectPool
import com.acornui.collection.Clearable

/**
 * A read-only interface to an [IntBounds] object.
 */
interface IntBoundsRo {
	val width: Int
	val height: Int
	fun isEmpty(): Boolean
	fun isNotEmpty(): Boolean

	fun copy(width: Int = this.width, height: Int = this.height): IntBounds {
		return IntBounds(width, height)
	}
}

class IntBounds(
		override var width: Int = 0,
		override var height: Int = 0
) : Clearable, IntBoundsRo {

	fun set(v: IntBoundsRo): IntBounds {
		width = v.width
		height = v.height
		return this
	}

	fun add(wD: Int, hD: Int): IntBounds {
		this.width += wD
		this.height += hD
		return this
	}

	fun set(width: Int, height: Int): IntBounds {
		this.width = width
		this.height = height
		return this
	}

	fun ext(width: Int, height: Int) {
		if (width > this.width) this.width = width
		if (height > this.height) this.height = height
	}

	override fun isEmpty(): Boolean {
		return width == 0 && height == 0
	}

	override fun isNotEmpty(): Boolean {
		return !isEmpty()
	}

	override fun clear() {
		this.width = 0
		this.height = 0
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as IntBoundsRo
		if (width != other.width) return false
		if (height != other.height) return false
		return true
	}

	override fun hashCode(): Int {
		var result = width.hashCode()
		result = 31 * result + height.hashCode()
		return result
	}

	override fun toString(): String {
		return "IntBounds(width=$width, height=$height)"
	}

	companion object {

		private val pool = ClearableObjectPool { IntBounds() }

		fun obtain(): IntBounds = pool.obtain()
		fun free(obj: IntBounds) = pool.free(obj)
	}

}
