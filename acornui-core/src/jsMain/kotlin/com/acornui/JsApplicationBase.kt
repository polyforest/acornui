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

import com.acornui.asset.Loaders
import com.acornui.asset.load
import com.acornui.async.PendingDisposablesRegistry
import com.acornui.audio.AudioManager
import com.acornui.audio.AudioManagerImpl
import com.acornui.component.Stage
import com.acornui.component.render
import com.acornui.di.*
import com.acornui.graphic.Window
import com.acornui.input.interaction.ContextMenuManager
import com.acornui.input.interaction.UndoDispatcher
import com.acornui.io.file.Files
import com.acornui.io.file.FilesImpl
import com.acornui.io.file.FilesManifest
import com.acornui.logging.Log
import com.acornui.persistence.JsPersistence
import com.acornui.persistence.Persistence
import com.acornui.selection.SelectionManager
import com.acornui.selection.SelectionManagerImpl
import com.acornui.serialization.jsonParse
import com.acornui.time.FrameDriver
import com.acornui.time.nowMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.LOADING
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
	}

	final override fun start(appConfig: AppConfig, onReady: Owned.() -> Unit) {
		set(AppConfig, appConfig)
		GlobalScope.launch(Dispatchers.Unconfined) {
			contentLoad()
			val owner = OwnedImpl(createInjector())
			PendingDisposablesRegistry.register(owner)
			initializeSpecialInteractivity(owner)
			owner.onReady()

			frameDriver = initializeFrameDriver(owner.injector)
			frameDriver!!.start()
		}
	}

	private suspend fun contentLoad() = suspendCoroutine<Unit> { cont ->
		if (document.readyState == DocumentReadyState.LOADING) {
			document.addEventListener("DOMContentLoaded", {
				cont.resume(Unit)
			})
		} else {
			cont.resume(Unit)
		}
	}

	protected open suspend fun initializeFrameDriver(injector: Injector): JsApplicationRunner {
		return JsApplicationRunnerImpl(injector)
	}

	override val filesTask by task(Files) {
		val path = config().rootPath + config().assetsManifestPath
		val manifest = jsonParse(FilesManifest.serializer(), get(Loaders.textLoader).load(path))
		FilesImpl(manifest)
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

class JsApplicationRunnerImpl(
		override val injector: Injector
) : JsApplicationRunner, Scoped {

	private var lastFrameMs: Long = 0L
	private val stage = inject(Stage)
	private val appWindow = inject(Window)

	private var isRunning: Boolean = false

	private var tickFrameId: Int = -1

	private val tick = {
		_: Double ->
		tick()
	}

	override fun start() {
		if (isRunning) return
		Log.info("Application#startIndex")
		isRunning = true
		stage.activate()
		lastFrameMs = nowMs()
		tickFrameId = window.requestAnimationFrame(tick)
	}

	private fun tick() {
		val now = nowMs()
		val dT = (now - lastFrameMs) / 1000f
		lastFrameMs = now
		FrameDriver.dispatch(dT)
		if (appWindow.shouldRender(true)) {
			stage.update()
			appWindow.renderBegin()
			if (stage.visible)
				stage.render()
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
