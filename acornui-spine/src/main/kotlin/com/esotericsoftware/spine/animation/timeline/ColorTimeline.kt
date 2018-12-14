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

import com.acornui.math.MathUtils
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SpineEvent
import com.esotericsoftware.spine.animation.Animation
import com.esotericsoftware.spine.data.animation.timeline.ColorTimelineData


class ColorTimeline(
		private val slotIndex: Int = 0,
		private val data: ColorTimelineData
) : CurveTimeline(data) {

	private val frames: FloatArray // time, r, g, b, a, ...

	init {
		frames = FloatArray(frameCount * 5)
		for (i in 0..data.frames.lastIndex) {
			val frame = data.frames[i]
			val frameIndex = i * 5
			frames[frameIndex] = frame.time
			frames[frameIndex + 1] = frame.color.r
			frames[frameIndex + 2] = frame.color.g
			frames[frameIndex + 3] = frame.color.b
			frames[frameIndex + 4] = frame.color.a
		}
	}

	override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: MutableList<SpineEvent>?, alpha: Float) {
		val frames = this.frames
		if (time < frames[0]) return  // Time is before first frame.

		val r: Float
		val g: Float
		val b: Float
		val a: Float
		if (time >= frames[frames.size - 5]) {
			// Time is after last frame.
			val i = frames.size - 1
			r = frames[i - 3]
			g = frames[i - 2]
			b = frames[i - 1]
			a = frames[i]
		} else {
			// Interpolate between the previous frame and the current frame.
			val frameIndex = Animation.binarySearch(frames, time, 5)
			val prevFrameR = frames[frameIndex - 4]
			val prevFrameG = frames[frameIndex - 3]
			val prevFrameB = frames[frameIndex - 2]
			val prevFrameA = frames[frameIndex - 1]
			val frameTime = frames[frameIndex]
			var percent = MathUtils.clamp(1f - (time - frameTime) / (frames[frameIndex + PREV_FRAME_TIME] - frameTime), 0f, 1f)
			percent = getCurvePercent(frameIndex / 5 - 1, percent)

			r = prevFrameR + (frames[frameIndex + FRAME_R] - prevFrameR) * percent
			g = prevFrameG + (frames[frameIndex + FRAME_G] - prevFrameG) * percent
			b = prevFrameB + (frames[frameIndex + FRAME_B] - prevFrameB) * percent
			a = prevFrameA + (frames[frameIndex + FRAME_A] - prevFrameA) * percent
		}
		val color = skeleton.slots[slotIndex].color
		if (alpha < 1)
			color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha)
		else
			color.set(r, g, b, a)
	}

	companion object {
		private val PREV_FRAME_TIME = -5
		private val FRAME_R = 1
		private val FRAME_G = 2
		private val FRAME_B = 3
		private val FRAME_A = 4
	}
}