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

package com.acornui.js

import com.acornui.AppConfig
import com.acornui.ApplicationBase
import com.acornui.Version
import com.acornui.asset.Loaders
import com.acornui.asset.load
import com.acornui.async.PendingDisposablesRegistry
import com.acornui.async.globalLaunch
import com.acornui.audio.AudioManager
import com.acornui.audio.AudioManagerImpl
import com.acornui.component.HtmlComponent
import com.acornui.component.stage
import com.acornui.cursor.CursorManager
import com.acornui.di.*
import com.acornui.focus.FocusManager
import com.acornui.graphic.Window
import com.acornui.input.*
import com.acornui.input.interaction.ContextMenuManager
import com.acornui.input.interaction.UndoDispatcher
import com.acornui.io.file.Files
import com.acornui.io.file.FilesImpl
import com.acornui.io.file.FilesManifest
import com.acornui.js.cursor.JsCursorManager
import com.acornui.js.input.JsClipboard
import com.acornui.js.input.JsKeyInput
import com.acornui.js.input.JsMouseInput
import com.acornui.js.persistence.JsPersistence
import com.acornui.logging.Log
import com.acornui.persistence.Persistence
import com.acornui.selection.SelectionManager
import com.acornui.selection.SelectionManagerImpl
import com.acornui.serialization.jsonParse
import com.acornui.uncaughtExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.HTMLElement
import org.w3c.dom.LOADING
import kotlin.browser.document
import kotlin.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The application base that would be used by all JS-based applications.
 */
@Suppress("unused")
abstract class JsApplicationBase : ApplicationBase() {

	private var frameDriver: JsApplicationRunner? = null

	init {
		// Uncaught exception handler
		val prevOnError = window.onerror
		window.onerror = { message, source, lineNo, colNo, error ->
			prevOnError?.invoke(message, source, lineNo, colNo, error)
			if (error is Throwable)
				uncaughtExceptionHandler(error)
			else
				uncaughtExceptionHandler(Exception("Unknown error: $message $lineNo $source $colNo $error"))
		}

		if (::memberRefTest != ::memberRefTest)
			Log.error("[SEVERE] Member reference equality fix isn't working.")

		window.addEventListener("unload", { event ->
			dispose()
		})
	}

	override fun start(appConfig: AppConfig, onReady: Owned.() -> Unit) {
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

	abstract val canvasTask: suspend () -> HTMLElement
	abstract val windowTask: suspend () -> Window
	abstract val componentsTask: suspend () -> (owner: Owned) -> HtmlComponent

	protected open suspend fun initializeFrameDriver(injector: Injector): JsApplicationRunner {
		return JsApplicationRunnerImpl(injector)
	}

	protected open val mouseInputTask by task(MouseInput) {
		JsMouseInput(get(CANVAS))
	}

	protected open val keyInputTask by task(KeyInput) {
		JsKeyInput(get(CANVAS), config().input.jsCaptureAllKeyboardInput)
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

	protected open val interactivityTask by task(InteractivityManager) {
		InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager))
	}

	protected open val cursorManagerTask by task(CursorManager) {
		JsCursorManager(get(CANVAS))
	}

	protected open val persistenceTask by task(Persistence) {
		JsPersistence(get(Version))
	}

	protected open val selectionManagerTask by task(SelectionManager) {
		SelectionManagerImpl()
	}

	protected open val clipboardTask by task(Clipboard) {
		JsClipboard(
				get(CANVAS),
				get(FocusManager),
				get(InteractivityManager),
				config().input.jsCaptureAllKeyboardInput
		)
	}

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

	companion object {
		protected val CANVAS = dKey<HTMLElement>()
	}

}