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

package com.acornui

import com.acornui.async.isUiThread
import com.acornui.component.Stage
import com.acornui.di.inject
import com.acornui.graphic.Window
import com.acornui.graphic.updateAndRender
import com.acornui.logging.Log
import com.acornui.time.FrameDriver
import com.acornui.time.loopWhile
import kotlinx.coroutines.runBlocking
import kotlin.time.seconds

open class JvmApplicationRunner(
		protected val stage: Stage
) {

	protected val window = stage.inject(Window)
	protected val appConfig = stage.inject(AppConfig)

	open fun run() = runBlocking {
		Log.debug("Activating stage")
		stage.activate()

		// The window has been damaged.
		loopWhile(appConfig.frameTime.toDouble().seconds) { dT ->
			pollEvents()
			tick(dT)
			!window.isCloseRequested()
		}
		Log.debug("Window closed")
	}

	/**
	 * Poll for window events - input callbacks will be called at this time.
	 */
	protected open fun pollEvents() {
	}

	protected fun tick(dT: Float) {
		FrameDriver.dispatch(dT)
		window.updateAndRender(stage)
	}
}