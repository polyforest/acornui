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

package com.acornui.lwjgl.audio

import com.acornui.collection.poll
import com.acornui.collection.pop
import com.acornui.audio.AudioManagerImpl
import com.acornui.time.FrameDriverRo
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer


/**
 * @author nbilyk
 */
class OpenAlAudioManager(frameDriver: FrameDriverRo) : AudioManagerImpl(frameDriver) {

	private var device: Long = 0
	private var context: Long = 0

	private val idleSources = ArrayList<Int>(simultaneousSounds)
	
	init {
		try {
			device = ALC10.alcOpenDevice(null as ByteBuffer?)
		} catch (e: Throwable) {}
		if (device == 0L) {
			throw NoAudioException()
		}
		val deviceCapabilities = ALC.createCapabilities(device)
		context = ALC10.alcCreateContext(device, null as IntBuffer?)
		if (context == 0L) {
			ALC10.alcCloseDevice(device)
			throw NoAudioException()
		}
		if (!ALC10.alcMakeContextCurrent(context)) {
			throw NoAudioException()
		}
		AL.createCapabilities(deviceCapabilities)

		for (i in 0 until simultaneousSounds) {
			val sourceId = AL10.alGenSources()
			if (AL10.alGetError() != AL10.AL_NO_ERROR) break
			idleSources.add(sourceId)
		}

		initListener()
	}

	private fun initListener() {
		// Set up the listener. This is fixed; the ability to change the listener position is not exposed.
		val orientation = BufferUtils.createFloatBuffer(6).put(floatArrayOf(0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f)).flip() as FloatBuffer // atX, atY, atZ, upX, upY, upZ
		AL10.alListenerfv(AL10.AL_ORIENTATION, orientation)
		val velocity = BufferUtils.createFloatBuffer(3).put(floatArrayOf(0.0f, 0.0f, 0.0f)).flip() as FloatBuffer
		AL10.alListenerfv(AL10.AL_VELOCITY, velocity)
		val position = BufferUtils.createFloatBuffer(3).put(floatArrayOf(0.0f, 0.0f, 0.0f)).flip() as FloatBuffer
		AL10.alListenerfv(AL10.AL_POSITION, position)
	}

	fun obtainSourceId(): Int {
		return idleSources.poll()
	}

	fun freeSourceId(sourceId: Int) {
		idleSources.add(sourceId)
		if (idleSources.size > simultaneousSounds)
			throw Exception("Idle sources grew larger than it should have.")
	}


	override fun dispose() {
		println("Audio Manager Disposed")
		super.dispose()

		while (idleSources.isNotEmpty()) {
			AL10.alDeleteSources(idleSources.pop())
		}
		ALC10.alcDestroyContext(context)
		ALC10.alcCloseDevice(device)
	}

}



class NoAudioException : Throwable("Audio could not be initialized")
