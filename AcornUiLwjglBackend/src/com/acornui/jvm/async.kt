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

package com.acornui.jvm

import com.acornui.async.*
import java.util.concurrent.Executors

fun <T> asyncThread(work: Work<T>): Deferred<T> {

	// TODO:

	return async(work)

//	return object : Promise<T>(), Deferred<T> {
//		private val executor = Executors.newSingleThreadExecutor()
//		init {
//			executor.submit {
//				launch {
//					try {
//						success(work())
//					} catch (e: Throwable) {
//						fail(e)
//					}
//				}
//			}
//		}
//	}
}