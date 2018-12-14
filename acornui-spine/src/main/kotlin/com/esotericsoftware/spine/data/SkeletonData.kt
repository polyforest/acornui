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

package com.esotericsoftware.spine.data

import com.esotericsoftware.spine.data.animation.AnimationData

@Suppress("ArrayInDataClass")
data class SkeletonData(

		var name: String?,

		var width: Float,
		var height: Float,

		val version: String?,

		val imagesPath: String?,
		val animations: Map<String, AnimationData>,
		val bones: Array<BoneData>,
		val events: Map<String, SpineEventDefaults>,
		val ikConstraints: Array<IkConstraintData>,
		val skins: Map<String, SkinData>,

		val slots: Array<SlotData>,
		val transformConstraints: Array<TransformConstraintData>,
		var hash: String? = null
) {

	init {
	}

	fun findBone(boneName: String): BoneData? {
		val bones = this.bones
		var i = 0
		val n = bones.size
		while (i < n) {
			val bone = bones[i]
			if (bone.name == boneName) return bone
			i++
		}
		return null
	}

	/**
	 * @return -1 if the bone was not found.
	 */
	fun findBoneIndex(boneName: String): Int {
		return slots.indexOfFirst { it.name == boneName }
	}

	fun findSlot(slotName: String): SlotData? {
		return slots.find { it.name == slotName }
	}

	/**
	 * @return -1 if the slot was not found.
	 */
	fun findSlotIndex(slotName: String): Int {
		return slots.indexOfFirst { it.name == slotName }
	}

	fun findSkin(skinName: String): SkinData? {
		return skins[skinName]
	}

	fun findEvent(eventName: String): SpineEventDefaults? {
		return events[eventName]
	}

	fun findAnimation(animationName: String): AnimationData? {
		return animations[animationName]
	}

	fun findIkConstraint(constraintName: String): IkConstraintData? {
		return ikConstraints.find { it.name == constraintName }
	}

	fun findTransformConstraint(constraintName: String): TransformConstraintData? {
		return transformConstraints.find { it.name == constraintName }
	}

}