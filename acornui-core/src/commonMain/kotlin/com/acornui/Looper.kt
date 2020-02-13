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

import com.acornui.signal.Signal
import kotlinx.coroutines.Job
import kotlin.time.Duration

/**
 * GlobalLooper is responsible for setting up the frame loop to be used by one or many acorn applications.
 */
interface Looper {

	/**
	 * The interval between each frame. This will be set to
	 * This has no effect on JS backends; they use window animation frames.
	 * This will be initialized to [AppConfig.frameRate].
	 */
	var frameTime: Duration

	/**
	 * Dispatched when the global loop has begun.
	 */
	val started: Signal<() -> Unit>

	/**
	 * Dispatched when the acorn applications should poll for events.
	 * Note: JS Backends do not poll for events and therefore this will not be dispatched.
	 */
	val pollEvents: Signal<() -> Unit>

	/**
	 * Dispatched when the acorn applications should update and render.
	 */
	val updateAndRender: Signal<(Float) -> Unit>

	/**
	 * Runs the multi-application loop.
	 * The loop will remain active as long as the provided job is active.
	 * @param mainJob The job to watch, looping until this job is completed.  If the job completes exceptionally,
	 * (except [kotlinx.coroutines.CancellationException]) then that exception will be thrown from this loop.
	 *
	 * @see Job.cancel
	 */
	fun loop(mainJob: Job)
}

expect fun looper(): Looper