package com.acornui.gl.core

import com.acornui.collection.ClearableObjectPool
import com.acornui.collection.Clearable
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.Vector2
import com.acornui.math.Vector3
import com.acornui.math.Vector3Ro

/**
 * A read-only interface to a vertex.
 */
interface VertexRo {
	val position: Vector3Ro
	val normal: Vector3Ro
	val colorTint: ColorRo
	val u: Float
	val v: Float

	fun copy(position: Vector3Ro = this.position, normal: Vector3Ro = this.normal, colorTint: ColorRo = this.colorTint, u: Float = this.u, v: Float = this.v): Vertex {
		return Vertex(position.copy(), normal.copy(), colorTint.copy(), u, v)
	}
}

class Vertex(
		override val position: Vector3 = Vector3(),
		override val normal: Vector3 = Vector3(),
		override val colorTint: Color = Color.WHITE.copy(),
		override var u: Float = 0f,
		override var v: Float = 0f
) : Clearable, VertexRo {

	override fun clear() {
		position.clear()
		normal.clear()
		colorTint.set(Color.WHITE)
		u = 0f
		v = 0f
	}

	fun free() {
		pool.free(this)
	}

	fun set(other: VertexRo): Vertex {
		position.set(other.position)
		normal.set(other.normal)
		colorTint.set(other.colorTint)
		u = other.u
		v = other.v
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true

		other as VertexRo

		if (position != other.position) return false
		if (normal != other.normal) return false
		if (colorTint != other.colorTint) return false
		if (u != other.u) return false
		if (v != other.v) return false

		return true
	}

	override fun hashCode(): Int {
		var result = position.hashCode()
		result = 31 * result + normal.hashCode()
		result = 31 * result + colorTint.hashCode()
		result = 31 * result + u.hashCode()
		result = 31 * result + v.hashCode()
		return result
	}

	companion object {
		private val pool = ClearableObjectPool {
			Vertex()
		}

		fun obtain(): Vertex {
			return pool.obtain()
		}

		fun obtain(copy: VertexRo): Vertex {
			val vertex = pool.obtain()
			vertex.set(copy)
			return vertex
		}

		fun obtain(position: Vector2, normal: Vector3, colorTint: Color, u: Float, v: Float): Vertex {
			val vertex = pool.obtain()
			vertex.position.set(position)
			vertex.normal.set(normal)
			vertex.colorTint.set(colorTint)
			vertex.u = u
			vertex.v = v
			return vertex
		}

		fun obtain(position: Vector3, normal: Vector3, colorTint: Color, u: Float, v: Float): Vertex {
			val vertex = pool.obtain()
			vertex.position.set(position)
			vertex.normal.set(normal)
			vertex.colorTint.set(colorTint)
			vertex.u = u
			vertex.v = v
			return vertex
		}
	}
}

/**
 * A [VertexFeed] provides a way to push vertex information.
 */
interface VertexFeed {

	fun putVertex(position: Vector3Ro, normal: Vector3Ro, colorTint: ColorRo, u: Float, v: Float)

	fun putVertex(vertex: VertexRo) {
		putVertex(vertex.position, vertex.normal, vertex.colorTint, vertex.u, vertex.v)
	}

}

/**
 * An [IndexFeed] provides a way to push indices to an index buffer.
 */
interface IndexFeed {

	/**
	 * Returns the currently highest index pushed.
	 * This will be -1 if no indices have been pushed.
	 */
	val highestIndex: Short

	/**
	 * Sets the index at the current buffer position.
	 */
	fun putIndex(index: Short)

	/**
	 * For convenience, casts the index Int to a Short
	 */
	fun putIndex(index: Int) {
		putIndex(index.toShort())
	}

}


fun IndexFeed.pushQuadIndices() {
	val n = highestIndex + 1
	putIndex(n + 0)
	putIndex(n + 1)
	putIndex(n + 2)
	putIndex(n + 2)
	putIndex(n + 3)
	putIndex(n + 0)
}