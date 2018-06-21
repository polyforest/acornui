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

import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SpineEvent
import com.esotericsoftware.spine.animation.Animation
import com.esotericsoftware.spine.data.SpineEventDefaults
import com.esotericsoftware.spine.data.animation.timeline.EventTimelineData


class EventTimeline(
		defaults: Map<String, SpineEventDefaults>,
		data: EventTimelineData
) : Timeline {

	val frames: FloatArray // time, ...
	val events: Array<SpineEvent>

	init {
		frames = FloatArray(data.frames.size) {
			data.frames[it].time
		}
		events = Array(data.frames.size) {
			val frame = data.frames[it]
			SpineEvent(frame, defaults[frame.name]!!)
		}
	}

	/**
	 * Fires events for frames > lastTime and <= time.
	 */
	override fun apply(skeleton: Skeleton, lastTime: Float, time: Float, events: MutableList<SpineEvent>?, alpha: Float) {
		if (events == null) return
		var lastTime = lastTime
		val frames = this.frames
		val frameCount = frames.size

		if (lastTime > time) {
			// Fire events after last time for looped animations.
			apply(skeleton, lastTime, Int.MAX_VALUE.toFloat(), events, alpha)
			lastTime = -1f
		} else if (lastTime >= frames[frameCount - 1])
		// Last time is after last frame.
			return
		if (time < frames[0]) return  // Time is before first frame.

		var frameIndex: Int
		if (lastTime < frames[0])
			frameIndex = 0
		else {
			frameIndex = Animation.binarySearch(frames, lastTime)
			val frame = frames[frameIndex]
			while (frameIndex > 0) {
				// Fire multiple events with the same frame.
				if (frames[frameIndex - 1] != frame) break
				frameIndex--
			}
		}
		while (frameIndex < frameCount && time >= frames[frameIndex]) {
			events.add(this.events[frameIndex])
			frameIndex++
		}
	}
}