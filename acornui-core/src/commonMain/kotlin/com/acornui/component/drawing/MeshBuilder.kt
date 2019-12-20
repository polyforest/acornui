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

package com.acornui.component.drawing

import com.acornui.recycle.Clearable
import com.acornui.collection.stringMapOf
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*

/**
 * A MeshRegion provides utility for writing to a mesh and optionally transforming that section of the mesh.
 */
class MeshRegion(val batch: ShaderBatch) : ShaderBatch by batch {

	override val vertexComponentsCount: Int
		get() = batch.vertexComponentsCount
	override val indicesCount: Int
		get() = batch.indicesCount

	/**
	 * The vertex component buffer position where this region begins.
	 */
	var startVertexPosition: Int = batch.vertexComponents.position
		private set

	/**
	 * The index buffer position where this region begins.
	 */
	var startIndexPosition: Int = batch.indices.position
		private set

	/**
	 * The draw call list index where this region begins.
	 */
	var startDrawCallIndex: Int = batch.drawCalls.size
		private set

	/**
	 * Transform the vertices by the given matrix, and the normals by the inverse-transpose of that matrix.
	 */
	fun transform(value: Matrix4Ro) {
		transformVertices(value)
		tmpMat.set(value).setTranslation(0f, 0f, 0f).inv().tra()
		transformNormals(tmpMat)
	}

	fun transformVertices(value: Matrix4Ro) {
		iterateVector3Attribute(VertexAttributeLocation.POSITION) {
			value.prj(it)
		}
	}

	/**
	 * Multiplies the normals by the given matrix.
	 */
	fun transformNormals(value: Matrix4Ro) {
		iterateVector3Attribute(VertexAttributeLocation.NORMAL) {
			value.rot(it).nor()
		}
	}

	/**
	 * Translate the vertices by the given deltas.
	 */
	fun trn(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
		iterateVector3Attribute(VertexAttributeLocation.POSITION) {
			it.add(x, y, z)
		}
	}

	/**
	 * Scales the vertices by the given multipliers.
	 */
	fun scl(x: Float = 1f, y: Float = 1f, z: Float = 1f) {
		if (x == y && x == z) {
			// No need to manipulate the normals.
			iterateVector3Attribute(VertexAttributeLocation.POSITION) {
				it.scl(x, y, z)
			}
		} else {
			mat.idt()
			mat.scl(x, y, z)
			transform(mat)
		}
	}

	/**
	 * Multiply the vertices colorTint property by [colorTint]
	 */
	fun colorTransform(colorTint: ColorRo) {
		val v = batch.vertexComponents
		batch.iterateVertexAttribute(VertexAttributeLocation.COLOR_TINT, this.startVertexPosition) {
			val r = v.get()
			val g = v.get()
			val b = v.get()
			val a = v.get()
			tmpColor.set(r, g, b, a).mul(colorTint)
			v.position -= 4
			v.put(tmpColor.r)
			v.put(tmpColor.g)
			v.put(tmpColor.b)
			v.put(tmpColor.a)
		}
	}

	private inline fun iterateVector3Attribute(usage: Int, inner: (v: Vector3) -> Unit) {
		batch.iterateVertexAttribute(usage, this.startVertexPosition) {
			val x = it.get()
			val y = it.get()
			val z = it.get()
			inner(tmpVec.set(x, y, z))
			it.position -= 3
			it.put(tmpVec.x)
			it.put(tmpVec.y)
			it.put(tmpVec.z)
		}
	}

	companion object {
		private val tmpMat = Matrix4()
		private val tmpVec = Vector3()
		private val tmpColor = Color()
	}
}

private val mat = Matrix4()
private val quat = Quaternion()

fun MeshRegion.rotate(yaw: Float = 0f, pitch: Float = 0f, roll: Float = 0f) {
	quat.setEulerAngles(pitch, yaw, roll)
	mat.set(quat)
	// For just a rotation matrix, we don't need to calculate the inverse-transpose to change the normals.
	transformVertices(mat)
	transformNormals(mat)
}

fun MeshRegion.transform(position: Vector3Ro = Vector3.ZERO, scale: Vector3Ro = Vector3.ONE, rotation: Vector3Ro = Vector3.ZERO, origin: Vector3Ro = Vector3.ZERO) {
	mat.idt()
	mat.trn(position)
	mat.scl(scale)
	if (!rotation.isZero()) {
		quat.setEulerAngles(rotation.x, rotation.y, rotation.z)
		mat.rotate(quat)
	}
	if (!origin.isZero())
		mat.translate(-origin.x, -origin.y, -origin.z)
	transform(mat)
}

interface LineStyleRo {

	/**
	 * The style used to draw the caps of joining lines.
	 * @see [CapStyle.CAP_BUILDERS]
	 */
	val capStyle: String

	/**
	 * The thickness of the line.
	 */
	val thickness: Float

	/**
	 * The color of the line.
	 */
	val colorTint: ColorRo

}

data class LineStyle(

		/**
		 * The style used to draw the caps of joining lines.
		 * @see [CapStyle.CAP_BUILDERS]
		 */
		override var capStyle: String = CapStyle.MITER,

		/**
		 * The thickness of the line.
		 */
		override var thickness: Float = 1f,

		/**
		 * The color of the line.
		 */
		override var colorTint: ColorRo = Color.WHITE
) : Clearable, LineStyleRo {

	override fun clear() {
		capStyle = CapStyle.MITER
		thickness = 1f
		colorTint = Color.WHITE
	}
}

/**
 * A registration of mesh builders for supported cap styles.
 *
 * The CAP_BUILDERS map is pre-populated with styles supported by default, but additional cap styles may be added or
 * replaced.
 */
object CapStyle {

	const val NONE: String = "none"
	const val MITER: String = "miter"

	/**
	 * A map of cap styles to their respective mesh builders.
	 */
	private val CAP_BUILDERS = stringMapOf<CapBuilder>()

	fun getCapBuilder(style: String): CapBuilder? {
		return CAP_BUILDERS[style]
	}

	fun setCapBuilder(style: String, builder: CapBuilder) {
		CAP_BUILDERS[style] = builder
	}

	init {
		CAP_BUILDERS[MITER] = MiterCap
		CAP_BUILDERS[NONE] = NoCap
	}
}

/**
 * An interface for building a mesh for a line cap.
 */
interface CapBuilder {

	/**
	 * Creates a cap for a line. The cap must end with two vertices to be used as the endpoints of the p1-p2 line.
	 * The cap should be for the [p1] endpoint.
	 *
	 * @param p1 The first point in the line
	 * @param p2 The second point in the line
	 * @param control The optional previous point before this line.
	 */
	fun createCap(p1: Vector2Ro, p2: Vector2Ro, control: Vector2Ro?, meshRegion: ShaderBatch, lineStyle: LineStyleRo, controlLineThickness: Float, clockwise: Boolean)
}

enum class MeshIntersectionType {

	/**
	 * Only the 3d bounding box is considered for hit detection.
	 */
	BOUNDING_BOX,

	/**
	 * If the 3d bounding box passes, each individual triangle will be tested. (Slower than BOUNDING_BOX, but more
	 * precise.)
	 */
	EXACT
}


fun mesh(batch: ShaderBatch, init: MeshRegion.() -> Unit = {}) {
	val p = MeshRegion(batch)
	p.begin()
	p.init()
}

fun MeshRegion.mesh(init: MeshRegion.() -> Unit = {}) {
	val p = MeshRegion(batch)
	p.begin()
	p.init()
}
