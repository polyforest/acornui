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
import com.acornui.core.AppConfig
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.Window
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.time
import com.acornui.logging.Log
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
	private val appConfig = inject(AppConfig)

	private var isRunning: Boolean = false

	private var tickFrameId: Int = -1

	/**
	 * The next time to do a step.
	 */
	private var nextTick: Long = 0

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
		nextTick = time.nowMs()
		tickFrameId = window.requestAnimationFrame(tick)
	}

	private fun _tick() {
		val stepTimeFloat = appConfig.stepTime
		val stepTimeMs = 1000 / appConfig.frameRate
		var loops = 0
		val now: Long = time.nowMs()
		// Do a best attempt to keep the time driver in sync, but stage updates and renders may be sacrificed.
		while (now > nextTick) {
			nextTick += stepTimeMs
			timeDriver.update(stepTimeFloat)
			if (++loops > MAX_FRAME_SKIP) {
				// If we're too far behind, break and reset.
				nextTick = time.nowMs() + stepTimeMs
				break
			}
		}
		if (appWindow.shouldRender(true)) {
			stage.update()
			appWindow.renderBegin()
			if (stage.visible)
				stage.render(viewport.set(0f, 0f, appWindow.width, appWindow.height))
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

	companion object {

		/**
		 * The maximum number of update() calls before a render is required.
		 */
		private val MAX_FRAME_SKIP = 10
	}
}