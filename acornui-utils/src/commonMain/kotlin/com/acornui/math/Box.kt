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

package com.acornui.math

import com.acornui.recycle.Clearable
import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlin.math.abs

@Serializable(with = BoxSerializer::class)
interface BoxRo : RectangleRo {

	val min: Vector3Ro
	val max: Vector3Ro

	val center: Vector3Ro
	val dimensions: Vector3Ro
	
	override val x: Float
		get() = min.x

	override val y: Float
		get() = min.y

	val z: Float
		get() = min.z

	override val right: Float
		get() = max.x

	override val bottom: Float
		get() = max.y

	val front: Float
		get() = min.z
	
	val back: Float
		get() = max.z

	override val width: Float
		get() = max.x - min.x

	override val height: Float
		get() = max.y - min.y

	val depth: Float
		get() = max.z - min.z

	/**
	 * @param corners A list of 8 Vector3 objects that will be populated with the corners of this bounding box.
	 */
	fun getCorners(corners: List<Vector3>): List<Vector3> {
		val min = min
		val max = max
		corners[0].set(min.x, min.y, min.z)
		corners[1].set(max.x, min.y, min.z)
		corners[2].set(max.x, max.y, min.z)
		corners[3].set(min.x, max.y, min.z)
		corners[4].set(min.x, min.y, max.z)
		corners[5].set(max.x, min.y, max.z)
		corners[6].set(max.x, max.y, max.z)
		corners[7].set(min.x, max.y, max.z)
		return corners
	}

	fun getCorner000(out: Vector3): Vector3 {
		return out.set(min.x, min.y, min.z)
	}

	fun getCorner001(out: Vector3): Vector3 {
		return out.set(min.x, min.y, max.z)
	}

	fun getCorner010(out: Vector3): Vector3 {
		return out.set(min.x, max.y, min.z)
	}

	fun getCorner011(out: Vector3): Vector3 {
		return out.set(min.x, max.y, max.z)
	}

	fun getCorner100(out: Vector3): Vector3 {
		return out.set(max.x, min.y, min.z)
	}

	fun getCorner101(out: Vector3): Vector3 {
		return out.set(max.x, min.y, max.z)
	}

	fun getCorner110(out: Vector3): Vector3 {
		return out.set(max.x, max.y, min.z)
	}

	fun getCorner111(out: Vector3): Vector3 {
		return out.set(max.x, max.y, max.z)
	}

	/**
	 * Returns whether this bounding box is valid. This means that {@link #max} is greater than {@link #min}.
	 * @return True in case the bounding box is valid, false otherwise
	 */
	fun isValid(): Boolean {
		return min.x < max.x && min.y < max.y && min.z < max.z
	}


	/**
	 * Returns whether the given bounding box is intersecting this bounding box (at least one point in).
	 * @param b The bounding box
	 * @return Whether the given bounding box is intersected
	 */
	fun intersects(b: BoxRo): Boolean {
		if (!isValid()) return false

		// test using SAT (separating axis theorem)

		val lX = abs(center.x - b.center.x)
		val sumX = (dimensions.x * 0.5f) + (b.dimensions.x * 0.5f)

		val lY = abs(center.y - b.center.y)
		val sumY = (dimensions.y * 0.5f) + (b.dimensions.y * 0.5f)

		val lZ = abs(center.z - b.center.z)
		val sumZ = (dimensions.z * 0.5f) + (b.dimensions.z * 0.5f)

		return (lX <= sumX && lY <= sumY && lZ <= sumZ)
	}

