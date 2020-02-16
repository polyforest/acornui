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

@file:Suppress("unused")

package com.acornui.headless

import com.acornui.ApplicationBase
import com.acornui.MainContext
import com.acornui.asset.Loaders
import com.acornui.di.ContextImpl
import com.acornui.graphic.RgbData
import com.acornui.io.*
import com.acornui.uncaughtExceptionHandler
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * A Headless application initializes utility dependencies, but does not create any windowing, graphics, or input.
 * @author nbilyk
 */
open class JvmHeadlessApplication(mainContext: MainContext) : ApplicationBase(mainContext) {

	override suspend fun onBeforeStart() {
		mainContext.looper.frameTime = config().frameTime.toDouble().seconds
	}

	override suspend fun createContext() = ContextImpl(
			owner = null,
			dependencies = HeadlessDependencies.create(config()) + bootstrap.dependencies(),
			coroutineContext = applicationScope.coroutineContext + Job(applicationJob)
	)

	protected open val rgbDataLoader by task(Loaders.rgbDataLoader) {
		object : Loader<RgbData> {
			override val defaultInitialTimeEstimate: Duration
				get() = Bandwidth.downBpsInv.seconds * 100_000

			override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration): RgbData {
				return loadRgbData(requestData, progressReporter, initialTimeEstimate)
			}
		}
	}

	companion object {
		init {
			Thread.currentThread().setUncaughtExceptionHandler { _, exception ->
				uncaughtExceptionHandler(exception)
			}
		}
	}
}


