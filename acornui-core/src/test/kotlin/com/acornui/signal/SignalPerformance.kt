/*
 * Copyright 2015 Nicholas Bilyk
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
 * Commented out due to no common nanoTime replacement...
 * See https://github.com/polyforest/acornui/issues/121
 */
import com.acornui.logging.Log
import com.acornui.test.benchmark
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
class SignalPerformance {

	@Test fun dispatchSome() {
		val s = unmanagedSignal<Int>()
		s.listen {} // 4 handlers
		s.listen {}
		s.listen {}
		s.listen {}

		val speed = benchmark {
			for (i in 0..999) {
				s.dispatch(3)
			}
		}
		// Dispatch 4 avg: 53.3us
		Log.debug("Dispatch 4 avg: $speed")
	}

	@Test fun dispatchMany() {
		val s = unmanagedSignal<Int>()
		for (i in 0..999) {
			s.listen {}
		}

		val speed = benchmark {
			for (i in 0..1000) {
				s.dispatch(3)
			}
		}
		// Dispatch 1000 avg: 11.9ms
		Log.debug("Dispatch 1000 avg: $speed")
	}

	@Test fun addOnce() {
		val s = unmanagedSignal<Int>()
		s.listen {}

		val speed = benchmark {
			for (i in 0..999) {
				s.once {}
				s.once {}
				s.once {}
				s.once {}
				s.dispatch(3)
			}
		}
		// Add once 4 avg: 389us
		Log.debug("Add once 4 avg: $speed")
	}
}