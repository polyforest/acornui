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

import com.acornui.JsApplicationBase
import com.acornui.MainContext
import com.acornui.di.ContextImpl
import kotlinx.coroutines.Job

class JsHeadlessApplication(mainContext: MainContext) : JsApplicationBase(mainContext) {

	/**
	 * Creates an injector with JS dependencies from the bootstrap, and mock dependencies for input and graphics.
	 */
	override suspend fun createContext() = ContextImpl(
			owner = null,
			dependencies = HeadlessDependencies.create(config()) + bootstrap.dependencies(),
			coroutineContext = applicationScope.coroutineContext + Job(applicationJob)
	)
}