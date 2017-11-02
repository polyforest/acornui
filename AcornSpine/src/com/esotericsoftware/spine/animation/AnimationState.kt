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

import com.acornui.collection.ClearableObjectPool
import com.acornui.collection.Clearable
import com.acornui.collection.fill
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SpineEvent


/**
 * Stores state for an animation and automatically mixes between animations.
 */
class AnimationState(
		private val skeleton: Skeleton,
		val data: AnimationStateData
) {

	/**
	 * Returns the list of tracks that have animations, which may contain nulls.
	 */
	private val tracks: MutableList<AnimationTrackEntry?> = ArrayList()

	private val events = ArrayList<SpineEvent>()
	private val listeners = ArrayList<AnimationStateListener>()

	var timeScale = 1f

	private val trackEntryPool = ClearableObjectPool { AnimationTrackEntry() }

	fun update(delta: Float) {
		val delta = delta * timeScale
		for (i in 0..tracks.lastIndex) {
			var current: AnimationTrackEntry = tracks[i] ?: continue

			val next = current.next
			if (next != null) {
				val nextTime = current.lastTime - next.delay
				if (nextTime >= 0) {
					val nextDelta = delta * next.timeScale
					next.time = nextTime + nextDelta // For start event to see correct time.
					current.time += delta * current.timeScale // For end event to see correct time.
					setCurrent(i, next)
					next.time -= nextDelta // Prevent increasing time twice, below.
					current = next
				}
			} else if (!current.loop && current.lastTime >= current.endTime) {
				// End non-looping animation when it reaches its end time and there is no next entry.
				clearTrack(i)
				continue
			}

			current.time += delta * current.timeScale
			if (current.previous != null) {
				val previousDelta = delta * current.previous!!.timeScale
				current.previous!!.time += previousDelta
				current.mixTime += previousDelta
			}
		}
	}

	fun apply(skeleton: Skeleton) {
		val events = this.events
		val listenerCount = listeners.size

		for (i in 0..tracks.lastIndex) {
			val current = tracks[i] ?: continue

			events.clear()

			var time = current.time
			val lastTime = current.lastTime
			val endTime = current.endTime
			val loop = current.loop
			if (!loop && time > endTime) time = endTime

			val previous = current.previous
			if (previous == null)
				current.animation!!.apply(skeleton, lastTime, time, loop, events, current.mix)
			else {
				var previousTime = previous.time
				if (!previous.loop && previousTime > previous.endTime) previousTime = previous.endTime
				previous.animation!!.apply(skeleton, previousTime, previousTime, previous.loop, null, alpha = 1f)

				var alpha = current.mixTime / current.mixDuration * current.mix
				if (alpha >= 1) {
					alpha = 1f
					trackEntryPool.free(previous)
					current.previous = null
				}
				current.animation!!.apply(skeleton, lastTime, time, loop, events, alpha)
			}

			run {
				for (ii in 0..events.lastIndex) {
					val event = events[ii]
					current.listener?.event(i, event)
					for (iii in 0..listenerCount - 1)
						listeners[iii].event(i, event)
				}
			}

			// Check if completed the animation or a loop iteration.
			if (if (loop) lastTime % endTime > time % endTime else lastTime < endTime && time >= endTime) {
				val count = (time / endTime).toInt()
				if (current.listener != null) current.listener!!.complete(i, count)
				for (ii in 0..listeners.lastIndex) {
					listeners[ii].complete(i, count)
				}
			}

			current.lastTime = current.time
		}
	}

	fun clearTracks() {
		for (i in 0..tracks.lastIndex) {
			clearTrack(i)
		}
		tracks.clear()
	}

	fun clearTrack(trackIndex: Int) {
		if (trackIndex >= tracks.size) return
		val current = tracks[trackIndex] ?: return

		if (current.listener != null) current.listener!!.end(trackIndex)
		for (i in 0..listeners.lastIndex) {
			listeners[i].end(trackIndex)
		}

		tracks[trackIndex] = null

		freeAll(current)
		if (current.previous != null) trackEntryPool.free(current.previous!!)
	}

	private fun freeAll(entry: AnimationTrackEntry?) {
		var i = entry
		while (i != null) {
			val next = i.next
			trackEntryPool.free(i)
			i = next
		}
	}

	private fun expandToIndex(index: Int): AnimationTrackEntry? {
		if (index < tracks.size)
			return tracks[index]
		tracks.fill(index + 1) { null }
		return null
	}

	private fun setCurrent(index: Int, entry: AnimationTrackEntry) {
		val current = expandToIndex(index)
		if (current != null) {
			var previous = current.previous
			current.previous = null

			if (current.listener != null) current.listener!!.end(index)
			for (i in 0..listeners.lastIndex) {
				listeners[i].end(index)
			}

			entry.mixDuration = data.getMix(current.animation!!.data, entry.animation!!.data)
			if (entry.mixDuration > 0) {
				entry.mixTime = 0f
				// If a mix is in progress, mix from the closest animation.
				if (previous != null && current.mixTime / current.mixDuration < 0.5f) {
					entry.previous = previous
					previous = current
				} else
					entry.previous = current
			} else
				trackEntryPool.free(current)

			if (previous != null) trackEntryPool.free(previous)
		}

		tracks[index] = entry

		if (entry.listener != null) entry.listener!!.start(index)
		for (i in 0..listeners.lastIndex) {
			listeners[i].start(index)
		}
	}

	fun setAnimation(trackIndex: Int, animationName: String, loop: Boolean = false): AnimationTrackEntry {
		val animation = skeleton.findAnimation(animationName) ?: throw IllegalArgumentException("Animation not found: $animationName")
		return setAnimation(trackIndex, animation, loop)
	}

	/** Set the current animation. Any queued animations are cleared.  */
	fun setAnimation(trackIndex: Int, animation: Animation, loop: Boolean): AnimationTrackEntry {
		val current = expandToIndex(trackIndex)
		if (current != null) freeAll(current.next)

		val entry = trackEntryPool.obtain()
		entry.animation = animation
		entry.loop = loop
		entry.endTime = animation.duration
		setCurrent(trackIndex, entry)
		return entry
	}

	/** [.addAnimation]  */
	fun addAnimation(trackIndex: Int, animationName: String, loop: Boolean = false, delay: Float = 0f): AnimationTrackEntry {
		val animation = skeleton.findAnimation(animationName) ?: throw IllegalArgumentException("AnimationInstance not found: " + animationName)
		return addAnimation(trackIndex, animation, loop, delay)
	}

	/** Adds an animation to be played delay seconds after the current or last queued animation.
	 * @param delay May be <= 0 to use duration of previous animation minus any mix duration plus the negative delay.
	 */
	fun addAnimation(trackIndex: Int, animation: Animation, loop: Boolean = false, delay: Float = 0f): AnimationTrackEntry {
		var delay = delay
		val entry = trackEntryPool.obtain()
		entry.animation = animation
		entry.loop = loop
		entry.endTime = animation.duration

		var last = expandToIndex(trackIndex)
		if (last != null) {
			while (last!!.next != null)
				last = last.next
			last.next = entry
		} else
			tracks[trackIndex] = entry

		if (delay <= 0) {
			if (last != null)
				delay += last.endTime - data.getMix(last.animation!!.data, animation.data)
			else
				delay = 0f
		}
		entry.delay = delay

		return entry
	}

	fun getCurrent(trackIndex: Int): AnimationTrackEntry? {
		if (trackIndex >= tracks.size) return null
		return tracks[trackIndex]
	}

	/**
	 * Adds a listener to receive events for all animations.
	 */
	fun addListener(listener: AnimationStateListener) {
		listeners.add(listener)
	}

	/**
	 * Removes the listener added with [.addListener].
	 */
	fun removeListener(listener: AnimationStateListener) {
		listeners.remove(listener)
	}

	fun clearListeners() {
		listeners.clear()
	}

}

