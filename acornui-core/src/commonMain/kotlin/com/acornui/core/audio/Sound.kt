/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.core.audio

import com.acornui.core.Disposable
import com.acornui.math.MathUtils
import com.acornui.math.PI
import com.acornui.signal.Signal
import kotlin.math.cos
import kotlin.math.sin

interface SoundFactory : Disposable {

	/**
	 * New sounds from this sound source will be created with this priority unless explicitly specified with
	 * [createInstance]
	 */
	var defaultPriority: Float

	/**
	 * The duration of the sound, in seconds.
	 */
	val duration: Float

	/**
	 * Creates an in-memory audio clip instance.
	 * Returns null if the audio manager is already at-capacity with sounds that all have higher priority.
	 */
	fun createInstance(): Sound? = createInstance(defaultPriority)
	fun createInstance(priority: Float): Sound?
}

/**
 * An interface for controlling an in-memory sound clip.
 */
interface Sound : Disposable {

	/**
	 * This sound's priority. The [AudioManager.simultaneousSounds] property limits how many sounds can
	 * be simultaneously played. When the limit is reached higher priority sounds will stop sounds with lower
	 * priority values.
	 */
	val priority: Float

	var onCompleted: (()->Unit)?

	var loop: Boolean

	var volume: Float

	/**
	 * Sets the audio clip's 3d position.
	 * The listener is always located at 0f, 0f, 0f, facing 0f, 0f, 1f
	 * Note, this does not work on all systems with stereo audio clips. Use mono for 3d sounds.
	 */
	fun setPosition(x: Float, y: Float, z: Float)

	/**
	 * Immediately starts this sound. Sounds are fire and forget, which means that when a sound has finished
	 * (its [onCompleted] handler will be called, there should no longer be any references to this sound. It may
	 * be recycled at that point.
	 */
	fun start()

	/**
	 * Immediately stops this sound.
	 * When a sound is stopped, [onCompleted] will be invoked and the sound will no longer be active.
	 * Sound instances are fire-and-forget; they cannot be restarted.
	 */
	fun stop()

	/**
	 * The current playback time (in seconds). This can only be read. To change the seek position of a sound,
	 * stop the sound, create a new sound from the [SoundFactory], then start the new sound using the new start time.
	 *
	 * Note: this will not be accurate enough to do seamless stitching.
	 */
	val currentTime: Float

	/**
	 * Returns true if the sound is currently playing.
	 * Note: Sounds cannot be paused, to simulate pausing, store the [currentTime], then create a new sound source,
	 * starting with the previous currentTime as the new startTime.
	 */
	val isPlaying: Boolean

	/**
	 * Updates this object.
	 */
	fun update()
}

/**
 * An interface for controlling a progressive-download music file.
 */
interface Music : Disposable {

	/**
	 * Dispatched when the music has completed playing.
	 */
	var onCompleted: (()->Unit)?

	val duration: Float

	val readyStateChanged: Signal<()->Unit>
	val readyState: MusicReadyState

	/**
	 * Returns true if this music is playing, or false if it's stopped or paused.
	 */
	val isPlaying: Boolean

	/**
	 * Returns true if the music is not playing and is not at current time 0f
	 */
	val isPaused: Boolean
		get() = (!isPlaying && currentTime > 0f)

	/**
	 * If true, the music will loop.
	 * (Default false)
	 */
	var loop: Boolean

	/**
	 * 0f-1f
	 */
	var volume: Float

	/**
	 * Toggles the playing status.
	 */
	fun toggle(): Boolean {
		if (isPlaying) pause()
		else play()
		return isPlaying
	}

	fun play()
	fun pause()

	/**
	 * Stops the music, setting the [currentTime] to 0f. This is not the same as [Sound]; the music is not disposed
	 * when it's stopped or paused.
	 */
	fun stop()

	/**
	 * Indicates the current playback time (in seconds). Setting this will seek to the new time.
	 */
	var currentTime: Float

	/**
	 * Updates this object.
	 */
	fun update()
}

enum class MusicReadyState {

	/**
	 * Has no data.
	 */
	NOTHING,

	/**
	 * Has enough data to start playing.
	 */
	READY
}

/**
 * Sets the audio's position so that
 * @param value -1
 */
fun Sound.setPanning(value: Float) {
	setPosition(cos((value - 1f) * PI / 2f), 0f, sin((value + 1f) * PI / 2f))
}
