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

import com.acornui.Disposable
import kotlinx.browser.window
import kotlin.js.Date
import kotlin.time.Duration
import kotlin.time.milliseconds

/**
 * Schedules a callback for a certain timestamp.
 */
fun schedule(date: Date, callback: () -> Unit): Disposable = schedule((date.getTime() - Date.now()).milliseconds, callback)

/**
 * Schedules a callback [duration] in the future.
 * Immediately started.
 */
fun schedule(duration: Duration, callback: () -> Unit): Disposable {
	val id = window.setTimeout(callback, duration.inMilliseconds.toInt())
	return Disposable {
		window.clearTimeout(id)
	}
}