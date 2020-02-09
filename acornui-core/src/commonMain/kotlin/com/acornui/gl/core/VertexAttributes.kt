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

class VertexAttributes(
		val attributes: List<VertexAttribute>
) {

	/**
	 * The number of bytes between the consecutive attributes. Typically the number of bytes per vertex.
	 */
	val stride: Int

	/**
	 * The number of floats per vertex.
	 */
	val vertexSize: Int

	private val offsetsByUsage: Map<Int, Int>
	private val attributeByUsage: Map<Int, VertexAttribute>

	init {
		check(attributes.size <= 16) { "A shader program may not contain more than 16 vertex attribute objects." }
		val offsetsByUsage = HashMap<Int, Int>()
		val attributeByUsage = HashMap<Int, VertexAttribute>()
		var offset = 0
		for (i in 0..attributes.lastIndex) {
			val attribute = attributes[i]
			require(!attributeByUsage.containsKey(attribute.index)) { "Cannot have two attributes with the same usage." }
			attributeByUsage[attribute.index] = attribute
			offsetsByUsage[attribute.index] = offset shr 2
			offset += attribute.size
		}
		this.offsetsByUsage = offsetsByUsage
		this.attributeByUsage = attributeByUsage
		stride = offset
		vertexSize = offset shr 2
	}

	/**
	 * Returns the offset for the given attribute usage.
	 * This will return null if the attribute usage was not found.
	 */
	fun getOffsetByUsage(usage: Int): Int? {
		return offsetsByUsage[usage]
	}

	/**
	 * Returns the attribute with the given usage.
	 */
	fun getAttributeByUsage(usage: Int): VertexAttribute? {
		return attributeByUsage[usage]
	}

	fun bind(gl: Gl20) {
		var offset = 0
		for (i in 0..attributes.lastIndex) {
			val attribute = attributes[i]
			gl.enableVertexAttribArray(attribute.index)
			gl.vertexAttribPointer(attribute.index, attribute.numComponents, attribute.type, attribute.normalized, stride, offset)
			offset += attribute.size
		}
	}

	fun unbind(gl: Gl20) {
		for (i in 0..attributes.lastIndex) {
			gl.disableVertexAttribArray(attributes[i].index)
		}
	}
}

/**
 * @author nbilyk
 */
data class VertexAttribute(

		/**
		 * The number of components this attribute has.
		 **/
		val numComponents: Int,

		/**
		 * If true and [type] is not [Gl20.FLOAT], the data will be mapped to the range -1 to 1 for signed types and
		 * the range 0 to 1 for unsigned types.
		 */
		val normalized: Boolean,

		/**
		 * The OpenGL type of each component, e.g. [Gl20.FLOAT] or [Gl20.UNSIGNED_BYTE].
		 */
		val type: Int,

		/**
		 * The index location of this attribute.
		 */
		val index: Int
) {

	/**
	 * The number of bytes per component.
	 */
	val componentSize: Int

	/**
	 * The total number of bytes for this vertex component. ([numComponents] * [componentSize])
	 */
	val size: Int

	init {
		require(numComponents >= 1) { "numComponents must be at least 1" }
		require(numComponents <= 4) { "numComponents must be at most 4" }
		componentSize = when (type) {
			Gl20.FLOAT, Gl20.INT, Gl20.UNSIGNED_INT -> 4
			Gl20.SHORT, Gl20.UNSIGNED_SHORT -> 2
			Gl20.BYTE, Gl20.UNSIGNED_BYTE -> 1
			else -> throw Exception("Unknown attribute type.")
		}
		size = componentSize * numComponents
	}
}

/**
 * An enumeration of vertex attribute locations.
 */
object VertexAttributeLocation {
	const val POSITION = 0
	const val NORMAL = 1
	const val COLOR_TINT = 2
	const val TEXTURE_COORD = 3
	const val TANGENT = 4
	const val BITANGENT = 5
}
