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

package com.acornui.mock

import com.acornui.io.Loader
import com.acornui.io.ProgressReporter
import com.acornui.io.UrlRequestData

class MockLoader<T>(private val default: T) : Loader<T> {

	override val defaultInitialTimeEstimate: Float
		get() = 1f

	override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Float): T {
		return default
	}
}