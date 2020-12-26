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

package com.acornui.io

import com.acornui.async.ProgressImpl
import com.acornui.async.await
import com.acornui.async.delay
import com.acornui.test.assertGreaterThan
import com.acornui.test.runTest
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.time.measureTime
import kotlin.time.milliseconds

class ProgressTest {

	@Test fun await() = runTest {
        val p = ProgressImpl(total = 200.milliseconds)
        launch {
            delay(p.total)
            p.complete()
        }
        val time = measureTime {
            p.await()
        }
        assertGreaterThan(p.total, time)
    }
}