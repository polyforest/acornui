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

package com.acornui.headless

import com.acornui.io.Loader
import com.acornui.io.ProgressReporter
import com.acornui.io.UrlRequestData
import kotlin.time.Duration
import kotlin.time.seconds

class MockLoader<T>(private val factory: suspend (requestData: UrlRequestData) -> T) : Loader<T> {

	constructor(default: T) : this({ default })

	override val defaultInitialTimeEstimate: Duration
		get() = 1.seconds

	override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration, connectTimeout: Duration): T {
		return factory(requestData)
	}
}