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
import com.acornui.time.FrameDriverImpl
import com.acornui.time.FrameDriverRo
import com.acornui.time.nowMs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import setTimeout
import kotlin.browser.window

actual fun looper(): Looper = JsLooper()

class JsLooper : Looper {

	private val _frameDriver = FrameDriverImpl()
	override val frameDriver: FrameDriverRo = _frameDriver

	/**
	 * Setting this does nothing on the JS Backend. Frames are determined by [org.w3c.dom.Window.requestAnimationFrame]
	 */
	@Suppress("UNUSED_PARAMETER", "SetterBackingFieldAssignment")
	override var frameRate: Int = 60
		set(value) {}

	private val _started = Signal0()
	override val started = _started.asRo()

	override val pollEvents: Signal<() -> Unit> = emptySignal()

	private val _updateAndRender = Signal1<Float>()

	private val isBrowser = userInfo.isBrowser
	private var lastFrameMs = nowMs()
	private lateinit var mainJob: Job

	/**
	 * Runs the multi-application loop.
	 */
	override fun loop(mainJob: Job) {
		this.mainJob = mainJob
		_started.dispatch()
		scheduleTick()
	}

	private fun tick() {
		// Poll for window events. Input callbacks will be invoked at this time.
		val now = nowMs()
		val dT = (now - lastFrameMs) / 1000f
		lastFrameMs = now
		try {
			_frameDriver.dispatch(dT)
		} catch (e: Throwable) {
			mainJob.cancel(CancellationException(e.message, e))
		}
		if (mainJob.isActive)
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

	@UseExperimental(InternalCoroutinesApi::class)
	private fun shutdown() {
		_started.clear()
		_updateAndRender.clear()
	}
}