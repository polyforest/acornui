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

import com.acornui.dom.hidden
import com.acornui.dom.visibilityChange
import com.acornui.function.as1
import com.acornui.signal.SignalImpl
import com.acornui.signal.SignalSubscription
import com.acornui.signal.once
import com.acornui.time.markNow
import kotlin.browser.document
import kotlin.browser.window
import kotlin.time.Duration
import kotlin.time.seconds

private object Frame {

	/**
	 * A Signal dispatched on the windows animation frame when requested.
	 */
	val frame = object : SignalImpl<Duration>() {
		override fun listen(isOnce: Boolean, handler: (Duration) -> Unit): SignalSubscription {
			val sub = super.listen(isOnce, handler)
			requestFrame()
			return sub
		}
	}

	/**
	 * If a frame hangs, the delta provided will be capped to this value.
	 * This is so that animations aren't entirely skipped.
	 */
	var maxFrameTime: Duration = 0.1.seconds

	private var time = markNow()
	private var frameRequested = false

	private val tick: (Double) -> Unit = {
		frameRequested = false
		val dT = minOf(maxFrameTime, time.elapsedNow())
		time = markNow()
		frame.dispatch(dT)
		requestFrame()
	}

	private fun requestFrame() {
		if (frameRequested || document.hidden || !frame.isNotEmpty()) return
		window.requestAnimationFrame(tick)
		frameRequested = true
	}

	init {
		document.visibilityChange.listen(::requestFrame.as1)
	}
}

/**
 * The signal dispatched on every frame.
 */
val frame by lazy { Frame.frame }