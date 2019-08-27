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

package com.acornui.async

import com.acornui.logging.Log
import kotlinx.coroutines.Job
import kotlin.system.exitProcess

/**
 * When this job has finished, call [exitProcess] with a status of either 0 for normal completion, or if the job
 * failed, the error will be logged and the status code will be -1.
 */
fun Job.exitOnCompletion() {
	invokeOnCompletion { 
		e ->
		if (e == null) exitProcess(0)
		else {
			Log.error(e)
			exitProcess(-1)
		}
	}
}