	/**
	 * Calculates whether or not the given Ray intersects with this Box.
	 *
	 * @param r The ray to project towards this box.
	 * @param out If provided, and if there's an intersection, the location of the intersection will be set on
	 * this vector.
	 * @return Returns true if the ray intersects with this box.
	 */
	override fun intersects(r: RayRo, out: Vector3?): Boolean {
		if (dimensions.x <= 0f || dimensions.y <= 0f) return false
		if (dimensions.z == 0f) {
			// Optimization for a common case is that this box is actually nothing more than a rectangle.
			if (r.direction.z == 0f) return false
			val m = (min.z - r.origin.z) * r.directionInv.z
			if (m < 0) return false // Intersection (if there is one) is behind the ray.
			val x = r.origin.x + m * r.direction.x
			val y = r.origin.y + m * r.direction.y

			val intersects = min.x <= x && max.x >= x && min.y <= y && max.y >= y
			if (out != null && intersects) {
				r.getEndPoint(m, out)
			}
			return intersects
		}

		val d = r.directionInv
		val o = r.origin
		val t1 = (min.x - o.x) * d.x
		val t2 = (max.x - o.x) * d.x
		val t3 = (min.y - o.y) * d.y
		val t4 = (max.y - o.y) * d.y
		val t5 = (min.z - o.z) * d.z
		val t6 = (max.z - o.z) * d.z

		val tMin = maxOf(minOf(t1, t2), minOf(t3, t4), minOf(t5, t6))
		val tMax = minOf(maxOf(t1, t2), maxOf(t3, t4), maxOf(t5, t6))

		// if tmax < 0, ray (line) is intersecting AABB, but whole AABB is behind us
		// if tmin > tmax, ray doesn't intersect AABB
		if (tMax < 0 || tMin > tMax) {
			return false
		}
		if (out != null) {
			r.getEndPoint(tMin, out)
		}
		return true
	}

	/**
	 * Returns whether the given bounding box is contained in this bounding box.
	 * @param b The bounding box
	 * @return Whether the given bounding box is contained
	 */
	fun contains(b: BoxRo): Boolean

	/**
	 * Returns whether the given vector is contained in this bounding box.
	 * @param v The vector
	 * @return Whether the vector is contained or not.
	 */
	fun contains(v: Vector3Ro): Boolean

	fun contains(x: Float, y: Float, z: Float): Boolean

	fun copy(min: Vector3Ro = this.min, max: Vector3Ro = this.max): Box {
		return Box(min.copy(), max.copy())
	}
}

/**
 * Encapsulates an axis aligned bounding box represented by a minimum and a maximum Vector. Additionally you can query for the
 * bounding box's center, dimensions and corner points.
 *
 * @author badlogicgames@gmail.com, Xoppa
 */
