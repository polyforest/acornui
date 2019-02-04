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
import com.acornui.collection.fill
import com.acornui.math.MathUtils
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SpineEvent
import com.esotericsoftware.spine.animation.Animation
import com.esotericsoftware.spine.attachments.FfdAttachment
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.attachments.SkinAttachment
import com.esotericsoftware.spine.attachments.WeightedMeshAttachment
import com.esotericsoftware.spine.data.animation.timeline.FfdTimelineData


class FfdTimeline(
		val slotIndex: Int,
		val attachment: SkinAttachment,

		data: FfdTimelineData
) : CurveTimeline(data) {

	val frames: FloatArray // time, ...
	val frameVertices: Array<FloatArray>

	init {
		frames = FloatArray(frameCount) {
			data.frames[it].time
		}

		val vertexCount: Int
		if (attachment is MeshAttachment) {
			vertexCount = attachment.data.vertices.size
		} else {
			vertexCount = (attachment as WeightedMeshAttachment).data.weights.size / 3 * 2
		}

		frameVertices = Array(frameCount) {
			val frame = data.frames[it]
			val frameVertices = frame.vertices
			val vertices: FloatArray

			if (frameVertices == null) {
				if (attachment is MeshAttachment) {
					vertices = attachment.data.vertices
				} else {
					attachment as WeightedMeshAttachment
					vertices = FloatArray(vertexCount) // 0f
				}
			} else {
				vertices = FloatArray(vertexCount)
				arrayCopy(frameVertices, 0, vertices, frame.offset, frameVertices.size)
				//
				if (attachment is MeshAttachment) {
					val meshVertices = attachment.data.vertices
					for (i in 0..vertexCount - 1)
						vertices[i] += meshVertices[i]
				}
			}
			vertices
		}
	}

	override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: MutableList<SpineEvent>?, alpha: Float) {
		var alpha = alpha
		val slot = skeleton.slots[slotIndex]
		val slotAttachment = slot.attachment
		if (slotAttachment !is FfdAttachment || !slotAttachment.shouldApplyFfd(attachment)) return

		val frames = this.frames
		if (time < frames[0]) return  // Time is before first frame.

		val frameVertices = this.frameVertices
		val vertexCount = frameVertices[0].size

		val slotVertices = slot.attachmentVertices
		if (slotVertices.size != vertexCount) alpha = 1f // Don't mix from uninitialized slot vertices.
		slotVertices.clear()
		slotVertices.fill(vertexCount) { 0f }

		if (time >= frames.last()) {
			// Time is after last frame.
			val lastVertices = frameVertices.last()

			if (alpha < 1) {
				for (i in 0..vertexCount - 1)
					slotVertices[i] += (lastVertices[i] - slotVertices[i]) * alpha
			} else {
				for (i in 0..vertexCount - 1)
					slotVertices[i] = lastVertices[i]
			}

			return
		}

		// Interpolate between the previous frame and the current frame.
		val frameIndex = Animation.binarySearch(frames, time)
		val frameTime = frames[frameIndex]
		var percent = MathUtils.clamp(1f - (time - frameTime) / (frames[frameIndex - 1] - frameTime), 0f, 1f)
		percent = getCurvePercent(frameIndex - 1, percent)

		val prevVertices = frameVertices[frameIndex - 1]
		val nextVertices = frameVertices[frameIndex]

		if (alpha < 1) {
			for (i in 0..vertexCount - 1) {
				val prev = prevVertices[i]
				slotVertices[i] += (prev + (nextVertices[i] - prev) * percent - slotVertices[i]) * alpha
			}
		} else {
			for (i in 0..vertexCount - 1) {
				val prev = prevVertices[i]
				slotVertices[i] = prev + (nextVertices[i] - prev) * percent
			}
		}
	}
}