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

import com.acornui.core.closeTo
import com.acornui.math.MathUtils.clamp
import kotlin.math.*

interface QuaternionRo {

	val x: Float
	val y: Float
	val z: Float
	val w: Float

	/**
	 * @return the euclidian length of this quaternion
	 */
	fun len(): Float

	/**
	 * Get the pole of the gimbal lock, if any.
	 * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
	 */
	fun getGimbalPole(): Int

	/**
	 * Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
	 * @return the rotation around the z axis in radians (between -PI and +PI)
	 */
	fun getRoll(): Float

	/**
	 * Get the pitch euler angle in radians, which is the rotation around the x axis. Requires that this quaternion is normalized.
	 * @return the rotation around the x axis in radians (between -(PI/2) and +(PI/2))
	 */
	fun getPitch(): Float

	/**
	 * Get the yaw euler angle in radians, which is the rotation around the y axis. Requires that this quaternion is normalized.
	 * @return the rotation around the y axis in radians (between -PI and +PI)
	 */
	fun getYaw(): Float

	/**
	 * @return the length of this quaternion without square root
	 */
	fun len2(): Float

	/**
	 * @return If this quaternion is an identity Quaternion
	 */
	fun isIdentity(): Boolean

	/**
	 * @return If this quaternion is an identity Quaternion
	 */
	fun isIdentity(tolerance: Float): Boolean

	/**
	 * Get the dot product between this and the other quaternion (commutative).
	 * @param other the other quaternion.
	 * @return the dot product of this and the other quaternion.
	 */
	fun dot(other: QuaternionRo): Float

	/**
	 * Get the dot product between this and the other quaternion (commutative).
	 * @param x the x component of the other quaternion
	 * @param y the y component of the other quaternion
	 * @param z the z component of the other quaternion
	 * @param w the w component of the other quaternion
	 * @return the dot product of this and the other quaternion.
	 */
	fun dot(x: Float, y: Float, z: Float, w: Float): Float

	/**
	 * Get the axis-angle representation of the rotation in radians. The supplied vector will receive the axis (x, y and z values)
	 * of the rotation and the value returned is the angle in radians around that axis. Note that this method will alter the
	 * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
	 * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
	 * zero.
	 *
	 * @param axis vector which will receive the axis
	 * @return the angle in radians
	 * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a>
	 */
	fun getAxisAngle(axis: Vector3): Float

	/**
	 * Get the angle in radians of the rotation this quaternion represents. Does not normalize the quaternion. Use
	 * {@link #getAxisAngle(Vector3)} to get both the axis and the angle of this rotation. Use
	 * {@link #getAngleAround(Vector3)} to get the angle around a specific axis.
	 * @return the angle in radians of the rotation
	 */
	fun getAngle(): Float

	/**
	 * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
	 * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
	 * axis perpendicular to the specified axis.
	 * </p>
	 * The swing and twist rotation can be used to reconstruct the original quaternion: this = swing * twist
	 *
	 * @param axisX the X component of the normalized axis for which to get the swing and twist rotation
	 * @param axisY the Y component of the normalized axis for which to get the swing and twist rotation
	 * @param axisZ the Z component of the normalized axis for which to get the swing and twist rotation
	 * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
	 * @param twist will receive the twist rotation: the rotation around the specified axis
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
	 */
	fun getSwingTwist(axisX: Float, axisY: Float, axisZ: Float, swing: Quaternion, twist: Quaternion)

	/**
	 * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
	 * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
	 * axis perpendicular to the specified axis.
	 * </p>
	 * The swing and twist rotation can be used to reconstruct the original quaternion: this = swing * twist
	 *
	 * @param axis the normalized axis for which to get the swing and twist rotation
	 * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
	 * @param twist will receive the twist rotation: the rotation around the specified axis
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
	 */
	fun getSwingTwist(axis: Vector3Ro, swing: Quaternion, twist: Quaternion)

