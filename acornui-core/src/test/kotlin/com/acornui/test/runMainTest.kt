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

package com.acornui.test

import com.acornui.AppConfig
import com.acornui.MainContext
import com.acornui.component.Stage
import com.acornui.runMain
import com.acornui.runMainInternal
import initMockDom
import kotlinx.coroutines.launch
import kotlin.js.Promise
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * [runMain] but with a more sensible timeout default for unit tests.
 * @return Returns a Promise, suitable for asynchronous unit test frameworks.
 */
fun runMainTest(timeout: Duration = 10.seconds, block: suspend MainContext.() -> Unit): Promise<Unit> {
	initMockDom()
	return runMainInternal(timeout, block)
}

/**
 * Runs a test using a headless application.
 * The headless application will automatically exit once it has become idle.
 */
fun runHeadlessTest(appConfig: AppConfig = AppConfig(), timeout: Duration = 10.seconds, block: suspend Stage.() -> Unit): Promise<Unit> =
	runMainTest(timeout) {
		headlessApplication(appConfig) {
			launch { block() }
		}.join()
	}