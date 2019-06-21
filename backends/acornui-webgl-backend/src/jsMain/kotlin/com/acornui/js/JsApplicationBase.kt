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

import com.acornui.async.PendingDisposablesRegistry
import com.acornui.async.launch
import com.acornui.component.HtmlComponent
import com.acornui.component.stage
import com.acornui.core.AppConfig
import com.acornui.core.ApplicationBase
import com.acornui.core.Version
import com.acornui.core.asset.AssetManager
import com.acornui.core.asset.AssetManagerImpl
import com.acornui.core.asset.AssetType
import com.acornui.core.asset.LoaderFactory
import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.AudioManagerImpl
import com.acornui.core.cursor.CursorManager
import com.acornui.core.di.*
import com.acornui.core.focus.FocusManager
import com.acornui.core.graphic.Window
import com.acornui.core.input.*
import com.acornui.core.input.interaction.ContextMenuManager
import com.acornui.core.input.interaction.UndoDispatcher
import com.acornui.core.io.file.Files
import com.acornui.core.io.file.FilesImpl
import com.acornui.core.persistence.Persistence
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionManagerImpl
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.TimeDriverImpl
import com.acornui.io.file.FilesManifestSerializer
import com.acornui.js.audio.JsAudioElementMusicLoader
import com.acornui.js.audio.JsAudioElementSoundLoader
import com.acornui.js.audio.JsWebAudioSoundLoader
import com.acornui.js.audio.audioContextSupported
import com.acornui.js.cursor.JsCursorManager
import com.acornui.js.input.JsClipboard
import com.acornui.js.input.JsKeyInput
import com.acornui.js.input.JsMouseInput
import com.acornui.js.loader.JsBinaryLoader
import com.acornui.js.loader.JsTextLoader
import com.acornui.js.persistence.JsPersistence
import com.acornui.logging.Log
import com.acornui.serialization.parseJson
import com.acornui.uncaughtExceptionHandler
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
		js( // language=JS
				"""
Function.prototype.uncachedBind = Function.prototype.bind;
/**
 * Workaround to kotlin member references not being equal. KT-15101
 */
Function.prototype.bind = function() {
	if (arguments.length !== 2 || arguments[0] !== null || arguments[1] === null) return this.uncachedBind.apply(this, arguments);
	var receiver = arguments[1];
	if (!receiver.__bindingCache) receiver.__bindingCache = {};
	var existing = receiver.__bindingCache[this];
	if (existing !== undefined) return existing;
	var newBind = this.uncachedBind.apply(this, arguments);
	receiver.__bindingCache[this] = newBind;
	return newBind;
};

Kotlin.uncachedIsType = Kotlin.isType;
Kotlin.isType = function(object, klass) {
	if (klass === Object) {
      switch (typeof object) {
        case 'string':
        case 'number':
        case 'boolean':
        case 'function':
          return true;
        default:return object instanceof Object;
      }
    }
    if (object == null || klass == null || (typeof object !== 'object' && typeof object !== 'function')) {
      return false;
    }
    if (typeof klass === 'function' && object instanceof klass) {
      return true;
    }

	if (!object.__typeCache) object.__typeCache = {};
	var existing = object.__typeCache[klass];
	if (existing !== undefined) return existing;
	var typeCheck = Kotlin.uncachedIsType.apply(this, arguments);
	object.__typeCache[klass] = typeCheck;
	return typeCheck;
};
""")

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

	fun start(appConfig: AppConfig, onReady: Owned.() -> Unit) {
		set(AppConfig, appConfig)
		launch {
			contentLoad()
			awaitAll()

			val owner = OwnedImpl(createInjector())
			PendingDisposablesRegistry.register(owner)
			initializeSpecialInteractivity(owner)
			owner.stage.onReady()

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
		val manifest = parseJson(JsTextLoader(path).await(), FilesManifestSerializer)
		FilesImpl(manifest)
	}

	override val assetManagerTask by task(AssetManager) {
		val loaders = HashMap<AssetType<*>, LoaderFactory<*>>()
		addAssetLoaders(loaders)
		AssetManagerImpl(config().rootPath, get(Files), loaders, appendVersion = true)
	}

	protected open val audioManagerTask by task(AudioManager) {
		// JS Audio doesn't need to be updated like OpenAL audio does, so we don't add it to the TimeDriver.
		AudioManagerImpl()
	}

	protected open suspend fun addAssetLoaders(loaders: MutableMap<AssetType<*>, LoaderFactory<*>>) {
		val audioManager = get(AudioManager)
		loaders[AssetType.TEXT] = { path: String, estimatedBytesTotal: Int -> JsTextLoader(path, estimatedBytesTotal) }
		loaders[AssetType.BINARY] = { path: String, estimatedBytesTotal: Int -> JsBinaryLoader(path, estimatedBytesTotal) }
		loaders[AssetType.SOUND] = if (audioContextSupported) {
			{ path: String, _: Int -> JsWebAudioSoundLoader(path, audioManager) }
		} else {
			{ path: String, _: Int -> JsAudioElementSoundLoader(path, audioManager) }
		}
		loaders[AssetType.MUSIC] = { path: String, _: Int -> JsAudioElementMusicLoader(path, audioManager) }
	}

	protected open val timeDriverTask by task(TimeDriver) {
		TimeDriverImpl(config().timeDriverConfig)
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
		launch {
			awaitAll()
			frameDriver?.stop()
		}
	}

	companion object {
		protected val CANVAS = dKey<HTMLElement>()
	}

}