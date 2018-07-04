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
import com.acornui.graphics.ColorRo
import com.acornui.io.ReadBuffer
import com.acornui.io.WriteBuffer
import com.acornui.math.Vector3Ro

abstract class VertexAttributes(
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

	init {
		_assert(attributes.size <= 16, "A shader program may not contain more than 16 vertex attribute objects.")
		var offset = 0
		for (i in 0..attributes.lastIndex) {
			offset += attributes[i].size
		}
		stride = offset
		vertexSize = offset shr 2
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

	abstract fun getVertex(vertexData: ReadBuffer<Float>, out: Vertex)
	abstract fun putVertex(vertexData: WriteBuffer<Float>, position: Vector3Ro, normal: Vector3Ro, colorTint: ColorRo, u: Float, v: Float)
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
			Gl20.FLOAT, Gl20.INT, Gl20.UNSIGNED_INT -> {
				4
			}
			Gl20.SHORT, Gl20.UNSIGNED_SHORT -> {
				2
			}
			Gl20.BYTE, Gl20.UNSIGNED_BYTE -> {
				1
			}
			else -> throw Exception("Unknown attribute type.")
		}
		size = componentSize * numComponents
	}
}

object VertexAttributeUsage {
	const val POSITION = 0
	const val NORMAL = 1
	const val COLOR_TINT = 2
	const val TEXTURE_COORD = 3
}