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

import com.esotericsoftware.spine.data.animation.timeline.CurveType
import com.esotericsoftware.spine.data.animation.timeline.CurvedTimelineData

/**
 * Base class for frames that use an interpolation bezier curve.
 */
abstract class CurveTimeline(data: CurvedTimelineData) : Timeline {

	private val curves: FloatArray // type, x, y, ...

	val frameCount: Int

	init {
		frameCount = data.frames.size
		if (frameCount <= 0) throw IllegalArgumentException("frameCount must be > 0: " + frameCount)
		curves = FloatArray(frameCount * BEZIER_SIZE)
		for (i in 0..data.frames.lastIndex) {
			val frame = data.frames[i]
			when (frame.curve.curveType) {
				CurveType.STEPPED -> setStepped(i)
				CurveType.LINEAR -> setLinear(i)
				CurveType.BEZIER -> {
					val b = frame.curve.bezier!!
					setCurve(i, b.cx1, b.cy1, b.cx2, b.cy2)
				}
			}
		}
	}

	fun setLinear(frameIndex: Int) {
		curves[frameIndex * BEZIER_SIZE] = LINEAR
	}

	fun setStepped(frameIndex: Int) {
		curves[frameIndex * BEZIER_SIZE] = STEPPED
	}

	fun getCurveType(frameIndex: Int): Float {
		val index = frameIndex * BEZIER_SIZE
		if (index == curves.size) return LINEAR
		val type = curves[index]
		if (type == LINEAR) return LINEAR
		if (type == STEPPED) return STEPPED
		return BEZIER
	}

	/**
	 * Sets the control handle positions for an interpolation bezier curve used to transition from this keyframe to the next.
	 * cx1 and cx2 are from 0 to 1, representing the percent of time between the two keyframes. cy1 and cy2 are the percent of
	 * the difference between the keyframe's values.
	 */
	fun setCurve(frameIndex: Int, cx1: Float, cy1: Float, cx2: Float, cy2: Float) {
		val subdiv1 = 1f / BEZIER_SEGMENTS
		val subdiv2 = subdiv1 * subdiv1
		val subdiv3 = subdiv2 * subdiv1
		val pre1 = 3 * subdiv1
		val pre2 = 3 * subdiv2
		val pre4 = 6 * subdiv2
		val pre5 = 6 * subdiv3
		val tmp1x = -cx1 * 2 + cx2
		val tmp1y = -cy1 * 2 + cy2
		val tmp2x = (cx1 - cx2) * 3 + 1
		val tmp2y = (cy1 - cy2) * 3 + 1
		var dfx = cx1 * pre1 + tmp1x * pre2 + tmp2x * subdiv3
		var dfy = cy1 * pre1 + tmp1y * pre2 + tmp2y * subdiv3
		var ddfx = tmp1x * pre4 + tmp2x * pre5
		var ddfy = tmp1y * pre4 + tmp2y * pre5
		val dddfx = tmp2x * pre5
		val dddfy = tmp2y * pre5

		var i = frameIndex * BEZIER_SIZE
		val curves = this.curves
		curves[i++] = BEZIER

		var x = dfx
		var y = dfy
		val n = i + BEZIER_SIZE - 1
		while (i < n) {
			curves[i] = x
			curves[i + 1] = y
			dfx += ddfx
			dfy += ddfy
			ddfx += dddfx
			ddfy += dddfy
			x += dfx
			y += dfy
			i += 2
		}
	}

	fun getCurvePercent(frameIndex: Int, percent: Float): Float {
		val curves = this.curves
		var i = frameIndex * BEZIER_SIZE
		val type = curves[i]
		if (type == LINEAR) return percent
		if (type == STEPPED) return 0f
		i++
		var x = 0f
		val start = i
		val n = i + BEZIER_SIZE - 1
		while (i < n) {
			x = curves[i]
			if (x >= percent) {
				val prevX: Float
				val prevY: Float
				if (i == start) {
					prevX = 0f
					prevY = 0f
				} else {
					prevX = curves[i - 2]
					prevY = curves[i - 1]
				}
				return prevY + (curves[i + 1] - prevY) * (percent - prevX) / (x - prevX)
			}
			i += 2
		}
		val y = curves[i - 1]
		return y + (1 - y) * (percent - x) / (1 - x) // Last point is 1,1.
	}

	companion object {
		val LINEAR = 0f
		val STEPPED = 1f
		val BEZIER = 2f
		private val BEZIER_SEGMENTS = 10
		private val BEZIER_SIZE = BEZIER_SEGMENTS * 2 - 1
	}
}