	/**
	 * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
	 * @param axisX the x component of the normalized axis for which to get the angle
	 * @param axisY the y component of the normalized axis for which to get the angle
	 * @param axisZ the z component of the normalized axis for which to get the angle
	 * @return the angle in radians of the rotation around the specified axis
	 */
	fun getAngleAround(axisX: Float, axisY: Float, axisZ: Float): Float

	/**
	 * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
	 * @param axis the normalized axis for which to get the angle
	 * @return the angle in radians of the rotation around the specified axis
	 */
	fun getAngleAround(axis: Vector3Ro): Float {
		return getAngleAround(axis.x, axis.y, axis.z)
	}

	/**
	 * Transforms the given vector using this quaternion
	 *
	 * @param v Vector to transform
	 */
	fun transform(v: Vector3): Vector3 {
		tmp2.set(this)
		tmp2.conjugate()
		tmp2.mulLeft(tmp1.set(v.x, v.y, v.z, 0f)).mulLeft(this)

		v.x = tmp2.x
		v.y = tmp2.y
		v.z = tmp2.z
		return v
	}

	fun closeTo(other: QuaternionRo?, epsilon: Float = 0.0001f): Boolean {
		if (other == null) return false
		return x.closeTo(other.x, epsilon) &&
				y.closeTo(other.y, epsilon) &&
				z.closeTo(other.z, epsilon) &&
				w.closeTo(other.w, epsilon)
	}

	fun copy(x: Float = this.x,
			 y: Float = this.y,
			 z: Float = this.z,
			 w: Float = this.w
	): Quaternion {
		return Quaternion(x, y, z, w)
	}

	companion object {
		private val tmp1 = Quaternion(0f, 0f, 0f, 0f)
		private val tmp2 = Quaternion(0f, 0f, 0f, 0f)
	}

}

/**
 * A simple Quaternion class. (May be an oxymoron)
 * @see <a href="http://en.wikipedia.org/wiki/Quaternion">http://en.wikipedia.org/wiki/Quaternion</a>
 * @author badlogicgames@gmail.com
 * @author vesuvio
 * @author xoppa */

