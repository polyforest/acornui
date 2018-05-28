package com.acornui.math

import com.acornui.collection.ClearableObjectPool
import com.acornui.collection.Clearable

/**
 * A read-only interface to a [Bounds] object.
 */
interface BoundsRo {
	val width: Float
	val height: Float
	fun isEmpty(): Boolean
	fun isNotEmpty(): Boolean

	fun copy(width: Float = this.width, height: Float = this.height): Bounds {
		return Bounds(width, height)
	}
}

class Bounds(
		override var width: Float = 0f,
		override var height: Float = 0f
) : Clearable, BoundsRo {

	fun set(v: BoundsRo): Bounds {
		width = v.width
		height = v.height
		return this
	}

	fun add(wD: Float, hD: Float): Bounds {
		this.width += wD
		this.height += hD
		return this
	}

	fun set(width: Float, height: Float): Bounds {
		this.width = width
		this.height = height
		return this
	}

	fun ext(width: Float, height: Float) {
		if (width > this.width) this.width = width
		if (height > this.height) this.height = height
	}

	override fun isEmpty(): Boolean {
		return width == 0f && height == 0f
	}

	override fun isNotEmpty(): Boolean {
		return !isEmpty()
	}

	override fun clear() {
		this.width = 0f
		this.height = 0f
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
		return true
	}

	override fun hashCode(): Int {
		var result = width.hashCode()
		result = 31 * result + height.hashCode()
		return result
	}


	companion object {

		private val pool = ClearableObjectPool { Bounds() }

		fun obtain(): Bounds = pool.obtain()
		fun free(obj: Bounds) = pool.free(obj)
	}

}
