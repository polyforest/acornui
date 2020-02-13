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

package com.acornui.async

import kotlinx.coroutines.Job
import kotlin.js.Promise

actual fun Job.toPromiseOrVoid(): dynamic = Promise<Unit> { resolve, reject ->
	invokeOnCompletion {
		val cause = it?.cause
		if (cause != null) reject(cause)
		else resolve(Unit)
	}
}