class Quaternion(
		override var x: Float = 0f,
		override var y: Float = 0f,
		override var z: Float = 0f,
		override var w: Float = 1f
) : QuaternionRo {

	/**
	 * Sets the components of the quaternion
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @param w The w-component
	 * @return This quaternion for chaining
	 */
	fun set(x: Float, y: Float, z: Float, w: Float): Quaternion {
		this.x = x
		this.y = y
		this.z = z
		this.w = w
		return this
	}

	/**
	 * Sets the quaternion components from the given quaternion.
	 * @param quaternion The quaternion.
	 * @return This quaternion for chaining.
	 */
	fun set(quaternion: QuaternionRo): Quaternion {
		return this.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
	}

	/**
	 * @return the euclidian length of this quaternion
	 */
	override fun len(): Float {
		return sqrt(x * x + y * y + z * z + w * w)
	}

	override fun toString(): String {
		return "[$x|$y|$z|$w]"
	}

	@Deprecated("Use setEulerAngles(pitch, yaw, roll)", ReplaceWith("setEulerAngles(pitch, yaw, roll)"), DeprecationLevel.ERROR)
	fun setEulerAnglesRad(yaw: Float, pitch: Float, roll: Float): Quaternion = setEulerAngles(pitch, yaw, roll)

	/**
	 * Sets the quaternion to the given euler angles in radians.
	 * @param pitch the rotation around the x axis in radians
	 * @param yaw the rotation around the y axis in radians
	 * @param roll the rotation around the z axis in radians
	 * @return this quaternion
	 */
	fun setEulerAngles(pitch: Float, yaw: Float, roll: Float): Quaternion {
		val hr = roll * 0.5f
		val shr = sin(hr)
		val chr = cos(hr)
		val hp = pitch * 0.5f
		val shp = sin(hp)
		val chp = cos(hp)
		val hy = yaw * 0.5f
		val shy = sin(hy)
		val chy = cos(hy)
		val chy_shp = chy * shp
		val shy_chp = shy * chp
		val chy_chp = chy * chp
		val shy_shp = shy * shp

		x = (chy_shp * chr) + (shy_chp * shr) // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
		y = (shy_chp * chr) - (chy_shp * shr) // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
		z = (chy_chp * shr) - (shy_shp * chr) // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
		w = (chy_chp * chr) + (shy_shp * shr) // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
		return this
	}

	/**
	 * Get the pole of the gimbal lock, if any.
	 * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
	 */
	override fun getGimbalPole(): Int {
		val t = y * x + z * w
		return if (t > 0.499f) 1 else (if (t < -0.499f) -1 else 0)
	}

	/**
	 * Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
	 * @return the rotation around the z axis in radians (between -PI and +PI)
	 */
	override fun getRoll(): Float {
		val pole = getGimbalPole()
		return if (pole == 0) atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z)) else pole.toFloat() * 2f * atan2(y, w)
	}

	/**
	 * Get the pitch euler angle in radians, which is the rotation around the x axis. Requires that this quaternion is normalized.
	 * @return the rotation around the x axis in radians (between -(PI/2) and +(PI/2))
	 */
	override fun getPitch(): Float {
		val pole = getGimbalPole()
		return if (pole == 0) asin(clamp(2f * (w * x - z * y), -1f, 1f)) else pole.toFloat() * PI * 0.5f
	}

	/**
	 * Get the yaw euler angle in radians, which is the rotation around the y axis. Requires that this quaternion is normalized.
	 * @return the rotation around the y axis in radians (between -PI and +PI)
	 */
	override fun getYaw(): Float {
		return if (getGimbalPole() == 0) atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)) else 0f
	}

	/**
	 * @return the length of this quaternion without square root
	 */
	override fun len2(): Float {
		return x * x + y * y + z * z + w * w
	}

	/**
	 * Normalizes this quaternion to unit length
	 * @return the quaternion for chaining
	 */
	fun nor(): Quaternion {
		var len = len2()
		if (len != 0f && !MathUtils.isEqual(len, 1f)) {
			len = sqrt(len)
			w /= len
			x /= len
			y /= len
			z /= len
		}
		return this
	}

	/**
	 * Conjugate the quaternion.
	 *
	 * @return This quaternion for chaining
	 */
	fun conjugate(): Quaternion {
		x = -x
		y = -y
		z = -z
		return this
	}

	/**
	 * Multiplies this quaternion with another one in the form of this = this * other
	 *
	 * @param other Quaternion to multiply with
	 * @return This quaternion for chaining
	 */
	fun mul(other: QuaternionRo): Quaternion {
		val newX = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y
		val newY = this.w * other.y + this.y * other.w + this.z * other.x - this.x * other.z
		val newZ = this.w * other.z + this.z * other.w + this.x * other.y - this.y * other.x
		val newW = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z
		this.x = newX
		this.y = newY
		this.z = newZ
		this.w = newW
		return this
	}

	operator fun times(other: QuaternionRo): Quaternion {
		return copy().mul(other)
	}

	/**
	 * Multiplies this quaternion with another one in the form of this = this * other
	 *
	 * @param x the x component of the other quaternion to multiply with
	 * @param y the y component of the other quaternion to multiply with
	 * @param z the z component of the other quaternion to multiply with
	 * @param w the w component of the other quaternion to multiply with
	 * @return This quaternion for chaining
	 */
	fun mul(x: Float, y: Float, z: Float, w: Float): Quaternion {
		val newX = this.w * x + this.x * w + this.y * z - this.z * y
		val newY = this.w * y + this.y * w + this.z * x - this.x * z
		val newZ = this.w * z + this.z * w + this.x * y - this.y * x
		val newW = this.w * w - this.x * x - this.y * y - this.z * z
		this.x = newX
		this.y = newY
		this.z = newZ
		this.w = newW
		return this
	}

	/**
	 * Multiplies this quaternion with another one in the form of this = other * this
	 *
	 * @param other Quaternion to multiply with
	 * @return This quaternion for chaining
	 */
	fun mulLeft(other: QuaternionRo): Quaternion {
		val newX = other.w * this.x + other.x * this.w + other.y * this.z - other.z * y
		val newY = other.w * this.y + other.y * this.w + other.z * this.x - other.x * z
		val newZ = other.w * this.z + other.z * this.w + other.x * this.y - other.y * x
		val newW = other.w * this.w - other.x * this.x - other.y * this.y - other.z * z
		this.x = newX
		this.y = newY
		this.z = newZ
		this.w = newW
		return this
	}

	/**
	 * Multiplies this quaternion with another one in the form of this = other * this
	 *
	 * @param x the x component of the other quaternion to multiply with
	 * @param y the y component of the other quaternion to multiply with
	 * @param z the z component of the other quaternion to multiply with
	 * @param w the w component of the other quaternion to multiply with
	 * @return This quaternion for chaining
	 */
	fun mulLeft(x: Float, y: Float, z: Float, w: Float): Quaternion {
		val newX = w * this.x + x * this.w + y * this.z - z * y
		val newY = w * this.y + y * this.w + z * this.x - x * z
		val newZ = w * this.z + z * this.w + x * this.y - y * x
		val newW = w * this.w - x * this.x - y * this.y - z * z
		this.x = newX
		this.y = newY
		this.z = newZ
		this.w = newW
		return this
	}

	/**
	 * Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion
	 */
	fun add(quaternion: QuaternionRo): Quaternion {
		this.x += quaternion.x
		this.y += quaternion.y
		this.z += quaternion.z
		this.w += quaternion.w
		return this
	}

	operator fun plus(quaternion: QuaternionRo): Quaternion {
		return copy().plus(quaternion)
	}

	/**
	 * Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion
	 */
	fun add(qx: Float, qy: Float, qz: Float, qw: Float): Quaternion {
		this.x += qx
		this.y += qy
		this.z += qz
		this.w += qw
		return this
	}

	/**
	 * Sets the quaternion to an identity Quaternion
	 * @return this quaternion for chaining
	 */
	fun idt(): Quaternion {
		return this.set(0f, 0f, 0f, 1f)
	}

	/**
	 * @return If this quaternion is an identity Quaternion
	 */
	override fun isIdentity(): Boolean {
		return MathUtils.isZero(x) && MathUtils.isZero(y) && MathUtils.isZero(z) && MathUtils.isEqual(w, 1f)
	}

	/**
	 * @return If this quaternion is an identity Quaternion
	 */
	override fun isIdentity(tolerance: Float): Boolean {
		return MathUtils.isZero(x, tolerance) && MathUtils.isZero(y, tolerance) && MathUtils.isZero(z, tolerance) && MathUtils.isEqual(w, 1f, tolerance)
	}

	/**
	 * Sets the quaternion components from the given axis and angle around that axis.
	 *
	 * @param axis The axis
	 * @param radians The angle in radians
	 * @return This quaternion for chaining.
	 */
	fun setFromAxis(axis: Vector3Ro, radians: Float): Quaternion {
		return setFromAxis(axis.x, axis.y, axis.z, radians)
	}

	/**
	 * Sets the quaternion components from the given axis and angle around that axis.
	 * @param x X direction of the axis
	 * @param y Y direction of the axis
	 * @param z Z direction of the axis
	 * @param radians The angle in radians
	 * @return This quaternion for chaining.
	 */
	fun setFromAxis(x: Float, y: Float, z: Float, radians: Float): Quaternion {
		var d = Vector3.len(x, y, z)
		if (d == 0f) return idt()
		d = 1f / d
		val l_ang = radians
		val l_sin = sin(l_ang / 2f)
		val l_cos = cos(l_ang / 2f)
		return this.set(d * x * l_sin, d * y * l_sin, d * z * l_sin, l_cos).nor()
	}

	/**
	 * Sets the Quaternion from the given matrix, optionally removing any scaling.
	 */
	fun setFromMatrix(matrix: Matrix4Ro, normalizeAxes: Boolean = false): Quaternion {
		return setFromAxes(matrix.values[Matrix4.M00], matrix.values[Matrix4.M01], matrix.values[Matrix4.M02], matrix.values[Matrix4.M10], matrix.values[Matrix4.M11], matrix.values[Matrix4.M12], matrix.values[Matrix4.M20], matrix.values[Matrix4.M21], matrix.values[Matrix4.M22], normalizeAxes)
	}

	/**
	 * Sets the Quaternion from the given matrix, optionally removing any scaling.
	 */
	fun setFromMatrix(matrix: Matrix3Ro, normalizeAxes: Boolean = false): Quaternion {
		return setFromAxes(matrix.values[Matrix3.M00], matrix.values[Matrix3.M01], matrix.values[Matrix3.M02], matrix.values[Matrix3.M10], matrix.values[Matrix3.M11], matrix.values[Matrix3.M12], matrix.values[Matrix3.M20], matrix.values[Matrix3.M21], matrix.values[Matrix3.M22], normalizeAxes)
	}

	/**
	 * <p>
	 * Sets the Quaternion from the given x-, y- and z-axis which have to be orthonormal.
	 * </p>
	 *
	 * <p>
	 * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
	 * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
	 * </p>
	 *
	 * @param xx x-axis x-coordinate
	 * @param xy x-axis y-coordinate
	 * @param xz x-axis z-coordinate
	 * @param yx y-axis x-coordinate
	 * @param yy y-axis y-coordinate
	 * @param yz y-axis z-coordinate
	 * @param zx z-axis x-coordinate
	 * @param zy z-axis y-coordinate
	 * @param zz z-axis z-coordinate
	 */
	fun setFromAxes(xx: Float, xy: Float, xz: Float, yx: Float, yy: Float, yz: Float, zx: Float, zy: Float, zz: Float): Quaternion {
		return setFromAxes(xx, xy, xz, yx, yy, yz, zx, zy, zz, false)
	}

	/**
	 * <p>
	 * Sets the Quaternion from the given x-, y- and z-axis.
	 * </p>
	 *
	 * <p>
	 * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
	 * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
	 * </p>
	 *
	 * @param normalizeAxes whether to normalize the axes (necessary when they contain scaling)
	 * @param xx x-axis x-coordinate
	 * @param xy x-axis y-coordinate
	 * @param xz x-axis z-coordinate
	 * @param yx y-axis x-coordinate
	 * @param yy y-axis y-coordinate
	 * @param yz y-axis z-coordinate
	 * @param zx z-axis x-coordinate
	 * @param zy z-axis y-coordinate
	 * @param zz z-axis z-coordinate
	 */
	@Suppress("NAME_SHADOWING") fun setFromAxes(xx: Float, xy: Float, xz: Float, yx: Float, yy: Float, yz: Float, zx: Float, zy: Float, zz: Float, normalizeAxes: Boolean = false): Quaternion {
		var xx = xx
		var xy = xy
		var xz = xz
		var yy = yy
		var yz = yz
		var zx = zx
		var zy = zy
		var zz = zz
		if (normalizeAxes) {
			val lx = 1f / Vector3.len(xx, xy, xz)
			val ly = 1f / Vector3.len(yx, yy, yz)
			val lz = 1f / Vector3.len(zx, zy, zz)
			xx *= lx
			xy *= lx
			xz *= lx
			yz *= ly
			yy *= ly
			yz *= ly
			zx *= lz
			zy *= lz
			zz *= lz
		}
		// the trace is the sum of the diagonal elements; see
		// http://mathworld.wolfram.com/MatrixTrace.html
		val t = xx + yy + zz

		// we protect the division by s by ensuring that s>=1
		if (t >= 0) {
			// |w| >= .5
			var s = sqrt((t + 1)) // |s|>=1 ...
			w = 0.5f * s
			s = 0.5f / s // so this division isn't bad
			x = (zy - yz) * s
			y = (xz - zx) * s
			z = (yx - xy) * s
		} else if ((xx > yy) && (xx > zz)) {
			var s = sqrt(1f + xx - yy - zz) // |s|>=1
			x = s * 0.5f // |x| >= .5
			s = 0.5f / s
			y = (yx + xy) * s
			z = (xz + zx) * s
			w = (zy - yz) * s
		} else if (yy > zz) {
			var s = sqrt(1f + yy - xx - zz) // |s|>=1
			y = s * 0.5f // |y| >= .5
			s = 0.5f / s
			x = (yx + xy) * s
			z = (zy + yz) * s
			w = (xz - zx) * s
		} else {
			var s = sqrt(1f + zz - xx - yy) // |s|>=1
			z = s * 0.5f // |z| >= .5
			s = 0.5f / s
			x = (xz + zx) * s
			y = (zy + yz) * s
			w = (yx - xy) * s
		}

		return this
	}

	/**
	 * Set this quaternion to the rotation between two vectors.
	 * @param v1 The base vector, which should be normalized.
	 * @param v2 The target vector, which should be normalized.
	 * @return This quaternion for chaining
	 */
	fun setFromCross(v1: Vector3Ro, v2: Vector3Ro): Quaternion {
		val dot = MathUtils.clamp(v1.dot(v2), -1f, 1f)
		val angle = acos(dot)
		return setFromAxis(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x, angle)
	}

	/**
	 * Set this quaternion to the rotation between two vectors.
	 * @param x1 The base vectors x value, which should be normalized.
	 * @param y1 The base vectors y value, which should be normalized.
	 * @param z1 The base vectors z value, which should be normalized.
	 * @param x2 The target vector x value, which should be normalized.
	 * @param y2 The target vector y value, which should be normalized.
	 * @param z2 The target vector z value, which should be normalized.
	 * @return This quaternion for chaining
	 */
	fun setFromCross(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Quaternion {
		val dot = MathUtils.clamp(Vector3.dot(x1, y1, z1, x2, y2, z2), -1f, 1f)
		val angle = acos(dot)
		return setFromAxis(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2, angle)
	}

	/**
	 * Spherical linear interpolation between this quaternion and the other quaternion, based on the alpha value in the range
	 * [0,1]. Taken from. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
	 * @param end the end quaternion
	 * @param alpha alpha in the range [0,1]
	 * @return this quaternion for chaining
	 */
	fun slerp(end: QuaternionRo, alpha: Float): Quaternion {
		val dot = dot(end)
		val absDot = if (dot < 0f) -dot else dot

		// Set the first and second scale for the interpolation
		var scale0 = 1f - alpha
		var scale1 = alpha

		// Check if the angle between the 2 quaternions was big enough to warrant such calculations
		if ((1f - absDot) > 0.1f) {
			// Get the angle between the 2 quaternions,
			// and then store the sin() of that angle
			val angle = acos(absDot)
			val invSinTheta = 1f / sin(angle)

			// Calculate the scale for q1 and q2, according to the angle and its sine value
			scale0 = (sin((1f - alpha) * angle) * invSinTheta)
			scale1 = (sin(alpha * angle) * invSinTheta)
		}

		if (dot < 0f) scale1 = -scale1

		// Calculate the x, y, z and w values for the quaternion by using a
		// special form of linear interpolation for quaternions.
		x = (scale0 * x) + (scale1 * end.x)
		y = (scale0 * y) + (scale1 * end.y)
		z = (scale0 * z) + (scale1 * end.z)
		w = (scale0 * w) + (scale1 * end.w)

		// Return the interpolated quaternion
		return this
	}

	/**
	 * Spherical linearly interpolates multiple quaternions and stores the result in this Quaternion.
	 * Will not destroy the data previously inside the elements of q.
	 * result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where w_i=1/n.
	 * @param q List of quaternions
	 * @return This quaternion for chaining
	 */
	fun slerp(q: Array<QuaternionRo>): Quaternion {
		//Calculate exponents and multiply everything from left to right
		val w = 1f / q.size.toFloat()
		set(q[0]).exp(w)
		for (i in 1..q.lastIndex)
			mul(tmp1.set(q[i]).exp(w))
		nor()
		return this
	}

	/**
	 * Spherical linearly interpolates multiple quaternions by the given weights and stores the result in this Quaternion.
	 * Will not destroy the data previously inside the elements of q or w.
	 * result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where the sum of w_i is 1.
	 * Lists must be equal in length.
	 * @param q List of quaternions
	 * @param w List of weights
	 * @return This quaternion for chaining
	 */
	fun slerp(q: Array<QuaternionRo>, w: FloatArray): Quaternion {

		//Calculate exponents and multiply everything from left to right
		set(q[0]).exp(w[0])
		for (i in 1..q.lastIndex)
			mul(tmp1.set(q[i]).exp(w[i]))
		nor()
		return this
	}

	/**
	 * Calculates (this quaternion)^alpha where alpha is a real number and stores the result in this quaternion.
	 * See http://en.wikipedia.org/wiki/Quaternion#Exponential.2C_logarithm.2C_and_power
	 * @param alpha Exponent
	 * @return This quaternion for chaining
	 */
	fun exp(alpha: Float): Quaternion {

		//Calculate |q|^alpha
		val norm = len()
		val normExp = norm.pow(alpha)

		//Calculate theta
		val theta = acos((w / norm))

		//Calculate coefficient of basis elements
		val coeff: Float
		if (abs(theta) < 0.001f)
		//If theta is small enough, use the limit of sin(alpha*theta) / sin(theta) instead of actual value
			coeff = normExp * alpha / norm
		else
			coeff = normExp * sin(alpha * theta) / (norm * sin(theta))

		//Write results
		w = normExp * cos(alpha * theta)
		x *= coeff
		y *= coeff
		z *= coeff

		//Fix any possible discrepancies
		nor()

		return this
	}

	/**
	 * Get the dot product between this and the other quaternion (commutative).
	 * @param other the other quaternion.
	 * @return the dot product of this and the other quaternion.
	 */
	override fun dot(other: QuaternionRo): Float {
		return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w
	}

	/**
	 * Get the dot product between this and the other quaternion (commutative).
	 * @param x the x component of the other quaternion
	 * @param y the y component of the other quaternion
	 * @param z the z component of the other quaternion
	 * @param w the w component of the other quaternion
	 * @return the dot product of this and the other quaternion.
	 */
	override fun dot(x: Float, y: Float, z: Float, w: Float): Float {
		return this.x * x + this.y * y + this.z * z + this.w * w
	}

	/**
	 * Multiplies the components of this quaternion with the given scalar.
	 * @param scalar the scalar.
	 * @return this quaternion for chaining.
	 */
	fun mul(scalar: Float): Quaternion {
		this.x *= scalar
		this.y *= scalar
		this.z *= scalar
		this.w *= scalar
		return this
	}

	fun times(scalar: Float): Quaternion {
		return copy().mul(scalar)
	}

	/**
	 * Get the axis-angle representation of the rotation in radians. The supplied vector will receive the axis (x, y and z values)
	 * of the rotation and the value returned is the angle in radians around that axis. Note that this method will alter the
	 * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
	 * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
	 * zero.
	 *
	 * @param axis vector which will receive the axis
	 * @return the angle in radians
	 * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a>
	 */
	override fun getAxisAngle(axis: Vector3): Float {
		if (this.w > 1f) this.nor() // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised
		val angle = (2f * acos(this.w))
		val s = sqrt((1f - this.w * this.w)) // assuming quaternion normalised then w is less than 1, so term always positive.
		if (s < MathUtils.FLOAT_ROUNDING_ERROR) {
			// test to avoid divide by zero, s is always positive due to sqrt
			// if s close to zero then direction of axis not important
			axis.x = this.x // if it is important that axis is normalised then replace with x=1; y=z=0;
			axis.y = this.y
			axis.z = this.z
		} else {
			axis.x = (this.x / s) // normalise axis
			axis.y = (this.y / s)
			axis.z = (this.z / s)
		}

		return angle
	}

	/**
	 * Get the angle in radians of the rotation this quaternion represents. Does not normalize the quaternion. Use
	 * {@link #getAxisAngle(Vector3)} to get both the axis and the angle of this rotation. Use
	 * {@link #getAngleAround(Vector3)} to get the angle around a specific axis.
	 * @return the angle in radians of the rotation
	 */
	override fun getAngle(): Float {
		return (2f * acos((if ((this.w > 1f)) (this.w / len()) else this.w)))
	}

	/**
	 * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
	 * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
	 * axis perpendicular to the specified axis.
	 * </p>
	 * The swing and twist rotation can be used to reconstruct the original quaternion: this = swing * twist
	 *
	 * @param axisX the X component of the normalized axis for which to get the swing and twist rotation
	 * @param axisY the Y component of the normalized axis for which to get the swing and twist rotation
	 * @param axisZ the Z component of the normalized axis for which to get the swing and twist rotation
	 * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
	 * @param twist will receive the twist rotation: the rotation around the specified axis
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
	 */
	override fun getSwingTwist(axisX: Float, axisY: Float, axisZ: Float, swing: Quaternion, twist: Quaternion) {
		val d = Vector3.dot(this.x, this.y, this.z, axisX, axisY, axisZ)
		twist.set(axisX * d, axisY * d, axisZ * d, this.w).nor()
		swing.set(twist).conjugate().mulLeft(this)
	}

	/**
	 * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
	 * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
	 * axis perpendicular to the specified axis.
	 * </p>
	 * The swing and twist rotation can be used to reconstruct the original quaternion: this = swing * twist
	 *
	 * @param axis the normalized axis for which to get the swing and twist rotation
	 * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
	 * @param twist will receive the twist rotation: the rotation around the specified axis
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
	 */
	override fun getSwingTwist(axis: Vector3Ro, swing: Quaternion, twist: Quaternion) {
		getSwingTwist(axis.x, axis.y, axis.z, swing, twist)
	}

	/**
	 * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
	 * @param axisX the x component of the normalized axis for which to get the angle
	 * @param axisY the y component of the normalized axis for which to get the angle
	 * @param axisZ the z component of the normalized axis for which to get the angle
	 * @return the angle in radians of the rotation around the specified axis
	 */
	override fun getAngleAround(axisX: Float, axisY: Float, axisZ: Float): Float {
		val d = Vector3.dot(this.x, this.y, this.z, axisX, axisY, axisZ)
		val l2 = Quaternion.len2(axisX * d, axisY * d, axisZ * d, this.w)
		return if (MathUtils.isZero(l2)) 0f else (2f * acos(MathUtils.clamp((this.w / sqrt(l2)), -1f, 1f)))
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as QuaternionRo
		if (x != other.x) return false
		if (y != other.y) return false
		if (z != other.z) return false
		if (w != other.w) return false
		return true
	}

	override fun hashCode(): Int {
		var result = x.hashCode()
		result = 31 * result + y.hashCode()
		result = 31 * result + z.hashCode()
		result = 31 * result + w.hashCode()
		return result
	}


	companion object {

		private val tmp1 = Quaternion(0f, 0f, 0f, 0f)

		/**
		 * @return the euclidian length of the specified quaternion
		 */
		fun len(x: Float, y: Float, z: Float, w: Float): Float {
			return sqrt(x * x + y * y + z * z + w * w)
		}

		fun len2(x: Float, y: Float, z: Float, w: Float): Float {
			return x * x + y * y + z * z + w * w
		}

		/**
		 * Get the dot product between the two quaternions (commutative).
		 * @param x1 the x component of the first quaternion
		 * @param y1 the y component of the first quaternion
		 * @param z1 the z component of the first quaternion
		 * @param w1 the w component of the first quaternion
		 * @param x2 the x component of the second quaternion
		 * @param y2 the y component of the second quaternion
		 * @param z2 the z component of the second quaternion
		 * @param w2 the w component of the second quaternion
		 * @return the dot product between the first and second quaternion.
		 */
		fun dot(x1: Float, y1: Float, z1: Float, w1: Float, x2: Float, y2: Float, z2: Float, w2: Float): Float {
			return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2
		}
	}
}
