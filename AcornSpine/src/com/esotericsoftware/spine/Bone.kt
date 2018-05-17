/*
 * Spine Runtimes Software License
 * Version 2.3
 *
 * Copyright (c) 2013-2015, Esoteric Software
 * All rights reserved.
 *
 * You are granted a perpetual, non-exclusive, non-sublicensable and
 * non-transferable license to use, install, execute and perform the Spine
 * Runtimes Software (the "Software") and derivative works solely for personal
 * or internal use. Without the written permission of Esoteric Software (see
 * Section 2 of the Spine Software License Agreement), you may not (a) modify,
 * translate, adapt or otherwise create derivative works, improvements of the
 * Software or develop new applications using the Software or (b) remove,
 * delete, alter or obscure any trademarks or any copyright, trademark, patent
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 *
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.spine

import com.acornui.math.MathUtils
import com.acornui.math.Matrix3
import com.acornui.math.Vector2
import com.esotericsoftware.spine.data.BoneData

class Bone : Updatable {
	val data: BoneData
	val skeleton: Skeleton
	val parent: Bone?
	var x: Float = 0f
	var y: Float = 0f
	/** Returns the forward kinetics rotation.  */
	var rotation: Float = 0f
	var scaleX: Float = 0f
	var scaleY: Float = 0f
	internal var appliedRotation: Float = 0f
	internal var appliedScaleX: Float = 0f
	internal var appliedScaleY: Float = 0f

	var a: Float = 0f
		internal set
	var b: Float = 0f
		internal set
	var worldX: Float = 0f
		internal set
	var c: Float = 0f
		internal set
	var d: Float = 0f
		internal set
	var worldY: Float = 0f
		internal set
	var worldSignX: Float = 0f
		internal set
	var worldSignY: Float = 0f
		internal set

	constructor(data: BoneData, skeleton: Skeleton, parent: Bone?) {
		this.data = data
		this.skeleton = skeleton
		this.parent = parent
		setToSetupPose()
	}

	/** Same as [.updateWorldTransform]. This method exists for Bone to implement [Updatable].  */
	override fun update() {
		updateWorldTransform(x, y, rotation, scaleX, scaleY)
	}

	/** Computes the world SRT using the parent bone and the specified local SRT.  */
	fun updateWorldTransform(x: Float = this.x, y: Float = this.y, rotation: Float = this.rotation, scaleX: Float = this.scaleX, scaleY: Float = this.scaleY) {
		var x = x
		var y = y
		appliedRotation = rotation
		appliedScaleX = scaleX
		appliedScaleY = scaleY

		var cos = MathUtils.cos(rotation * MathUtils.degRad)
		var sin = MathUtils.sin(rotation * MathUtils.degRad)
		var la = cos * scaleX
		var lb = -sin * scaleY
		var lc = sin * scaleX
		var ld = cos * scaleY
		var parent = this.parent
		if (parent == null) {
			// Root bone.
			val skeleton = this.skeleton
			if (skeleton.flipX) {
				x = -x
				la = -la
				lb = -lb
			}
			if (skeleton.flipY) {
				y = -y
				lc = -lc
				ld = -ld
			}
			a = la
			b = lb
			c = lc
			d = ld
			worldX = x
			worldY = y
			worldSignX = MathUtils.signum(scaleX)
			worldSignY = MathUtils.signum(scaleY)
			return
		}

		var pa = parent.a
		var pb = parent.b
		var pc = parent.c
		var pd = parent.d
		worldX = pa * x + pb * y + parent.worldX
		worldY = pc * x + pd * y + parent.worldY
		worldSignX = parent.worldSignX * MathUtils.signum(scaleX)
		worldSignY = parent.worldSignY * MathUtils.signum(scaleY)

		if (data.inheritRotation && data.inheritScale) {
			a = pa * la + pb * lc
			b = pa * lb + pb * ld
			c = pc * la + pd * lc
			d = pc * lb + pd * ld
		} else {
			if (data.inheritRotation) {
				// No scale inheritance.
				pa = 1f
				pb = 0f
				pc = 0f
				pd = 1f
				do {
					cos = MathUtils.cos(parent!!.appliedRotation * MathUtils.degRad)
					sin = MathUtils.sin(parent.appliedRotation * MathUtils.degRad)
					var temp = pa * cos + pb * sin
					pb = pa * -sin + pb * cos
					pa = temp
					temp = pc * cos + pd * sin
					pd = pc * -sin + pd * cos
					pc = temp

					if (!parent.data.inheritRotation) break
					parent = parent.parent
				} while (parent != null)
				a = pa * la + pb * lc
				b = pa * lb + pb * ld
				c = pc * la + pd * lc
				d = pc * lb + pd * ld
			} else if (data.inheritScale) {
				// No rotation inheritance.
				pa = 1f
				pb = 0f
				pc = 0f
				pd = 1f
				do {
					var r = parent!!.appliedRotation
					cos = MathUtils.cos(r * MathUtils.degRad)
					sin = MathUtils.sin(r * MathUtils.degRad)
					val psx = parent.appliedScaleX
					val psy = parent.appliedScaleY
					val za = cos * psx
					val zb = -sin * psy
					val zc = sin * psx
					val zd = cos * psy
					var temp = pa * za + pb * zc
					pb = pa * zb + pb * zd
					pa = temp
					temp = pc * za + pd * zc
					pd = pc * zb + pd * zd
					pc = temp

					if (psx < 0) r = -r
					cos = MathUtils.cos(-r * MathUtils.degRad)
					sin = MathUtils.sin(-r * MathUtils.degRad)
					temp = pa * cos + pb * sin
					pb = pa * -sin + pb * cos
					pa = temp
					temp = pc * cos + pd * sin
					pd = pc * -sin + pd * cos
					pc = temp

					if (!parent.data.inheritScale) break
					parent = parent.parent
				} while (parent != null)
				a = pa * la + pb * lc
				b = pa * lb + pb * ld
				c = pc * la + pd * lc
				d = pc * lb + pd * ld
			} else {
				a = la
				b = lb
				c = lc
				d = ld
			}
			if (skeleton.flipX) {
				a = -a
				b = -b
			}
			if (skeleton.flipY) {
				c = -c
				d = -d
			}
		}
	}

	fun setToSetupPose() {
		val data = this.data
		x = data.x
		y = data.y
		rotation = data.rotation
		scaleX = data.scaleX
		scaleY = data.scaleY
	}

	val worldRotationX: Float
		get() = MathUtils.atan2(c, a) * MathUtils.radDeg

	val worldRotationY: Float
		get() = MathUtils.atan2(d, b) * MathUtils.radDeg

	val worldScaleX: Float
		get() = MathUtils.sqrt((a * a + b * b)) * worldSignX

	val worldScaleY: Float
		get() = MathUtils.sqrt((c * c + d * d)) * worldSignY

	fun getWorldTransform(out: Matrix3): Matrix3 {
		val values = out.values
		values[Matrix3.M00] = a
		values[Matrix3.M01] = b
		values[Matrix3.M10] = c
		values[Matrix3.M11] = d
		values[Matrix3.M02] = worldX
		values[Matrix3.M12] = worldY
		values[Matrix3.M20] = 0f
		values[Matrix3.M21] = 0f
		values[Matrix3.M22] = 1f
		return out
	}

	fun worldToLocal(world: Vector2): Vector2 {
		val x = world.x - worldX
		val y = world.y - worldY
		val a = this.a
		val b = this.b
		val c = this.c
		val d = this.d
		val invDet = 1 / (a * d - b * c)
		world.x = x * d * invDet - y * b * invDet
		world.y = y * a * invDet - x * c * invDet
		return world
	}

	fun localToWorld(local: Vector2): Vector2 {
		val x = local.x
		val y = local.y
		local.x = x * a + y * b + worldX
		local.y = x * c + y * d + worldY
		return local
	}

	override fun toString(): String {
		return "Bone(name=\"${data.name}\")"
	}
}
