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

import kotlinx.coroutines.isActive
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

actual suspend fun loopWhile(frameTime: Duration, inner: (dT: Float) -> Boolean) = suspendCoroutine<Unit> { cont ->
	val frameTimeMs = frameTime.toLongMilliseconds()
	var lastFrameMs = nowMs()
	var shouldContinue = inner(0f)
	while (shouldContinue && cont.context.isActive) {
		// Poll for window events. Input callbacks will be invoked at this time.
		val now = nowMs()
		val dT = (now - lastFrameMs) / 1000f
		lastFrameMs = now
		shouldContinue = inner(dT)
		val sleepTime = lastFrameMs + frameTimeMs - nowMs()
		if (sleepTime > 0) Thread.sleep(sleepTime)
	}
	cont.resume(Unit)
}