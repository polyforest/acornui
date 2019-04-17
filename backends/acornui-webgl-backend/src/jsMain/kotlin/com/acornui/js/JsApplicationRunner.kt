/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.js

import com.acornui.component.Stage
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.Window
import com.acornui.core.time.TimeDriver
import com.acornui.graphic.Color
import com.acornui.logging.Log
import com.acornui.math.Matrix4
import com.acornui.math.MinMax
import kotlin.browser.window

interface JsApplicationRunner {

	fun start()

	fun stop()

}

class JsApplicationRunnerImpl(
		override val injector: Injector
) : JsApplicationRunner, Scoped {

	private val stage = inject(Stage)
	private val timeDriver = inject(TimeDriver)
	private val appWindow = inject(Window)

	private var isRunning: Boolean = false

	private var tickFrameId: Int = -1

	private val tick = {
		newTime: Double ->
		_tick()
	}

	private val viewport = MinMax()

	override fun start() {
		if (isRunning) return
		Log.info("Application#startIndex")
		isRunning = true
		stage.activate()
		timeDriver.activate()
		tickFrameId = window.requestAnimationFrame(tick)
	}

	private fun _tick() {
		timeDriver.update()
		if (appWindow.shouldRender(true)) {
			stage.update()
			appWindow.renderBegin()
			if (stage.visible)
				stage.render(viewport.set(0f, 0f, appWindow.width, appWindow.height), Matrix4.IDENTITY, Color.WHITE)
			appWindow.renderEnd()
		}
		tickFrameId = window.requestAnimationFrame(tick)
	}

	override fun stop() {
		if (!isRunning) return
		Log.info("Application#stop")
		isRunning = false
		window.cancelAnimationFrame(tickFrameId)
	}
}