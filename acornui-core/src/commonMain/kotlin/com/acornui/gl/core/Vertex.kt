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

package com.acornui.gl.core

import com.acornui.recycle.ClearableObjectPool
import com.acornui.recycle.Clearable
import com.acornui.recycle.freeAll
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
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

	@Deprecated("Use Vertex.free", ReplaceWith("Vertex.free(this)"))
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
		if (other == null) return false
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

		fun free(vertex: Vertex) {
			pool.free(vertex)
		}

		fun freeAll(list: List<Vertex>) {
			pool.freeAll(list)
		}
	}
}

fun ShaderBatch.putVertex(vertex: VertexRo) {
	putVertex(
			vertex.position.x,
			vertex.position.y,
			vertex.position.z,
			vertex.normal.x,
			vertex.normal.y,
			vertex.normal.z,
			vertex.colorTint.r,
			vertex.colorTint.g,
			vertex.colorTint.b,
			vertex.colorTint.a,
			vertex.u,
			vertex.v
	)
}