class AnimationTrackEntry : Clearable {
	var next: AnimationTrackEntry? = null
	internal var previous: AnimationTrackEntry? = null
	var animation: Animation? = null
	var loop: Boolean = false
	var delay: Float = 0f
	var time: Float = 0f
	var lastTime = -1f
	var endTime: Float = 0f
	var timeScale = 1f
	internal var mixTime: Float = 0f
	internal var mixDuration: Float = 0f
	var listener: AnimationStateListener? = null
	var mix = 1f

	override fun clear() {
		next = null
		previous = null
		animation = null
		listener = null
		timeScale = 1f
		lastTime = -1f // Trigger events on frame zero.
		time = 0f
	}

	/** Returns true if the current time is greater than the end time, regardless of looping.  */
	val isComplete: Boolean
		get() = time >= endTime
}


interface AnimationStateListener {

	/**
	 * Invoked when the current animation triggers an event.
	 */
	fun event(trackIndex: Int, event: SpineEvent) {
	}

	/**
	 * Invoked when the current animation has completed.
	 * @param loopCount The number of times the animation reached the end.
	 */
	fun complete(trackIndex: Int, loopCount: Int) {
	}

	/**
	 * Invoked just after the current animation is set.
	 */
	fun start(trackIndex: Int) {
	}

	/**
	 * Invoked just before the current animation is replaced.
	 */
	fun end(trackIndex: Int) {
	}
}

abstract class AnimationStateAdapter : AnimationStateListener {
	override fun event(trackIndex: Int, event: SpineEvent) {
	}

	override fun complete(trackIndex: Int, loopCount: Int) {
	}

	override fun start(trackIndex: Int) {
	}

	override fun end(trackIndex: Int) {
	}
}