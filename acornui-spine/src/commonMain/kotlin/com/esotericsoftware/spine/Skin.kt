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

import com.esotericsoftware.spine.attachments.*
import com.esotericsoftware.spine.data.SkinAttachmentType
import com.esotericsoftware.spine.data.SkinData
import com.esotericsoftware.spine.data.attachments.BoundingBoxAttachmentData
import com.esotericsoftware.spine.data.attachments.MeshAttachmentData
import com.esotericsoftware.spine.data.attachments.RegionAttachmentData
import com.esotericsoftware.spine.data.attachments.WeightedMeshAttachmentData


/**
 * Stores attachments by slot and attachment name pairs.
 */
class Skin(
		val skeleton: Skeleton,
		val data: SkinData
) {

	private val attachments: HashMap<SkinAttachmentKey, SkinAttachment> = HashMap()

	init {
		for (i in data.attachments) {
			val key = SkinAttachmentKey(skeleton.findSlotIndex(i.key.slotName), i.key.attachmentName)
			attachments[key] = when (i.value.type) {
				SkinAttachmentType.REGION -> {
					val (page, region) = skeleton.atlas.findRegion(i.key.attachmentName) ?: throw Exception("Could not find region with name ${i.key.attachmentName}")
					RegionAttachment(i.value as RegionAttachmentData, page, region)
				}
				SkinAttachmentType.BOUNDINGBOX -> {
					BoundingBoxAttachment(i.value as BoundingBoxAttachmentData)
				}
				SkinAttachmentType.MESH -> {
					val (page, region) = skeleton.atlas.findRegion(i.key.attachmentName) ?: throw Exception("Could not find region with name ${i.key.attachmentName}")
					MeshAttachment(i.value as MeshAttachmentData, page, region)
				}
				SkinAttachmentType.WEIGHTEDMESH -> {
					val (page, region) = skeleton.atlas.findRegion(i.key.attachmentName) ?: throw Exception("Could not find region with name ${i.key.attachmentName}")
					WeightedMeshAttachment(i.value as WeightedMeshAttachmentData, page, region)
				}
			}
		}
	}

	fun getAttachment(slotIndex: Int, name: String): SkinAttachment? {
		lookup.setData(slotIndex, name)
		return attachments[lookup]
	}

	/**
	 * Attach each attachment in this skin if the corresponding attachment in the old skin is currently attached.
	 */
	internal fun attachAll(skeleton: Skeleton, oldSkin: Skin) {
		for (entry in oldSkin.attachments.entries) {
			val slotIndex = entry.key.slotIndex
			val slot = skeleton.slots[slotIndex]
			if (slot.attachment === entry.value) {
				val attachment = getAttachment(slotIndex, entry.key.attachmentName)
				if (attachment != null) slot.attachment = attachment
			}
		}
	}

	companion object {
		private val lookup = SkinAttachmentKey(0, "")
	}
}

data class SkinAttachmentKey(
		var slotIndex: Int,
		var attachmentName: String
) {

	fun setData(slotIndex: Int, name: String) {
		this.slotIndex = slotIndex
		this.attachmentName = name
	}
}