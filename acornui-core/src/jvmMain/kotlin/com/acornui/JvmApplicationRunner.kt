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

import com.acornui.component.Stage
import com.acornui.component.render
import com.acornui.di.Injector
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.graphic.Window
import com.acornui.logging.Log
import com.acornui.time.FrameDriver
import com.acornui.time.loopWhile
import kotlinx.coroutines.runBlocking
import kotlin.time.seconds

open class JvmApplicationRunner(
		override val injector: Injector
) : Scoped {

	private val window = inject(Window)
	private val stage = inject(Stage)

	open fun run() = runBlocking {
		Log.debug("Activating stage")
		stage.activate()

		// The window has been damaged.
		loopWhile(inject(AppConfig).frameTime.toDouble().seconds) { dT ->
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
		if (window.shouldRender(true)) {
			stage.update()
			if (window.width > 0f && window.height > 0f) {
				window.renderBegin()
				if (stage.visible)
					stage.render()
				window.renderEnd()
			}
		}
	}
}