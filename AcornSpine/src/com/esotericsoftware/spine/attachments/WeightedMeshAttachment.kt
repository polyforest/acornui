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

package com.esotericsoftware.spine.attachments

import com.acornui.core.graphics.AtlasPageData
import com.acornui.core.graphics.AtlasRegionData
import com.acornui.gl.core.Vertex
import com.acornui.graphics.Color
import com.acornui.math.Vector3
import com.esotericsoftware.spine.Slot
import com.esotericsoftware.spine.data.attachments.WeightedMeshAttachmentData

// TODO: nb, inheritFfd
/**
 * Attachment that displays a texture region.
 */
class WeightedMeshAttachment(
		val data: WeightedMeshAttachmentData,
		val page: AtlasPageData,
		val region: AtlasRegionData
) : FfdAttachment {

//	var inheritFfd: Boolean = false

	private val worldVertices: Array<Vertex>
	private val color = Color()

	init {
		val bounds = region.bounds
		val u = bounds.x / page.width.toFloat()
		val v = bounds.y / page.height.toFloat()
		val width = bounds.width / page.width.toFloat()
		val height = bounds.height / page.height.toFloat()

		val regionUVs = data.regionUVs
		worldVertices = Array(regionUVs.size / 2) {
			if (region.isRotated) {
				Vertex(
						u = u + regionUVs[it * 2 + 1] * width,
						v = v + height - regionUVs[it * 2] * height,
						normal = Vector3.NEG_Z.copy()
				)
			} else {
				Vertex(
						u = u + regionUVs[it * 2] * width,
						v = v + regionUVs[it * 2 + 1] * height,
						normal = Vector3.NEG_Z.copy()
				)
			}
		}


	}

	/**
	 * @return The updated world vertices.
	 */
	fun updateWorldVertices(slot: Slot): Array<Vertex> {
		val skeleton = slot.skeleton
		color.set(skeleton.color).mul(slot.color).mul(data.color)

		val worldVertices = this.worldVertices
		val x = skeleton.x
		val y = skeleton.y
		val skeletonBones = skeleton.bones
		val weights = data.weights
		val bones = data.bones

		val ffdArray = slot.attachmentVertices
		if (ffdArray.size == 0) {
			var w = 0
			var v = 0
			var b = 0
			val n = bones.size
			while (v < n) {
				var wx = 0f
				var wy = 0f
				val nn = bones[v++] + v
				while (v < nn) {
					val vx = weights[b]
					val vy = weights[b + 1]
					val weight = weights[b + 2]
					wx += (vx * skeletonBones[bones[v]].a + vy * skeletonBones[bones[v]].b + skeletonBones[bones[v]].worldX) * weight
					wy += (vx * skeletonBones[bones[v]].c + vy * skeletonBones[bones[v]].d + skeletonBones[bones[v]].worldY) * weight
					v++
					b += 3
				}
				val vertex = worldVertices[w++]
				vertex.position.x = wx + x
				vertex.position.y = wy + y
				vertex.position.z = 0f
				vertex.colorTint.set(color)
			}
		} else {
			var w = 0
			var v = 0
			var b = 0
			var f = 0
			val n = bones.size
			while (v < n) {
				var wx = 0f
				var wy = 0f
				val nn = bones[v++] + v
				while (v < nn) {
					val vx = weights[b] + ffdArray[f]
					val vy = weights[b + 1] + ffdArray[f + 1]
					val weight = weights[b + 2]
					wx += (vx * skeletonBones[bones[v]].a + vy * skeletonBones[bones[v]].b + skeletonBones[bones[v]].worldX) * weight
					wy += (vx * skeletonBones[bones[v]].c + vy * skeletonBones[bones[v]].d + skeletonBones[bones[v]].worldY) * weight
					v++
					b += 3
					f += 2
				}
				val vertex = worldVertices[w++]
				vertex.position.x = wx + x
				vertex.position.y = wy + y
				vertex.position.z = 0f
				vertex.colorTint.set(color)
			}
		}
		return worldVertices
	}

	override fun shouldApplyFfd(sourceAttachment: SkinAttachment): Boolean {
//		return this === sourceAttachment || inheritFfd && parentMesh === sourceAttachment
		return this === sourceAttachment // TODO: NB Temp
	}
}
