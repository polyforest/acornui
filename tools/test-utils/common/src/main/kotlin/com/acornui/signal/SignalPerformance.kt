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

import kotlin.test.Test
//import com.acornui.test.benchmark

//class SignalPerformance {
//
//	@Test fun dispatchSome() {
//		val s = Signal1<Int>()
//		s.add {} // 4 handlers
//		s.add {}
//		s.add {}
//		s.add {}
//
//		val speed = benchmark {
//			for (i in 0..999) {
//				s.dispatch(3)
//			}
//		}
//		// (Old, linked list) Dispatch 4 avg: 0.046264168ms
//		// Dispatch 4 avg: 0.032411993ms
//		println("Dispatch 4 avg: ${speed}ms")
//	}
//
//	@Test fun dispatchMany() {
//		val s = Signal1<Int>()
//		for (i in 0..999) {
//			s.add {}
//		}
//
//		val speed = benchmark {
//			for (i in 0..1000) {
//				s.dispatch(3)
//			}
//		}
//		// (Old, linked list) Dispatch 1000 avg: 2.0503826ms
//		// (New, array list) Dispatch 1000 avg: 1.3271044ms
//		println("Dispatch 1000 avg: ${speed}ms")
//	}
//
//	@Test fun addOnce() {
//		val s = Signal1<Int>()
//		s.add {}
//
//		val speed = benchmark {
//			for (i in 0..999) {
//				s.addOnce {}
//				s.addOnce {}
//				s.addOnce {}
//				s.addOnce {}
//				s.dispatch(3)
//			}
//		}
//		// Old: Add once 4 avg: 0.107870445ms
//		// Add once 4 avg: 0.18459427ms
//		println("Add once 4 avg: ${speed}ms")
//	}
//}