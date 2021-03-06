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

import com.acornui.logging.Log
import com.acornui.test.runTest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.fail
import kotlin.time.seconds

class AsyncUtilsTest {

	@Test
	fun launchSupervisedShouldCatchExceptions() = runTest {
        launchSupervised {
            error("Should be caught")
        }
    }

	@Test
	fun launchSupervisedCancelParentShouldCancelChild() = runTest {
        launch {
            launchSupervised {
                delay(1.seconds)
                fail("Should be cancelled.")
            }.invokeOnCompletion {
				Log.debug("Complete $it")
            }
            delay(0.1.seconds)
			Log.debug("Cancelling...")
            cancel()
        }
    }
}