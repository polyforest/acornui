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

import com.acornui.core.INT_MAX_VALUE
import com.acornui.math.MathUtils
import com.acornui.math.PI
import com.esotericsoftware.spine.data.IkConstraintData
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class IkConstraint : Updatable {
	val data: IkConstraintData
	val bones: MutableList<Bone>
	val target: Bone
	var mix = 1f
	var bendDirection: Int = 0

	constructor(data: IkConstraintData, skeleton: Skeleton) {
		this.data = data
		mix = data.mix
		bendDirection = data.bendDirection

		bones = ArrayList(data.boneNames.size)
		for (boneName in data.boneNames) {
			val b = skeleton.findBone(boneName) ?: throw Exception("Bone not found $boneName")
			bones.add(b)
		}
		target = skeleton.findBone(data.targetName) ?: throw Exception("Target not found ${data.targetName}")
	}

	/** Copy constructor.  */
	constructor(ikConstraint: IkConstraint, skeleton: Skeleton) {
		data = ikConstraint.data
		bones = ArrayList(ikConstraint.bones.size)
		for (bone in ikConstraint.bones) {
			bones.add(skeleton.bones[bone.skeleton.bones.indexOf(bone)])
		}
		target = skeleton.bones[ikConstraint.target.skeleton.bones.indexOf(ikConstraint.target)]
		mix = ikConstraint.mix
		bendDirection = ikConstraint.bendDirection
	}

	fun apply() {
		update()
	}

	override fun update() {
		val target = this.target
		val bones = this.bones
		when (bones.size) {
			1 -> apply(bones.first(), target.worldX, target.worldY, mix)
			2 -> apply(bones.first(), bones[1], target.worldX, target.worldY, bendDirection, mix)
		}
	}

	override fun toString(): String {
		return data.name
	}

	companion object {

		/** Adjusts the bone rotation so the tip is as close to the target position as possible. The target is specified in the world
		 * coordinate system.  */
		fun apply(bone: Bone, targetX: Float, targetY: Float, alpha: Float) {
			val parentRotation = if (bone.parent == null) 0f else bone.parent.worldRotationX
			val rotation = bone.rotation
			var rotationIK = atan2(targetX - bone.worldX, targetY - bone.worldY) * MathUtils.radDeg - parentRotation
			if (bone.worldSignX != bone.worldSignY != (bone.skeleton.flipX != bone.skeleton.flipY)) rotationIK = 360 - rotationIK
			if (rotationIK > 180f)
				rotationIK -= 360f
			else if (rotationIK < -180) rotationIK += 360f
			bone.updateWorldTransform(bone.x, bone.y, rotation + (rotationIK - rotation) * alpha, bone.scaleX, bone.scaleY)
		}

		/** Adjusts the parent and child bone rotations so the tip of the child is as close to the target position as possible. The
		 * target is specified in the world coordinate system.
		 * @param child A direct descendant of the parent bone.
		 */
		fun apply(parent: Bone, child: Bone, targetX: Float, targetY: Float, bendDir: Int, alpha: Float) {
			if (alpha == 0f) return
			val px = parent.x
			val py = parent.y
			var psx = parent.scaleX
			var psy = parent.scaleY
			val os1: Int
			val os2: Int
			var s2: Int
			if (psx < 0) {
				psx = -psx
				os1 = 180
				s2 = -1
			} else {
				os1 = 0
				s2 = 1
			}
			if (psy < 0) {
				psy = -psy
				s2 = -s2
			}
			val cx = child.x
			var cy = child.y
			var csx = child.appliedScaleX
			val u = abs(psx - psy) <= 0.0001f
			if (!u && cy != 0f) {
				child.worldX = parent.a * cx + parent.worldX
				child.worldY = parent.c * cx + parent.worldY
				cy = 0f
			}
			if (csx < 0) {
				csx = -csx
				os2 = 180
			} else
				os2 = 0
			val pp = parent.parent
			val tx: Float
			val ty: Float
			val dx: Float
			val dy: Float
			if (pp == null) {
				tx = targetX - px
				ty = targetY - py
				dx = child.worldX - px
				dy = child.worldY - py
			} else {
				val a = pp.a
				val b = pp.b
				val c = pp.c
				val d = pp.d
				val invDet = 1 / (a * d - b * c)
				val wx = pp.worldX
				val wy = pp.worldY
				var x = targetX - wx
				var y = targetY - wy
				tx = (x * d - y * b) * invDet - px
				ty = (y * a - x * c) * invDet - py
				x = child.worldX - wx
				y = child.worldY - wy
				dx = (x * d - y * b) * invDet - px
				dy = (y * a - x * c) * invDet - py
			}
			val l1 = MathUtils.sqrt((dx * dx + dy * dy))
			var l2 = child.data.length * csx
			var a1: Float
			var a2: Float

			while (true) {
				if (u) {
					l2 *= psx
					var cos = (tx * tx + ty * ty - l1 * l1 - l2 * l2) / (2f * l1 * l2)
					if (cos < -1)
						cos = -1f
					else if (cos > 1) cos = 1f
					a2 = MathUtils.acos(cos) * bendDir
					val a = l1 + l2 * cos
					val o = l2 * sin(a2)
					a1 = atan2(tx * a + ty * o, ty * a - tx * o)
				} else {
					val a = psx * l2
					val b = psy * l2
					val ta = atan2(tx, ty)
					val aa = a * a
					val bb = b * b
					val ll = l1 * l1
					val dd = tx * tx + ty * ty
					val c0 = bb * ll + aa * dd - aa * bb
					val c1 = -2f * bb * l1
					val c2 = bb - aa
					val d = c1 * c1 - 4f * c2 * c0
					if (d >= 0) {
						var q = MathUtils.sqrt(d)
						if (c1 < 0) q = -q
						q = -(c1 + q) / 2
						val r0 = q / c2
						val r1 = c0 / q
						val r = if (abs(r0) < abs(r1)) r0 else r1
						if (r * r <= dd) {
							val y = MathUtils.sqrt(dd - r * r) * bendDir
							a1 = ta - atan2(r, y)
							a2 = atan2((r - l1) / psx, y / psy)
							break
						}
					}
					var minAngle = 0f
					var minDist = INT_MAX_VALUE.toFloat()
					var minX = 0f
					var minY = 0f
					var maxAngle = 0f
					var maxDist = 0f
					var maxX = 0f
					var maxY = 0f
					var x = l1 + a
					var dist = x * x
					if (dist > maxDist) {
						maxAngle = 0f
						maxDist = dist
						maxX = x
					}
					x = l1 - a
					dist = x * x
					if (dist < minDist) {
						minAngle = PI
						minDist = dist
						minX = x
					}
					val angle = MathUtils.acos(-a * l1 / (aa - bb))
					x = a * cos(angle) + l1
					val y = b * sin(angle)
					dist = x * x + y * y
					if (dist < minDist) {
						minAngle = angle
						minDist = dist
						minX = x
						minY = y
					}
					if (dist > maxDist) {
						maxAngle = angle
						maxDist = dist
						maxX = x
						maxY = y
					}
					if (dd <= (minDist + maxDist) / 2) {
						a1 = ta - atan2(minX, minY * bendDir)
						a2 = minAngle * bendDir
					} else {
						a1 = ta - atan2(maxX, maxY * bendDir)
						a2 = maxAngle * bendDir
					}
				}
				break
			}

			val o = atan2(cx, cy) * s2
			a1 = (a1 - o) * MathUtils.radDeg + os1
			a2 = (a2 + o) * MathUtils.radDeg * s2 + os2
			if (a1 > 180f)
				a1 -= 360f
			else if (a1 < -180f) a1 += 360f
			if (a2 > 180f)
				a2 -= 360f
			else if (a2 < -180f) a2 += 360f
			var rotation = parent.rotation
			parent.updateWorldTransform(px, py, rotation + (a1 - rotation) * alpha, parent.appliedScaleX, parent.appliedScaleY)
			rotation = child.rotation
			child.updateWorldTransform(cx, cy, rotation + (a2 - rotation) * alpha, child.appliedScaleX, child.appliedScaleY)
		}
	}
}