@Serializable(with = BoxSerializer::class)
class Box(
		override val min: Vector3 = Vector3(0f),
		override val max: Vector3 = Vector3(0f)
) : BoxRo, Clearable {

	private val _center: Vector3 = Vector3()
	override val center: Vector3Ro
		get() {
			_center.set(min).add(max).scl(0.5f)
			return _center
		}

	private val _dimensions: Vector3 = Vector3()
	override val dimensions: Vector3Ro
		get() {
			_dimensions.set(max).sub(min)
			return _dimensions
		}
	
	/**
	 * Sets the given bounding box.
	 *
	 * @param bounds The bounds.
	 * @return This bounding box for chaining.
	 */
	fun set(bounds: BoxRo): Box {
		return this.set(bounds.min, bounds.max)
	}

	/**
	 * Sets the given minimum and maximum vector.
	 *
	 * @param min The minimum vector
	 * @param max The maximum vector
	 * @return This bounding box for chaining.
	 */
	fun set(min: Vector3Ro, max: Vector3Ro): Box = set(min.x, min.y, min.z, max.x, max.y, max.z)

	/**
	 * Sets the given minimum and maximum vector.
	 *
	 * @return This bounding box for chaining.
	 */
	fun set(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float): Box {
		min.set(minX, minY, minZ)
		max.set(maxX, maxY, maxZ)
		return this
	}

	/**
	 * Sets the bounding box minimum and maximum vector from the given points.
	 *
	 * @param points The points.
	 * @return This bounding box for chaining.
	 */
	fun set(points: Array<Vector3Ro>): Box {
		inf()
		for (i in 0..points.lastIndex)
			ext(points[i])
		return this
	}

	/**
	 * Sets the bounding box minimum and maximum vector from the given points.
	 *
	 * @param points The points.
	 * @return This bounding box for chaining.
	 */
	fun set(points: List<Vector3Ro>): Box {
		inf()
		for (i in 0..points.lastIndex)
			ext(points[i])
		return this
	}

	/**
	 * Sets the minimum and maximum vector to positive and negative infinity.
	 *
	 * @return This bounding box for chaining.
	 */
	fun inf(): Box {
		min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
		max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
		return this
	}

	override fun clear() {
		min.clear()
		max.clear()
	}

	/**
	 * Extends the bounding box to incorporate the given {@link Vector3}.
	 * @param point The vector
	 * @return This bounding box for chaining.
	 */
	fun ext(point: Vector3Ro): Box = ext(point.x, point.y, point.z)

	/**
	 * Extends the bounding box by the given vector.
	 *
	 * @param x The x-coordinate
	 * @param y The y-coordinate
	 * @param z The z-coordinate
	 * @return This bounding box for chaining.
	 */
	fun ext(x: Float, y: Float, z: Float): Box {
		if (x < min.x) min.x = x
		if (y < min.y) min.y = y
		if (z < min.z) min.z = z
		if (x > max.x) max.x = x
		if (y > max.y) max.y = y
		if (z > max.z) max.z = z
		return this
	}
	/**
	 * Extends this bounding box by the given bounding box.
	 *
	 * @param bounds The bounding box
	 * @return This bounding box for chaining.
	 */
	fun ext(bounds: BoxRo): Box {
		return set(
				minOf(x, bounds.x),
				minOf(y, bounds.y),
				minOf(z, bounds.z),
				maxOf(right, bounds.right),
				maxOf(bottom, bounds.bottom),
				maxOf(back, bounds.back)
		)
	}
	
	fun ext(bounds: RectangleRo): Box {
		return set(
				minOf(min.x, bounds.x),
				minOf(min.y, bounds.y),
				min.z,
				maxOf(max.x, bounds.right),
				maxOf(max.y, bounds.bottom),
				max.z
		)
	}

	/**
	 * Extends this bounding box by the given transformed bounding box.
	 *
	 * @param bounds The bounding box
	 * @param transform The transformation matrix to apply to bounds, before using it to extend this bounding box.
	 * @return This bounding box for chaining.
	 */
	fun ext(bounds: BoxRo, transform: Matrix4Ro): Box {
		val v = tmpVec3
		ext(transform.prj(bounds.getCorner000(v)))
		ext(transform.prj(bounds.getCorner100(v)))
		ext(transform.prj(bounds.getCorner110(v)))
		ext(transform.prj(bounds.getCorner010(v)))
		if (bounds.depth != 0f) {
			ext(transform.prj(bounds.getCorner001(v)))
			ext(transform.prj(bounds.getCorner101(v)))
			ext(transform.prj(bounds.getCorner111(v)))
			ext(transform.prj(bounds.getCorner011(v)))
		}
		return this
	}

	/**
	 * Scales this value by the given scalars.
	 */
	fun scl(x: Float, y: Float): Box {
		min.x *= x
		min.y *= y
		max.x *= x
		max.y *= y
		return this
	}

	/**
	 * Scales this value by the given scalars.
	 */
	fun scl(x: Float, y: Float, z: Float): Box {
		min.x *= x
		min.y *= y
		min.z *= z
		max.x *= x
		max.y *= y
		max.z *= z
		return this
	}

	/**
	 * Multiplies the bounding box by the given matrix. This is achieved by multiplying the 8 corner points and then calculating
	 * the minimum and maximum vectors from the transformed points.
	 *
	 * @param transform The matrix
	 * @return This bounding box for chaining.
	 */
	fun mul(transform: Matrix4Ro): Box {
		val x0 = min.x
		val y0 = min.y
		val z0 = min.z
		val x1 = max.x
		val y1 = max.y
		val z1 = max.z
		inf()
		ext(transform.prj(tmpVec3.set(x0, y0, z0)))
		ext(transform.prj(tmpVec3.set(x1, y0, z0)))
		ext(transform.prj(tmpVec3.set(x1, y1, z0)))
		ext(transform.prj(tmpVec3.set(x0, y1, z0)))
		ext(transform.prj(tmpVec3.set(x0, y0, z1)))
		ext(transform.prj(tmpVec3.set(x1, y0, z1)))
		ext(transform.prj(tmpVec3.set(x1, y1, z1)))
		ext(transform.prj(tmpVec3.set(x0, y1, z1)))
		return this
	}

	/**
	 * Returns whether the given bounding box is contained in this bounding box.
	 * @param b The bounding box
	 * @return Whether the given bounding box is contained
	 */
	override fun contains(b: BoxRo): Boolean {
		return !isValid() || (min.x <= b.min.x && min.y <= b.min.y && min.z <= b.min.z && max.x >= b.max.x && max.y >= b.max.y && max.z >= b.max.z)
	}

	/**
	 * Returns whether the given vector is contained in this bounding box.
	 * @param v The vector
	 * @return Whether the vector is contained or not.
	 */
	override fun contains(v: Vector3Ro): Boolean {
		return contains(v.x, v.y, v.z)
	}

	override fun contains(x: Float, y: Float, z: Float): Boolean {
		return min.x <= x && max.x >= x && min.y <= y && max.y >= y && min.z <= z && max.z >= z
	}

	fun clamp(clip: MinMaxRo): Box {
		min.x = maxOf(min.x, clip.xMin)
		min.y = maxOf(min.y, clip.yMin)
		max.x = minOf(max.x, clip.xMax)
		max.y = minOf(max.y, clip.yMax)
		return this
	}

	/**
	 * Expands all boundaries [x], [y], [right], and [bottom] by the given amount.
	 */
	fun inflate(all: Float) = inflate(all, all, all, all, all, all)

	/**
	 * Increases this value by the given deltas.
	 */
	fun inflate(left: Float, top: Float, right: Float, bottom: Float): Box {
		min.x -= left
		min.y -= top
		max.x += right
		max.y += bottom
		return this
	}

	/**
	 * Increases this value by the given deltas.
	 */
	fun inflate(left: Float, top: Float, front: Float, right: Float, bottom: Float, back: Float): Box {
		min.x -= left
		min.y -= top
		min.z -= front
		max.x += right
		max.y += bottom
		max.z += back
		return this
	}

	/**
	 * Expands all boundaries [left], [top], [right], and [bottom] by the [pad] values.
	 */
	fun inflate(pad: PadRo) = inflate(pad.left, pad.top, pad.right, pad.bottom)

	/**
	 * Reduces all boundaries [left], [top], [right], and [bottom] by the given value.
	 */
	fun reduce(all: Float) = inflate(-all)

	/**
	 * Reduces all boundaries [left], [top], [right], and [bottom] by the given values.
	 */
	fun reduce(left: Float, top: Float, right: Float, bottom: Float) = inflate(-left, -top, -right, -bottom)

	/**
	 * Reduces all boundaries [left], [top], [front], [right], [bottom], and [back] by the given values.
	 */
	fun reduce(left: Float, top: Float, front: Float, right: Float, bottom: Float, back: Float): Box = inflate(-left, -top, -front, -right, -bottom, -back)

	/**
	 * Reduces all boundaries [left], [top], [right], and [bottom] by the [pad] values.
	 */
	fun reduce(pad: PadRo) = inflate(-pad.left, -pad.top, -pad.right, -pad.bottom)

	operator fun minusAssign(pad: PadRo) {
		reduce(pad)
	}

	operator fun plusAssign(pad: PadRo) {
		inflate(pad)
	}

	override fun toString(): String {
		return "[$min|$max]"
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null) return false
		other as BoxRo

		if (min != other.min) return false
		if (max != other.max) return false

		return true
	}

	override fun hashCode(): Int {
		var result = min.hashCode()
		result = 31 * result + max.hashCode()
		return result
	}


	companion object {

		private val tmpVec3 = Vector3()

	}
}

@Serializer(forClass = MinMax::class)
object BoxSerializer : KSerializer<Box> {

	override val descriptor: SerialDescriptor = PrimitiveDescriptor("Box", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: Box) {
		encoder.encodeSerializableValue(Float.serializer().list, listOf(value.min.x, value.min.y, value.min.z, value.max.x, value.max.y, value.max.z))
	}

	override fun deserialize(decoder: Decoder): Box {
		val values = decoder.decodeSerializableValue(Float.serializer().list)
		return Box().set(values[0], values[1], values[2], values[3], values[4], values[5])
	}
}