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
import com.acornui.core.graphic.AtlasPageData
import com.acornui.core.graphic.AtlasRegionData
import com.acornui.graphic.Color
import com.acornui.math.MathUtils
import com.esotericsoftware.spine.Slot
import com.esotericsoftware.spine.component.SpineVertexUtils.colorOffset
import com.esotericsoftware.spine.component.SpineVertexUtils.getUPos
import com.esotericsoftware.spine.component.SpineVertexUtils.getVPos
import com.esotericsoftware.spine.component.SpineVertexUtils.getXPos
import com.esotericsoftware.spine.component.SpineVertexUtils.getYPos
import com.esotericsoftware.spine.component.SpineVertexUtils.vertexSize
import com.esotericsoftware.spine.data.attachments.RegionAttachmentData
import kotlin.math.cos
import kotlin.math.sin

/**
 * Attachment that displays a texture region.
 */
open class RegionAttachment(
		val data: RegionAttachmentData,
		val page: AtlasPageData,
		val region: AtlasRegionData) : SkinAttachment {

	private val color: Color = Color()

	// TL, TR, BR, BL
	private val worldVertices = ArrayList(vertexSize * 4) { 0f }
	private val offset = FloatArray(8)

	init {
		val page = this.page
		val vertices = this.worldVertices
		val bounds = region.bounds
		val pageW = page.width.toFloat()
		val pageH = page.height.toFloat()
		if (region.isRotated) {
			vertices[getUPos(0)] = bounds.right / pageW
			vertices[getVPos(0)] = bounds.y / pageH
			vertices[getUPos(1)] = bounds.right / pageW
			vertices[getVPos(1)] = bounds.bottom / pageH
			vertices[getUPos(2)] = bounds.x / pageW
			vertices[getVPos(2)] = bounds.bottom / pageH
			vertices[getUPos(3)] = bounds.x / pageW
			vertices[getVPos(3)] = bounds.y / pageH
		} else {
			vertices[getUPos(0)] = bounds.x / pageW
			vertices[getVPos(0)] = bounds.y / pageH
			vertices[getUPos(1)] = bounds.right / pageW
			vertices[getVPos(1)] = bounds.y / pageH
			vertices[getUPos(2)] = bounds.right / pageW
			vertices[getVPos(2)] = bounds.bottom / pageH
			vertices[getUPos(3)] = bounds.x / pageW
			vertices[getVPos(3)] = bounds.bottom / pageH
		}

		updateOffset()
	}

	private fun updateOffset() {
		val width = data.width
		val height = data.height
		var localX2 = width / 2
		var localY2 = height / 2
		var localX = -localX2
		var localY = -localY2

		// Account for stripped whitespace.
		val region = this.region
		val paddingLeft = region.padding[0].toFloat()
		val paddingBottom = region.padding[3].toFloat()

		localX += paddingLeft / region.originalWidth * width
		localY += paddingBottom / region.originalHeight * height
		localX2 -= (region.originalWidth - paddingLeft - region.packedWidth) / region.originalWidth * width
		localY2 -= (region.originalHeight - paddingBottom - region.packedHeight) / region.originalHeight * height

		val scaleX = data.scaleX
		val scaleY = data.scaleY
		localX *= scaleX
		localY *= scaleY
		localX2 *= scaleX
		localY2 *= scaleY
		val rotation = data.rotation
		val cos = cos(rotation * MathUtils.degRad)
		val sin = sin(rotation * MathUtils.degRad)
		val x = data.x
		val y = data.y
		val localXCos = localX * cos + x
		val localXSin = localX * sin
		val localYCos = localY * cos + y
		val localYSin = localY * sin
		val localX2Cos = localX2 * cos + x
		val localX2Sin = localX2 * sin
		val localY2Cos = localY2 * cos + y
		val localY2Sin = localY2 * sin
		val offset = this.offset
		offset[BLX] = localXCos - localYSin
		offset[BLY] = localYCos + localXSin
		offset[ULX] = localXCos - localY2Sin
		offset[ULY] = localY2Cos + localXSin
		offset[URX] = localX2Cos - localY2Sin
		offset[URY] = localY2Cos + localX2Sin
		offset[BRX] = localX2Cos - localYSin
		offset[BRY] = localYCos + localX2Sin
	}

	/**
	 * @return The updated world vertices.
	 */
	open fun updateWorldVertices(slot: Slot): List<Float> {
		val skeleton = slot.skeleton

		val vertices = this.worldVertices
		val offset = this.offset
		val bone = slot.bone
		val x = skeleton.x + bone.worldX
		val y = skeleton.y + bone.worldY
		val m00 = bone.a
		val m01 = bone.b
		val m10 = bone.c
		val m11 = bone.d
		var offsetX: Float
		var offsetY: Float

		offsetX = offset[ULX]
		offsetY = offset[ULY]
		vertices[getXPos(0)] = offsetX * m00 + offsetY * m01 + x // ul
		vertices[getYPos(0)] = offsetX * m10 + offsetY * m11 + y

		offsetX = offset[URX]
		offsetY = offset[URY]
		vertices[getXPos(1)] = offsetX * m00 + offsetY * m01 + x // ur
		vertices[getYPos(1)] = offsetX * m10 + offsetY * m11 + y

		offsetX = offset[BRX]
		offsetY = offset[BRY]
		vertices[getXPos(2)] = offsetX * m00 + offsetY * m01 + x // br
		vertices[getYPos(2)] = offsetX * m10 + offsetY * m11 + y

		offsetX = offset[BLX]
		offsetY = offset[BLY]
		vertices[getXPos(3)] = offsetX * m00 + offsetY * m01 + x // bl
		vertices[getYPos(3)] = offsetX * m10 + offsetY * m11 + y

		run {
			// Update color
			color.set(skeleton.color).mul(slot.color).mul(data.color)
			var i = colorOffset
			while (i < vertices.size) {
				vertices[i + 0] = color.r
				vertices[i + 1] = color.g
				vertices[i + 2] = color.b
				vertices[i + 3] = color.a
				i += vertexSize
			}
		}

		return vertices
	}

	companion object {
		const val BLX = 0
		const val BLY = 1
		const val ULX = 2
		const val ULY = 3
		const val URX = 4
		const val URY = 5
		const val BRX = 6
		const val BRY = 7
	}
}
