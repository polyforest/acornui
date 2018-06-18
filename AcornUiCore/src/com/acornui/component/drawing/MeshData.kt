package com.acornui.component.drawing

import com.acornui.collection.Clearable
import com.acornui.collection.addAll
import com.acornui.core.Parent
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.Vertex
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*


class MeshData : Parent<MeshData>, Clearable {

	/**
	 * If this is true, this node will flush any batched operations before and after a render.
	 */
	var flushBatch = false

	override val children = ArrayList<MeshData>()
	override var parent: MeshData? = null

	var texture: Texture? = null
	var blendMode = BlendMode.NORMAL

	/**
	 * Must be one of [Gl20.POINTS], [Gl20.LINE_STRIP], [Gl20.LINE_LOOP], [Gl20.LINES], [Gl20.TRIANGLE_STRIP],
	 * [Gl20.TRIANGLE_FAN], or [Gl20.TRIANGLES].
	 */
	var drawMode = Gl20.TRIANGLES

	/**
	 * Vertices are added by reference, not value; they do not need to be cloned.
	 */
	val vertices = ArrayList<Vertex>()
	val indices = ArrayList<Int>()

	var highestIndex = -1

	/**
	 * Transform the vertices by the given matrix.
	 */
	fun transform(value: Matrix4) {
		for (i in 0..vertices.lastIndex) {
			val vertex = vertices[i]
			value.prj(vertex.position)
			value.rot(vertex.normal)
		}
		for (i in 0..children.lastIndex) {
			children[i].transform(value)
		}
	}

	/**
	 * Translate the vertices by the given deltas.
	 */
	fun trn(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
		for (i in 0..vertices.lastIndex) {
			val vertex = vertices[i]
			vertex.position.add(x, y, z)
		}
		for (i in 0..children.lastIndex) {
			children[i].trn(x, y, z)
		}
	}

	/**
	 * Scales the vertices by the given multipliers.
	 */
	fun scl(x: Float = 1f, y: Float = 1f, z: Float = 1f) {
		for (i in 0..vertices.lastIndex) {
			val vertex = vertices[i]
			vertex.position.scl(x, y, z)
		}
		for (i in 0..children.lastIndex) {
			children[i].scl(x, y, z)
		}
	}

	/**
	 * Multiply the vertices colorTint property by [colorTint]
	 */
	fun colorTransform(colorTint: ColorRo) {
		for (i in 0..vertices.lastIndex) {
			val vertex = vertices[i]
			vertex.colorTint.mul(colorTint)
		}
		for (i in 0..children.lastIndex) {
			children[i].colorTransform(colorTint)
		}
	}

	/**
	 * Pushes a single index, updating the [highestIndex] property.
	 */
	fun pushIndex(index: Int) {
		indices.add(index)
		if (index > highestIndex) highestIndex = index
	}

	/**
	 * Pushes a set of indices relative to the highest index.
	 */
	fun pushIndices(arr: IntArray) {
		val n: Int = highestIndex + 1
		for (i in 0..arr.lastIndex) {
			pushIndex(arr[i] + n)
		}
	}

	fun pushIndices(arr: List<Int>) {
		val n: Int = highestIndex + 1
		for (i in 0..arr.lastIndex) {
			pushIndex(arr[i] + n)
		}
	}

	fun pushVertex(position: Vector2Ro, fillStyle: FillStyle) {
		tmpUv.set(position).scl(fillStyle.uvDir).add(fillStyle.uvOffset)
		val u = tmpUv.x
		val v = tmpUv.y
		pushVertex(position.x, position.y, 0f, fillStyle.colorTint, u, v)
	}

	fun pushVertex(position: Vector2Ro, z: Float, fillStyle: FillStyle) {
		tmpUv.set(position).scl(fillStyle.uvDir).add(fillStyle.uvOffset)
		val u = tmpUv.x
		val v = tmpUv.y
		pushVertex(position.x, position.y, z, fillStyle.colorTint, u, v)
	}

	fun pushVertex(position: Vector3Ro, fillStyle: FillStyle) {
		tmpUv.set(position.x, position.y).scl(fillStyle.uvDir).add(fillStyle.uvOffset)
		val u = tmpUv.x
		val v = tmpUv.y
		pushVertex(position.x, position.y, position.z, fillStyle.colorTint, u, v)
	}

	fun pushVertex(position: Vector3Ro, fillStyle: FillStyle, normal: Vector3Ro) {
		tmpUv.set(position.x, position.y).scl(fillStyle.uvDir).add(fillStyle.uvOffset)
		val u = tmpUv.x
		val v = tmpUv.y
		pushVertex(position.x, position.y, position.z, fillStyle.colorTint, u, v, normal)
	}

	fun pushVertex(position: Vector3Ro, colorTint: ColorRo, u: Float = 0f, v: Float = 0f, normal: Vector3Ro = Vector3.NEG_Z) {
		pushVertex(position.x, position.y, position.z, colorTint, u, v, normal)
	}

	fun pushVertex(x: Float, y: Float, z: Float, colorTint: ColorRo, u: Float = 0f, v: Float = 0f, normal: Vector3Ro = Vector3.NEG_Z) {
		val localV = Vertex.obtain()
		localV.normal.set(normal)
		localV.position.set(x, y, z)
		localV.colorTint.set(colorTint)
		localV.u = u
		localV.v = v
		vertices.add(localV)
	}

