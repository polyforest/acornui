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

import com.acornui.function.as1
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import com.acornui.signal.Signal1
import com.acornui.signal.emptySignal
import com.acornui.system.userInfo
import com.acornui.time.FrameDriver
import com.acornui.time.nowMs
import kotlinx.coroutines.Job
import kotlin.browser.window
import kotlin.time.Duration
import kotlin.time.seconds

actual fun looper(): Looper = JsLooper()

class JsLooper : Looper {

	private val _job = Job()
	override val job: Job = _job

	/**
	 * Does nothing on the JS Backend.
	 */
	override var frameTime: Duration = (1.0 / 60.0).seconds

	private val _started = Signal0()
	override val started = _started.asRo()

	override val pollEvents: Signal<() -> Unit> = emptySignal()

	private val _updateAndRender = Signal1<Float>()

	/**
	 * Dispatched when the acorn applications should update and render.
	 */
	override val updateAndRender = _updateAndRender.asRo()

	private val _completed = Signal0()
	override val completed = _completed.asRo()

	private val isBrowser = userInfo.isBrowser
	private var lastFrameMs = nowMs()

	private var isLooping = false

	override var referenceCount: Int = 0
		private set

	override fun refInc() { ++referenceCount }
	override fun refDec() { --referenceCount }

	/**
	 * Runs the multi-application loop.
	 */
	override fun loop() {
		if (isLooping) return
		isLooping = true
		_started.dispatch()
		scheduleTick()
	}

	private fun tick() {
		// Poll for window events. Input callbacks will be invoked at this time.
		val now = nowMs()
		val dT = (now - lastFrameMs) / 1000f
		lastFrameMs = now
		FrameDriver.dispatch(dT)
		_updateAndRender.dispatch(dT)
		if (referenceCount > 0)
			scheduleTick()
		else
			shutdown()
	}

	private fun scheduleTick() {
		if (isBrowser)
			window.requestAnimationFrame(::tick.as1)
		else
			setTimeout(::tick)
	}

	private fun shutdown() {
		isLooping = false
		_started.clear()
		_updateAndRender.clear()
		_job.complete()
		_completed.dispatch()
		_completed.clear()
	}
}

/**
 * For nodejs
 */
private external fun setTimeout(handler: dynamic, timeout: Int = definedExternally, vararg arguments: Any?): Int

/**
 * For nodejs
 */
private external fun clearTimeout(handle: Int = definedExternally)