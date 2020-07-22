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

package com.acornui.signal


/**
 * A utility class to use as a parameter within a Signal that indicates that the behavior of signal should be
 * cancelled. Typically, a signal that can be canceled should be named as a gerund. Such as, changing, invalidating, etc.
 */
interface CancelRo {

	val isCancelled: Boolean

	fun cancel()
}

class Cancel : CancelRo {

	override var isCancelled: Boolean = false
		private set

	override fun cancel() {
		isCancelled = true
	}

	fun reset(): Cancel {
		isCancelled = false
		return this
	}
}