	override fun clear() {
		Vertex.freeAll(vertices)
		vertices.clear()
		indices.clear()
		drawMode = Gl20.TRIANGLES
		blendMode = BlendMode.NORMAL
		highestIndex = -1
		flushBatch = false

		for (i in 0..children.lastIndex) {
			val child = children[i]
			child.parent = null
			child.clear()
		}
		children.clear()
	}

	//--------------------------------------------
	// Children list methods
	//--------------------------------------------
	override fun <S : MeshData> addChild(index: Int, child: S): S {
		if (child == this) throw Exception("Cannot add a child to itself.")
		if (child.parent != null) throw Exception("ChildRo already has a parent.")
		child.parent = this
		children.add(index, child)
		return child
	}

	/**
	 * Syntax sugar for addChild.
	 */
	operator fun <P : MeshData> P.unaryPlus(): P {
		this@MeshData.addChild(this@MeshData.children.size, this)
		return this
	}

	operator fun <P : MeshData> P.unaryMinus(): P {
		this@MeshData.removeChild(this)
		return this
	}

	override fun removeChild(index: Int): MeshData {
		val child = children[index]
		child.parent = null
		children.removeAt(index)
		return child
	}

	fun remove() {
		parent?.removeChild(this)
	}

	/**
	 * Creates a deep copy of this mesh data.
	 */
	fun copy(): MeshData {
		val r = this
		return meshData {
			for (vertex in r.vertices) {
				vertices.add(vertex.copy())
			}
			indices.addAll(r.indices)
			drawMode = r.drawMode
			blendMode = r.blendMode
			highestIndex = r.highestIndex
			flushBatch = r.flushBatch

			for (i in 0..r.children.lastIndex) {
				val child = r.children[i].copy()
				child.parent = this
				children.add(child)
			}
		}
	}

	companion object {
		private val tmpUv = Vector2()
}
}

private val mat = Matrix4()
private val quat = Quaternion()

fun MeshData.rotate(yaw: Float = 0f, pitch: Float = 0f, roll: Float = 0f) {
	mat.idt()
	quat.setEulerAnglesRad(yaw, pitch, roll)
	mat.rotate(quat)
	transform(mat)
}

fun MeshData.transform(position: Vector3Ro = Vector3.ZERO, scale: Vector3Ro = Vector3.ONE, rotation: Vector3Ro = Vector3.ZERO, origin: Vector3Ro = Vector3.ZERO) {
	mat.idt()
	mat.trn(position)
	mat.scl(scale)
	if (!rotation.isZero()) {
		quat.setEulerAnglesRad(rotation.y, rotation.x, rotation.z)
		mat.rotate(quat)
	}
	if (!origin.isZero())
		mat.translate(-origin.x, -origin.y, -origin.z)
	transform(mat)
}

fun meshData(init: MeshData.() -> Unit = {}): MeshData {
	val p = MeshData()
	p.init()
	return p
}

data class FillStyle(

		/**
		 * The colorTint is multiplied against the texture color.
		 */
		val colorTint: Color = Color.WHITE.copy(),

		/**
		 * The u,v coordinates take are calculated by:
		 * uv = position * uvDir + uvOffset
		 */
		val uvDir: Vector2 = Vector2(1f, 1f),
		val uvOffset: Vector2 = Vector2(),
		var texture: Texture? = null,
		var isVisible: Boolean = true
) : Clearable {

	override fun clear() {
		colorTint.set(Color.WHITE)
		uvDir.clear()
		uvOffset.clear()
		texture = null
		isVisible = true
	}

	/**
	 * Returns true if this fill is completely transparent.
	 */
	val isTransparent: Boolean
		get() {
			return colorTint.a <= 0f
		}

}

data class LineStyle(

		/**
		 * The style used to draw the caps of joining lines.
		 * @see [CapStyle.CAP_BUILDERS]
		 */
		var capStyle: String = CapStyle.MITER,

		/**
		 * The thickness of the line.
		 */
		var thickness: Float = 1f,

		var fillStyle: FillStyle = FillStyle()
) : Clearable {

	var isVisible: Boolean
		get() {
			return thickness > 0f && fillStyle.isVisible
		}
		set(value) {
			fillStyle.isVisible = value
		}

	override fun clear() {
		capStyle = CapStyle.MITER
		thickness = 1f
		fillStyle.clear()
	}
}

/**
 * A registration of mesh builders for supported cap styles.
 *
 * The CAP_BUILDERS map is pre-populated with styles supported by default, but additional cap styles may be added or
 * replaced.
 */
object CapStyle {

	val NONE: String = "none"
	val MITER: String = "miter"

	/**
	 * A map of cap styles to their respective mesh builders.
	 */
	private val CAP_BUILDERS: HashMap<String, CapBuilder> = HashMap()

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
	fun createCap(p1: Vector2, p2: Vector2, control: Vector2?, meshData: MeshData, lineStyle: LineStyle, controlLineThickness: Float, clockwise: Boolean)
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

val fillStyle = FillStyle()
val lineStyle = LineStyle()