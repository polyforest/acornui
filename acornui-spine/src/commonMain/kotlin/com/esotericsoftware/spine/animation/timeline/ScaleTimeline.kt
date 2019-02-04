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
import com.esotericsoftware.spine.data.animation.timeline.ScaleTimelineData


class ScaleTimeline(
		val boneIndex: Int,
		val data: ScaleTimelineData
) : CurveTimeline(data) {

	val frames: FloatArray // time, x, y, ...

	init {
		frames = FloatArray(data.frames.size * 3)
		for (i in 0..data.frames.lastIndex) {
			val frame = data.frames[i]
			val frameIndex = i * 3
			frames[frameIndex + 0] = frame.time
			frames[frameIndex + 1] = frame.scaleX
			frames[frameIndex + 2] = frame.scaleY
		}
	}

	override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: MutableList<SpineEvent>?, alpha: Float) {
		val frames = this.frames
		if (time < frames[0]) return  // Time is before first frame.

		val bone = skeleton.bones[boneIndex]
		if (time >= frames[frames.size - 3]) {
			// Time is after last frame.
			bone.scaleX += (bone.data.scaleX * frames[frames.size - 2] - bone.scaleX) * alpha
			bone.scaleY += (bone.data.scaleY * frames[frames.size - 1] - bone.scaleY) * alpha
			return
		}

		// Interpolate between the previous frame and the current frame.
		val frameIndex = Animation.binarySearch(frames, time, 3)
		val prevFrameX = frames[frameIndex - 2]
		val prevFrameY = frames[frameIndex - 1]
		val frameTime = frames[frameIndex]
		var percent = MathUtils.clamp(1f - (time - frameTime) / (frames[frameIndex + PREV_FRAME_TIME] - frameTime), 0f, 1f)
		percent = getCurvePercent(frameIndex / 3 - 1, percent)

		bone.scaleX += (bone.data.scaleX * (prevFrameX + (frames[frameIndex + FRAME_X] - prevFrameX) * percent) - bone.scaleX) * alpha
		bone.scaleY += (bone.data.scaleY * (prevFrameY + (frames[frameIndex + FRAME_Y] - prevFrameY) * percent) - bone.scaleY) * alpha
	}

	companion object {
		internal val PREV_FRAME_TIME = -3
		internal val FRAME_X = 1
		internal val FRAME_Y = 2
	}
}