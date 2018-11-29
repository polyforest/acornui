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

package com.acornui.gl.core

import com.acornui._assert
import com.acornui.graphic.ColorRo
import com.acornui.math.Vector3Ro

class VertexAttributes(
		val attributes: List<VertexAttribute>
) {

	/**
	 * The number of bytes per vertex.
	 */
	val stride: Int

	/**
	 * The number of floats per vertex.
	 */
	val vertexSize: Int

	private val offsetsByUsage: Map<Int, Int>
	private val attributeByUsage: Map<Int, VertexAttribute>

	init {
		_assert(attributes.size <= 16, "A shader program may not contain more than 16 vertex attribute objects.")
		val offsetsByUsage = HashMap<Int, Int>()
		val attributeByUsage = HashMap<Int, VertexAttribute>()
		var offset = 0
		for (i in 0..attributes.lastIndex) {
			val attribute = attributes[i]
			if (attributeByUsage.containsKey(attribute.usage))
				throw IllegalArgumentException("Cannot have two attributes with the same usage.")
			attributeByUsage[attribute.usage] = attribute
			offsetsByUsage[attribute.usage] = offset shr 2
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

	fun bind(gl: Gl20, shaderProgram: ShaderProgram) {
		var offset = 0
		for (i in 0..attributes.lastIndex) {
			val attribute = attributes[i]
			val attributeLocation = shaderProgram.getAttributeLocationByUsage(attribute.usage)
			if (attributeLocation != -1) {
				gl.enableVertexAttribArray(attributeLocation)
				gl.vertexAttribPointer(attributeLocation, attribute.numComponents, attribute.type, attribute.normalized, stride, offset)
			}
			offset += attribute.size
		}
	}

	fun unbind(gl: Gl20, shaderProgram: ShaderProgram) {
		for (i in 0..attributes.lastIndex) {
			val attribute = attributes[i]
			val attributeLocation = shaderProgram.getAttributeLocationByUsage(attribute.usage)
			if (attributeLocation == -1) continue
			gl.disableVertexAttribArray(attributeLocation)
		}
	}
}

/**
 * An adapter that quickly converts vertex components expected in the [from] attributes, to the order of the vertex
 * components in the attributes required for the given [feed].
 * @param from The attribute order expected.  Attributes that do not exist here but do exist in [feed] attributes, will
 * be written as 0f.
 * @param feed The feed to put components into. The components are only guaranteed to be pushed at the end of every
 * full vertex.
 */
class VertexAttributesAdapter(
		private val from: VertexAttributes,
		private var feed: VertexFeed
) {

	/**
	 * Skip the temporary buffer for attributes that are an exact match.
	 * This marks the attribute index.
	 */
	private var bufferStartIndex = 0

	/**
	 * Skip the temporary buffer for attributes that are an exact match.
	 * This marks the attribute index.
	 */
	private var bufferStartComponentIndex = 0

	init {
		if (from === feed.vertexAttributes) {
			bufferStartIndex = from.attributes.size
			bufferStartComponentIndex = from.vertexSize
		}
		val fromAttribs = from.attributes
		val toAttribs = feed.vertexAttributes.attributes
		for (i in 0..minOf(fromAttribs.lastIndex, toAttribs.lastIndex)) {
			if (fromAttribs[i] == toAttribs[i]) {
				bufferStartIndex++
				bufferStartComponentIndex += fromAttribs[i].numComponents
			}
			else break
		}
	}

	private val buffer = FloatArray(from.vertexSize)
	private var componentIndex = 0

	fun putVertexComponent(value: Float) {
		val vertexAttributes = feed.vertexAttributes
		if (componentIndex >= bufferStartComponentIndex) {
			buffer[componentIndex++] = value
		} else {
			feed.putVertexComponent(value)
			componentIndex++
		}
		if (componentIndex >= from.vertexSize) {
			// Flush vertex
			for (i in bufferStartIndex..vertexAttributes.attributes.lastIndex) {
				val attribute = vertexAttributes.attributes[i]
				val fromOffset = from.getOffsetByUsage(attribute.usage)
				val fromSize = from.getAttributeByUsage(attribute.usage)!!.numComponents
				for (j in 0..attribute.numComponents - 1) {
					if (fromOffset == null || j >= fromSize) {
						feed.putVertexComponent(0f)
					} else {
						feed.putVertexComponent(buffer[fromOffset + j])
					}
				}
			}
			componentIndex = 0
		}
	}
}

/**
 * A wrapper to an attribute adapter that allows pushing position, normal, colorTint, u, and v components to a
 * target vertex feed.
 */
class StandardAttributesAdapter(feed: VertexFeed) {

	private val adapter = VertexAttributesAdapter(standardVertexAttributes, feed)

	fun putVertex(position: Vector3Ro, normal: Vector3Ro, colorTint: ColorRo, u: Float, v: Float) {
		putVertex(position.x, position.y, position.z, normal.x, normal.y, normal.z, colorTint.r, colorTint.g, colorTint.b, colorTint.a, u, v)
	}

	fun putVertex(positionX: Float, positionY: Float, positionZ: Float, normalX: Float, normalY: Float, normalZ: Float, colorR: Float, colorG: Float, colorB: Float, colorA: Float, u: Float, v: Float) {
		adapter.putVertexComponent(positionX)
		adapter.putVertexComponent(positionY)
		adapter.putVertexComponent(positionZ)
		adapter.putVertexComponent(normalX)
		adapter.putVertexComponent(normalY)
		adapter.putVertexComponent(normalZ)
		adapter.putVertexComponent(colorR)
		adapter.putVertexComponent(colorG)
		adapter.putVertexComponent(colorB)
		adapter.putVertexComponent(colorA)
		adapter.putVertexComponent(u)
		adapter.putVertexComponent(v)
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
		 * How the property on the [Vertex] matches to the [ShaderProgram] attribute.
		 */
		val usage: Int
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
		if (numComponents < 1 || numComponents > 4) throw IllegalArgumentException("numComponents must be between 1 and 4")
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
 * An enumeration of vertex attribute usages.
 * Custom vertex attribute usages should start at 16.
 */
object VertexAttributeUsage {
	const val POSITION = 0
	const val NORMAL = 1
	const val COLOR_TINT = 2
	const val TEXTURE_COORD = 3
	const val TANGENT = 4
	const val BITANGENT = 5
}