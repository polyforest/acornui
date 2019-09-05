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
import com.acornui.Updatable
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlin.time.Duration

/**
 * @author nbilyk
 */
private class Schedule private constructor() : Updatable, Clearable, Disposable {

	private var isActive = false

	/**
	 * The timestamp, where after passing, will invoke [callback].
	 */
	var date: DateRo = Date()
		private set

	/**
	 * The callback to invoke once [date] has been passed.
	 */
	private var callback: Disposable.() -> Unit = NOOP

	override fun update(dT: Float) {
		if (nowMs() >= date.time) {
			callback()
			dispose()
		}
	}

	override fun clear() {
		callback = NOOP
	}

	override fun dispose() {
		if (!isActive) return
		isActive = false
		stop()
		pool.free(this)
	}

	companion object {

		private val NOOP: Disposable.() -> Unit = {}

		private val pool = ClearableObjectPool { Schedule() }

		internal fun obtain(date: DateRo, callback: Disposable.() -> Unit): Disposable {
			val e = pool.obtain()
			e.callback = callback
			e.date = date
			e.isActive = true
			e.start()
			return e
		}
	}
}

/**
 * Invokes a callback once the current wall clock has surpassed [date].
 * @param date The date in the future to invoke [callback]. If this is in the past, the callback will be invoked on
 * the next frame.
 * @param callback Invoked once [date] has passed. The receiver is the disposable handle, from which this callback can
 * be removed.
 *
 * This is different from [timer] in that the wall clock is used, as opposed to the [FrameDriver] delta, which isn't
 * guaranteed accuracy if frames are dropped.
 * Use [timer] when syncing with animations, and `schedule` when specific time stamps are needed.
 */
fun schedule(date: DateRo, callback: Disposable.() -> Unit): Disposable = Schedule.obtain(date, callback)

/**
 * Schedules a callback [duration] in the future.
 */
fun schedule(duration: Duration, callback: Disposable.() -> Unit): Disposable = Schedule.obtain(Date() + duration, callback)