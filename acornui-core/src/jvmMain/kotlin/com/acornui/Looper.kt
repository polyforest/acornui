/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui

import com.acornui.signal.Signal0
import com.acornui.signal.Signal1
import com.acornui.time.FrameDriverImpl
import com.acornui.time.FrameDriverRo
import com.acornui.time.nowMs
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.seconds

actual fun looper(): Looper = JvmLooper()

/**
 * The looper is a loop where first the [FrameDriverRo] is invoked, then each application's windows are updated and
 * rendered.
 */
class JvmLooper : Looper {

	private val _frameDriver = FrameDriverImpl()
	override val frameDriver: FrameDriverRo = _frameDriver

	override var frameRate = 60
		set(value) {
			field = value
			frameTimeMs = (1000 / value).toLong()
		}

	private val _started = Signal0()
	override val started = _started.asRo()

	private val _pollEvents = Signal0()
	override val pollEvents = _pollEvents.asRo()

	private var frameTimeMs: Long = (1000 / frameRate).toLong()

	/**
	 * Runs the multi-application loop.
	 */
	@UseExperimental(InternalCoroutinesApi::class)
	override fun loop(mainJob: Job) {
		_started.dispatch()
		var lastFrameMs = nowMs()
		while (mainJob.isActive) {
			// Poll for window events. Input callbacks will be invoked at this time.
			val now = nowMs()
			val dT = (now - lastFrameMs) / 1000f
			lastFrameMs = now
			_pollEvents.dispatch()
			_frameDriver.dispatch(dT)
			val sleepTime = lastFrameMs + frameTimeMs - nowMs()
			if (sleepTime > 0) Thread.sleep(sleepTime)
		}
		_started.clear()
		_pollEvents.dispose()
		_frameDriver.clear()
		mainJob.getCancellationException().cause?.let { throw it }
	}
}