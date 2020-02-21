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
import com.acornui.di.Context
import com.acornui.start
import com.acornui.stop
import kotlin.time.Duration

/**
 * Invokes a callback once the current wall clock has surpassed [date].
 *
 * @param frameDriver The frame driver for this context.
 * @param date The date in the future to invoke [callback]. If this is in the past, the callback will be invoked on
 * the next frame.
 * @param callback Invoked once [date] has passed. The receiver is the disposable handle, from which this callback can
 * be removed.
 *
 * This is different from [timer] in that the wall clock is used, as opposed to the [FrameDriverRo] delta, which isn't
 * guaranteed accuracy if frames are dropped.
 *
 * In general, use [timer] when syncing with animations, and `schedule` when specific time stamps are needed.
 */
class Schedule(

		override val frameDriver: FrameDriverRo,

		/**
		 * The timestamp, where after passing, will invoke [callback].
		 */
		val date: DateRo,

		/**
		 * The callback to invoke once [date] has been passed.
		 */
		private val callback: Disposable.() -> Unit
) : Updatable, Disposable {

	override fun update(dT: Float) {
		if (nowMs() >= date.time) {
			callback()
			dispose()
		}
	}

	override fun dispose() {
		stop()
	}
}

/**
 * Schedules a callback for a certain timestamp.
 * Immediately started.
 */
fun Context.schedule(date: DateRo, callback: Disposable.() -> Unit): Schedule = Schedule(inject(FrameDriverRo), date, callback).start()

/**
 * Schedules a callback [duration] in the future.
 * Immediately started.
 */
fun Context.schedule(duration: Duration, callback: Disposable.() -> Unit): Schedule = Schedule(inject(FrameDriverRo), Date() + duration, callback).start()