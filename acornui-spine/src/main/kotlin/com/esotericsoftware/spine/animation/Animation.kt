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

package com.esotericsoftware.spine.animation

import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SpineEvent
import com.esotericsoftware.spine.animation.timeline.*
import com.esotericsoftware.spine.data.animation.AnimationData
import com.esotericsoftware.spine.data.animation.timeline.*


class Animation(
		val skeleton: Skeleton,
		val data: AnimationData
) {

	val duration = data.duration

	private val timelines = ArrayList<Timeline>()

	init {
		for ((key, list) in data.slotTimelines) {
			val slotIndex = skeleton.findSlotIndex(key)
			for (value in list) {
				if (value is AttachmentTimelineData) {
					timelines.add(AttachmentTimeline(slotIndex, value))
				} else if (value is ColorTimelineData) {
					timelines.add(ColorTimeline(slotIndex, value))
				}
			}
		}
		for ((key, list) in data.boneTimelines) {
			val boneIndex = skeleton.findBoneIndex(key)
			for (value in list) {
				if (value is RotateTimelineData) {
					timelines.add(RotateTimeline(boneIndex, value))
				} else if (value is ScaleTimelineData) {
					timelines.add(ScaleTimeline(boneIndex, value))
				} else if (value is TranslateTimelineData) {
					timelines.add(TranslateTimeline(boneIndex, value))
				}
			}
		}
		for ((key, value) in data.ikConstraintTimelines) {
			val ikConstraintIndex = skeleton.findIkConstraintIndex(key)
			timelines.add(IkConstraintTimeline(ikConstraintIndex, value))
		}
		for ((key, value) in data.ffdTimelines) {
			val skin = skeleton.findSkin(key.skinName) ?: throw Exception("Skin not found: ${key.skinName}")
			val slotIndex = skeleton.findSlotIndex(key.slotName)
			val attachment = skin.getAttachment(slotIndex, key.attachmentName) ?: throw Exception("Skin attachment not found: ${key.attachmentName}")
			timelines.add(FfdTimeline(slotIndex, attachment, value))
		}
		if (data.drawOrderTimeline != null) timelines.add(DrawOrderTimeline(data.drawOrderTimeline))
		if (data.eventTimeline != null) timelines.add(EventTimeline(skeleton.data.events, data.eventTimeline))

	}

	/**
	 * Poses the skeleton at the specified time for this animation.
	 * @param lastTime The last time the animation was applied.
	 * @param events Any triggered events are added.
	 */
	fun apply(skeleton: Skeleton, lastTime: Float, time: Float, loop: Boolean, events: MutableList<SpineEvent>?, alpha: Float = 1f) {
		var lastTime = lastTime
		var time = time

		if (loop && duration != 0f) {
			time %= duration
			if (lastTime > 0)
				lastTime %= duration
		}

		val timelines = this.timelines
		for (i in 0..timelines.lastIndex) {
			timelines[i].apply(skeleton, lastTime, time, events, alpha)
		}
	}

	companion object {

		/**
		 * @param target After the first and before the last value.
		 * @return index of first value greater than the target.
		 */
		internal fun binarySearch(values: FloatArray, target: Float, step: Int): Int {
			var low = 0
			var high = values.size / step - 2
			if (high == 0) return step
			var current = high.ushr(1)
			while (true) {
				if (values[(current + 1) * step] <= target)
					low = current + 1
				else
					high = current
				if (low == high) return (low + 1) * step
				current = (low + high).ushr(1)
			}
		}

		/**
		 * @param target After the first and before the last value.
		 * @return index of first value greater than the target.
		 */
		internal fun binarySearch(values: FloatArray, target: Float): Int {
			var low = 0
			var high = values.size - 2
			if (high == 0) return 1
			var current = high.ushr(1)
			while (true) {
				if (values[current + 1] <= target)
					low = current + 1
				else
					high = current
				if (low == high) return low + 1
				current = (low + high).ushr(1)
			}
		}
	}
}
