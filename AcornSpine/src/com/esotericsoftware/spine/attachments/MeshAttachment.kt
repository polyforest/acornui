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

import com.acornui.collection.ArrayList
import com.acornui.core.graphics.AtlasPageData
import com.acornui.core.graphics.AtlasRegionData
import com.acornui.graphics.Color
import com.esotericsoftware.spine.Slot
import com.esotericsoftware.spine.component.SpineVertexUtils.colorOffset
import com.esotericsoftware.spine.component.SpineVertexUtils.positionOffset
import com.esotericsoftware.spine.component.SpineVertexUtils.vertexSize
import com.esotericsoftware.spine.component.SpineVertexUtils.textureCoordOffset
import com.esotericsoftware.spine.data.attachments.MeshAttachmentData

/**
 * Attachment that displays a texture region.
 */
class MeshAttachment(
		val data: MeshAttachmentData,
		val page: AtlasPageData,
		val region: AtlasRegionData
) : FfdAttachment {

	private val worldVertices: MutableList<Float>
	private val color = Color()

//	var inheritFfd: Boolean = false

	init {
		val bounds = region.bounds
		val u = bounds.x / page.width.toFloat()
		val v = bounds.y / page.height.toFloat()
		val width = bounds.width / page.width.toFloat()
		val height = bounds.height / page.height.toFloat()

		val regionUVs = data.regionUVs
		worldVertices = ArrayList(regionUVs.size / 2 * vertexSize) { 0f }
		var i = textureCoordOffset
		val n = worldVertices.size
		var uvIndex = 0
		if (region.isRotated) {
			while (i < n) {
				worldVertices[i + 1] = v + height - regionUVs[uvIndex++] * height
				worldVertices[i] = u + regionUVs[uvIndex++] * width
				i += vertexSize
			}
		} else {
			while (i < n) {
				worldVertices[i] = u + regionUVs[uvIndex++] * width
				worldVertices[i + 1] = v + regionUVs[uvIndex++] * height
				i += vertexSize
			}
		}
	}

	/**
	 * @return The updated world vertices.
	 */
	fun updateWorldVertices(slot: Slot): List<Float> {
		val skeleton = slot.skeleton
		color.set(skeleton.color).mul(slot.color).mul(data.color)

		val worldVertices = this.worldVertices
		val slotVertices = slot.attachmentVertices
		val vertices = data.vertices
		val useSlotVertices = slotVertices.size == vertices.size

		val bone = slot.bone
		val x = skeleton.x + bone.worldX
		val y = skeleton.y + bone.worldY
		val m00 = bone.a
		val m01 = bone.b
		val m10 = bone.c
		val m11 = bone.d
		var i = 0
		val n = worldVertices.size
		var vIndex = 0
		while (i < n) {
			val vx: Float
			val vy: Float
			if (useSlotVertices) {
				vx = slotVertices[vIndex * 2]
				vy = slotVertices[vIndex * 2 + 1]
			} else {
				vx = vertices[vIndex * 2]
				vy = vertices[vIndex * 2 + 1]
			}
			worldVertices[i + positionOffset + 0] = vx * m00 + vy * m01 + x
			worldVertices[i + positionOffset + 1] = vx * m10 + vy * m11 + y

			worldVertices[i + colorOffset + 0] = color.r
			worldVertices[i + colorOffset + 1] = color.g
			worldVertices[i + colorOffset + 2] = color.b
			worldVertices[i + colorOffset + 3] = color.a
			i += vertexSize
			vIndex++
		}
		return worldVertices
	}

	override fun shouldApplyFfd(sourceAttachment: SkinAttachment): Boolean {
		//		return this === sourceAttachment || inheritFfd && parentMesh === sourceAttachment
		return this === sourceAttachment // TODO: nb tmp
	}
}
