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
import com.acornui.time.FrameDriver
import com.acornui.time.nowMs
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.seconds

actual fun looper(): Looper = JvmLooper()

/**
 * The looper is a loop where first the [FrameDriver] is invoked, then each application's windows are updated and
 * rendered.
 */
class JvmLooper : Looper {

	private val _job = Job()
	override val job: Job = _job

	override var frameTime: Duration = (1.0 / 60.0).seconds

	private val _started = Signal0()
	override val started = _started.asRo()

	private val _pollEvents = Signal0()
	override val pollEvents = _pollEvents.asRo()

	private val _updateAndRender = Signal1<Float>()

	/**
	 * Dispatched when the acorn applications should update and render.
	 */
	override val updateAndRender = _updateAndRender.asRo()

	private val _completed = Signal0()
	override val completed = _completed.asRo()
	
	private var isLooping = false

	override var referenceCount: Int = 0
		private set

	@Synchronized
	override fun refInc() { ++referenceCount }

	@Synchronized
	override fun refDec() { --referenceCount }

	/**
	 * Runs the multi-application loop.
	 */
	override fun loop() {
		if (isLooping) return
		isLooping = true
		_started.dispatch()
		val frameTimeMs = frameTime.toLongMilliseconds()
		var lastFrameMs = nowMs()
		while (referenceCount > 0 && !_job.isCancelled) {
			// Poll for window events. Input callbacks will be invoked at this time.
			val now = nowMs()
			val dT = (now - lastFrameMs) / 1000f
			lastFrameMs = now
			_pollEvents.dispatch()
			FrameDriver.dispatch(dT)
			_updateAndRender.dispatch(dT)
			val sleepTime = lastFrameMs + frameTimeMs - nowMs()
			if (sleepTime > 0) Thread.sleep(sleepTime)
		}
		isLooping = false
		_started.clear()
		_pollEvents.dispose()
		_updateAndRender.clear()
		_job.complete()
		_completed.dispatch()
		_completed.clear()
	}
}