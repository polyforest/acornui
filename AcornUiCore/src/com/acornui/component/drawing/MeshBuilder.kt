package com.acornui.component.drawing

import com.acornui.collection.Clearable
import com.acornui.collection.ObjectPool
import com.acornui.gl.core.*
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*

/**
 * A MeshRegion provides utility for writing to a mesh and optionally transforming that section of the mesh.
 */
class MeshRegion private constructor() : Clearable, ShaderBatch {

	private var _startVertexPosition = 0
	private var _startIndexPosition = 0
	private var _startDrawCallIndex = 0

	/**
	 * Returns the vertex component buffer position where this region begins.
	 */
	val startVertexPosition: Int
		get() = _startVertexPosition

	/**
	 * Returns the index buffer position where this region begins.
	 */
	val startIndexPosition: Int
		get() = _startIndexPosition

	/**
	 * Returns the draw call list index where this region begins.
	 */
	val startDrawCallIndex: Int
		get() = _startDrawCallIndex

	private var _batch: StaticShaderBatch? = null
	val batch: StaticShaderBatch
		get() = _batch ?: throw Exception("batch not set; this mesh region was not initialized.")

	fun init(batch: StaticShaderBatch) {
		this._batch = batch
		_startVertexPosition = batch.vertexComponents.position
		_startIndexPosition = batch.indices.position
		_startDrawCallIndex = batch.drawCalls.size
	}

	override val highestIndex: Short
		get() = batch.highestIndex

	override fun resetRenderCount() = batch.resetRenderCount()

	override val renderCount: Int
		get() = batch.renderCount

	override fun begin(drawMode: Int) = batch.begin(drawMode)

	override fun flush(force: Boolean) = batch.flush(force)

	override fun putVertex(positionX: Float, positionY: Float, positionZ: Float, normalX: Float, normalY: Float, normalZ: Float, colorR: Float, colorG: Float, colorB: Float, colorA: Float, u: Float, v: Float) = batch.putVertex(positionX, positionY, positionZ, normalX, normalY, normalZ, colorR, colorG, colorB, colorA, u, v)

	override val vertexAttributes: VertexAttributes
		get() = batch.vertexAttributes

	override fun putIndex(index: Short) = batch.putIndex(index)

	override fun putVertexComponent(value: Float) = batch.putVertexComponent(value)

	/**
	 * Transform the vertices by the given matrix, and the normals by the inverse-transpose of that matrix.
	 */
	fun transform(value: Matrix4Ro) {
		transformVertices(value)
		tmpMat.set(value).setTranslation(0f, 0f, 0f).inv().tra()
		transformNormals(tmpMat)
	}

	fun transformVertices(value: Matrix4Ro) {
		iterateVector3Attribute(VertexAttributeUsage.POSITION) {
			value.prj(it)
		}
	}

	private inline fun iterateVector3Attribute(usage: Int, inner: (v: Vector3) -> Unit) {
		batch.iterateVertexAttribute(usage, _startVertexPosition) {
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

	/**
	 * Multiplies the normals by the given matrix.
	 */
	fun transformNormals(value: Matrix4Ro) {
		iterateVector3Attribute(VertexAttributeUsage.NORMAL) {
			value.rot(it).nor()
		}
	}

	/**
	 * Translate the vertices by the given deltas.
	 */
	fun trn(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
		iterateVector3Attribute(VertexAttributeUsage.POSITION) {
			it.add(x, y, z)
		}
	}

	/**
	 * Scales the vertices by the given multipliers.
	 */
	fun scl(x: Float = 1f, y: Float = 1f, z: Float = 1f) {
		if (x == y && x == z) {
			// No need to manipulate the normals.
			iterateVector3Attribute(VertexAttributeUsage.POSITION) {
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
		batch.iterateVertexAttribute(VertexAttributeUsage.COLOR_TINT, _startVertexPosition) {
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

	override fun clear() {
		_batch = null
	}

	companion object {
		private val tmpMat = Matrix4()
		private val tmpVec = Vector3()
		private val tmpColor = Color()

		private val pool = ObjectPool { MeshRegion() }

		fun obtain(batch: StaticShaderBatch): MeshRegion {
			val region = pool.obtain()
			region.init(batch)
			return region
		}

		fun free(region: MeshRegion) {
			pool.free(region)
		}
	}
}

private val mat = Matrix4()
private val quat = Quaternion()

fun MeshRegion.rotate(yaw: Float = 0f, pitch: Float = 0f, roll: Float = 0f) {
	quat.setEulerAnglesRad(yaw, pitch, roll)
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
		quat.setEulerAnglesRad(rotation.y, rotation.x, rotation.z)
		mat.rotate(quat)
	}
	if (!origin.isZero())
		mat.translate(-origin.x, -origin.y, -origin.z)
	transform(mat)
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

		/**
		 * The color of the line.
		 */
		var colorTint: ColorRo = Color.WHITE
) : Clearable {

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
	fun createCap(p1: Vector2Ro, p2: Vector2Ro, control: Vector2Ro?, meshRegion: MeshRegion, lineStyle: LineStyle, controlLineThickness: Float, clockwise: Boolean)
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


fun meshData(batch: StaticShaderBatch, init: MeshRegion.() -> Unit = {}) {
	val p = MeshRegion.obtain(batch)
	p.begin()
	p.init()
	MeshRegion.free(p)
}

fun MeshRegion.meshData(init: MeshRegion.() -> Unit = {}) {
	val p = MeshRegion.obtain(batch)
	p.begin()
	p.init()
	MeshRegion.free(p)
}