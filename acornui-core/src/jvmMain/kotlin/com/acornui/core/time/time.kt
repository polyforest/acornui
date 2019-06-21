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

package com.acornui.core.time

import com.acornui.time.Date
import com.acornui.time.DateImpl

/**
 * @author nbilyk
 */
private class TimeProviderImpl: TimeProvider {

	private var _startTime = (DateImpl().time - msElapsed())

	override fun now(): Date {
		return DateImpl()
	}

	override fun nowMs(): Long {
		return _startTime + msElapsed()
	}

	override fun nanoElapsed(): Long {
		return System.nanoTime()
	}
}

/**
 * A global abstracted time provider.
 */
actual val time: TimeProvider = TimeProviderImpl()
