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
import com.acornui.time.nowMs

open class JvmApplicationRunner(
		override val injector: Injector
) : Scoped {

	private val window = inject(Window)
	private val stage = inject(Stage)

	open fun run() {
		Log.debug("Activating stage")
		stage.activate()

		// The window has been damaged.
		var lastFrameMs = nowMs()
		val frameTimeMs = 1000 / inject(AppConfig).frameRate
		while (!window.isCloseRequested()) {
			// Poll for window events. Input callbacks will be invoked at this time.
			val now = nowMs()
			val dT = (now - lastFrameMs) / 1000f
			lastFrameMs = now
			tick(dT)
			val sleepTime = lastFrameMs + frameTimeMs - nowMs()
			if (sleepTime > 0) Thread.sleep(sleepTime)
			pollEvents()
		}
		Log.debug("Window closed")
	}

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