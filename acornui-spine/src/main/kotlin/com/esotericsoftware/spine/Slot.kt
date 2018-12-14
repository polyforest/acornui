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

import com.acornui.graphic.Color
import com.esotericsoftware.spine.attachments.SkinAttachment
import com.esotericsoftware.spine.data.SlotData


class Slot {
	val data: SlotData
	val bone: Bone
	val color: Color = Color.WHITE.copy()

	private var _attachment: SkinAttachment? = null

	/**
	 * Sets the attachment and if it changed, resets [.getAttachmentTime] and clears [.getAttachmentVertices].
	 */
	var attachment: SkinAttachment?
		get() = _attachment
		set(attachment) {
			if (_attachment == attachment) return
			_attachment = attachment
			attachmentTime = bone.skeleton.time
			attachmentVertices.clear()
		}

	var attachmentTime: Float = 0f

	val attachmentVertices = ArrayList<Float>()

	constructor(data: SlotData, bone: Bone) {
		this.data = data
		this.bone = bone
		setToSetupPose()
	}

	/** Copy constructor.  */
	constructor(slot: Slot, bone: Bone) {
		data = slot.data
		this.bone = bone
		color.set(slot.color)
		attachment = slot.attachment
		attachmentTime = slot.attachmentTime
	}

	val skeleton: Skeleton
		get() = bone.skeleton

	fun setElapsedAttachmentTime(time: Float) {
		attachmentTime = bone.skeleton.time - time
	}

	/** Returns the time since the attachment was set.  */
	fun getElapsedAttachmentTime(): Float {
		return bone.skeleton.time - attachmentTime
	}

	fun setToSetupPose() {
		setToSetupPose(bone.skeleton.data.slots.indexOf(data))
	}

	internal fun setToSetupPose(slotIndex: Int) {
		color.set(data.color)
		if (data.attachmentName == null)
			attachment = null
		else {
			attachment = null
			attachment = bone.skeleton.getAttachment(slotIndex, data.attachmentName)
		}
	}

	override fun toString(): String {
		return data.name
	}
}
