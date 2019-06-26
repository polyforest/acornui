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

package com.acornui.async

import com.acornui.core.Disposable
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val sleepExecutor by lazy {
	val e = Executors.newCachedThreadPool{
		r ->
		Thread(r, "AcornUI_Sleep")
	}
	val d = object : Disposable {
		override fun dispose() {
			e.shutdownNow()
			PendingDisposablesRegistry.unregister(this)
		}
	}
	PendingDisposablesRegistry.register(d)
	e
}

/**
 * Suspends the coroutine for [duration] seconds.
 */
actual suspend fun delay(duration: Float) = suspendCoroutine<Unit> { cont ->
//	sleepExecutor.submit {
//		Thread.sleep((duration * 1000).toLong())
//		cont.resume(Unit)
//	}
	cont.resume(Unit)
}