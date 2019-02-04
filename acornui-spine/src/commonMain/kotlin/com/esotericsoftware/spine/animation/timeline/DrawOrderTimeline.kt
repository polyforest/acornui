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

package com.esotericsoftware.spine.animation.timeline

import com.acornui.collection.arrayCopy
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SpineEvent
import com.esotericsoftware.spine.animation.Animation
import com.esotericsoftware.spine.data.animation.timeline.DrawOrderTimelineData


class DrawOrderTimeline(data: DrawOrderTimelineData) : Timeline {

	val frames: FloatArray // time, ...
	val drawOrders: Array<IntArray?>

	init {
		frames = FloatArray(data.frames.size) { data.frames[it].time }
		drawOrders = Array<IntArray?>(data.frames.size) {
			null
		}
// TODO: nb
		//		// Draw order timeline.
		//			val slotCount = skeletonData.slots.size
		//			var frameIndex = 0
		//			var drawOrderMap = drawOrdersMap!!.child
		//			while (drawOrderMap != null) {
		//				var drawOrder: IntArray? = null
		//				val offsets = drawOrderMap!!.get("offsets")
		//				if (offsets != null) {
		//					drawOrder = IntArray(slotCount)
		//					for (i in slotCount - 1 downTo 0)
		//						drawOrder[i] = -1
		//					val unchanged = IntArray(slotCount - offsets!!.size)
		//					var originalIndex = 0
		//					var unchangedIndex = 0
		//					var offsetMap = offsets!!.child
		//					while (offsetMap != null) {
		//						val slotIndex = skeletonData.findSlotIndex(offsetMap!!.getString("slot"))
		//						if (slotIndex == -1) throw Exception("Slot not found: " + offsetMap!!.getString("slot"))
		//						// Collect unchanged items.
		//						while (originalIndex != slotIndex)
		//							unchanged[unchangedIndex++] = originalIndex++
		//						// Set changed items.
		//						drawOrder[originalIndex + offsetMap!!.getInt("offset")] = originalIndex++
		//						offsetMap = offsetMap!!.next
		//					}
		//					// Collect remaining unchanged items.
		//					while (originalIndex < slotCount)
		//						unchanged[unchangedIndex++] = originalIndex++
		//					// Fill in unchanged items.
		//					for (i in slotCount - 1 downTo 0)
		//						if (drawOrder[i] == -1) drawOrder[i] = unchanged[--unchangedIndex]
		//				}
		//				timeline.setFrame(frameIndex++, drawOrderMap!!.getFloat("time"), drawOrder)
		//				drawOrderMap = drawOrderMap!!.next
		//			}
		//			timelines.add(timeline)
		//			duration = maxOf(duration, timeline.frames[timeline.frameCount - 1])
		//		}
	}

	override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: MutableList<SpineEvent>?, alpha: Float) {
		val frames = this.frames
		if (time < frames[0]) return  // Time is before first frame.

		val frameIndex: Int
		if (time >= frames[frames.size - 1])
		// Time is after last frame.
			frameIndex = frames.size - 1
		else
			frameIndex = Animation.binarySearch(frames, time) - 1

		val drawOrder = skeleton.drawOrder
		val slots = skeleton.slots
		val drawOrderToSetupIndex = drawOrders[frameIndex]
		if (drawOrderToSetupIndex == null)
			arrayCopy(slots, 0, drawOrder, 0, slots.size)
		else {
			var i = 0
			val n = drawOrderToSetupIndex.size
			while (i < n) {
				drawOrder[i] = slots[drawOrderToSetupIndex[i]]
				i++
			}
		}
	}
}