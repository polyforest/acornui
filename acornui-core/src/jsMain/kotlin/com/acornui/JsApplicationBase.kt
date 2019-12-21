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

import com.acornui.async.PendingDisposablesRegistry
import com.acornui.audio.AudioManager
import com.acornui.audio.AudioManagerImpl
import com.acornui.component.Stage
import com.acornui.di.*
import com.acornui.graphic.Window
import com.acornui.graphic.updateAndRender
import com.acornui.input.interaction.ContextMenuManager
import com.acornui.input.interaction.UndoDispatcher
import com.acornui.logging.Log
import com.acornui.persistence.JsPersistence
import com.acornui.persistence.Persistence
import com.acornui.selection.SelectionManager
import com.acornui.selection.SelectionManagerImpl
import com.acornui.system.userInfo
import com.acornui.time.FrameDriver
import com.acornui.time.nowMs
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.LOADING
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The application base that would be used by all JS-based applications, including node-js.
 */
@Suppress("unused")
abstract class JsApplicationBase : ApplicationBase() {

	private var frameDriver: JsApplicationRunner? = null

	init {
		if (::memberRefTest != ::memberRefTest)
			Log.error("[SEVERE] Member reference equality fix isn't working.")

		if (!userInfo.isBrowser && jsTypeOf(XMLHttpRequest) == "undefined") {
			println("Requiring XMLHttpRequest")
			js("""global.XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;""")
		}
	}

	override suspend fun start(appConfig: AppConfig, onReady: Stage.() -> Unit) {
		set(AppConfig, appConfig)
		contentLoad()
		val stage = createStage(createInjector())
		PendingDisposablesRegistry.register(stage)
		initializeSpecialInteractivity(stage)
		stage.onReady()

		frameDriver = initializeFrameDriver(stage)
		frameDriver!!.start()
	}

	private suspend fun contentLoad() = suspendCoroutine<Unit> { cont ->
		if (userInfo.isBrowser && document.readyState == DocumentReadyState.LOADING) {
			document.addEventListener("DOMContentLoaded", {
				cont.resume(Unit)
			})
		} else {
			cont.resume(Unit)
		}
	}

	protected open suspend fun initializeFrameDriver(stage: Stage): JsApplicationRunner {
		return if (userInfo.isBrowser) JsBrowserApplicationRunnerImpl(stage) else JsNodeApplicationRunnerImpl(stage)
	}

	protected open val audioManagerTask by task(AudioManager) {
		// JS Audio doesn't need to be updated like OpenAL audio does, so we don't add it to the TimeDriver.
		AudioManagerImpl()
	}

	protected open val persistenceTask by task(Persistence) {
		JsPersistence(get(Version))
	}

	protected open val selectionManagerTask by task(SelectionManager) {
		SelectionManagerImpl()
	}

	// TODO: Browserless clipboard

	protected open suspend fun initializeSpecialInteractivity(owner: Owned) {
		owner.own(UndoDispatcher(owner.injector))
		owner.own(ContextMenuManager(owner.injector))
	}

	private fun memberRefTest() {}

	//-----------------------------------
	// Disposable
	//-----------------------------------

	override fun dispose() {
		super.dispose()
		frameDriver?.stop()
	}

}

external fun delete(p: dynamic): Boolean


interface JsApplicationRunner {

	fun start()

	fun stop()

}

abstract class JsApplicationRunnerBase(
		protected val stage: Stage
) : JsApplicationRunner {

	private var lastFrameMs: Long = 0L
	protected val appWindow = stage.inject(Window)

	private var isRunning: Boolean = false

	protected var tickFrameId: Int = -1

	override fun start() {
		if (isRunning) return
		Log.info("Application#startIndex")
		isRunning = true
		stage.activate()
		lastFrameMs = nowMs()
	}

	protected open fun tick() {
		val now = nowMs()
		val dT = (now - lastFrameMs) / 1000f
		lastFrameMs = now
		FrameDriver.dispatch(dT)
		appWindow.updateAndRender(stage)
	}

	override fun stop() {
		if (!isRunning) return
		Log.info("Application#stop")
		isRunning = false
	}
}


class JsBrowserApplicationRunnerImpl(stage: Stage) : JsApplicationRunnerBase(stage) {

	private val tickCallback = { _: Double ->
		tick()
	}

	override fun start() {
		super.start()
		tickFrameId = window.requestAnimationFrame(tickCallback)
	}

	override fun tick() {
		super.tick()
		tickFrameId = if (appWindow.isCloseRequested()) -1 else window.requestAnimationFrame(tickCallback)
	}

	override fun stop() {
		super.stop()
		window.cancelAnimationFrame(tickFrameId)
	}
}


class JsNodeApplicationRunnerImpl(stage: Stage) : JsApplicationRunnerBase(stage) {

	private val tickCallback = {
		tick()
	}

	override fun start() {
		super.start()
		tickFrameId = setTimeout(tickCallback)
	}

	override fun tick() {
		super.tick()
		tickFrameId = if (appWindow.isCloseRequested()) -1 else setTimeout(tickCallback)
	}

	override fun stop() {
		super.stop()
		clearTimeout(tickFrameId)
	}
}

/**
 * For nodejs
 */
private external fun setTimeout(handler: dynamic, timeout: Int = definedExternally, vararg arguments: Any?): Int

/**
 * For nodejs
 */
private external fun clearTimeout(handle: Int = definedExternally)