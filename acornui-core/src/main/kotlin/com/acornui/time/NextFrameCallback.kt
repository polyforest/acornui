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

package com.acornui.time

import com.acornui.Owner
import com.acornui.own
import kotlinx.browser.window

private class NextFrameCallback(
		val callback: () -> Unit
) : CallbackWrapper {

	private var animationFrameId: Int = -1

	private fun update(t: Double) {
		animationFrameId = -1
		callback()
	}

	override operator fun invoke() {
		if (animationFrameId != -1) return
		animationFrameId = window.requestAnimationFrame(::update)
	}

	override fun dispose() {
		stop()
	}

	private fun stop() {
		window.cancelAnimationFrame(animationFrameId)
	}
}

/**
 * Creates a callback wrapper that will only invoke once per animation frame.
 * When the receiver is disposed, the callback will be canceled.
 */
fun Owner.nextFrameCallback(callback: () -> Unit): CallbackWrapper {
	return own(NextFrameCallback(callback))
}