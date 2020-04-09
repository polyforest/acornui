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

import com.acornui.io.*
import kotlin.time.Duration
import kotlin.time.seconds

class MockLoader<T>(initialTimeEstimate: Duration = 1.seconds, private val factory: suspend (requestData: UrlRequestData) -> T) : Loader<T> {

	constructor(default: T) : this(factory = { default })

	override val requestSettings: RequestSettings = RequestSettings("", ProgressReporterImpl(), initialTimeEstimate)

	override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): T {
		return settings.progressReporter.addWork(settings.initialTimeEstimate) {
			factory(requestData)
		}
	}
}