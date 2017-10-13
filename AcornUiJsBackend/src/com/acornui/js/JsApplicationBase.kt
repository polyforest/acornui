/*
 * Copyright 2014 Nicholas Bilyk
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

import com.acornui.assertionsEnabled
import com.acornui.async.catch
import com.acornui.async.then
import com.acornui.browser.appendParam
import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import com.acornui.component.Stage
import com.acornui.core.AppConfig
import com.acornui.core.Disposable
import com.acornui.core.UidUtil
import com.acornui.core.assets.AssetManager
import com.acornui.core.assets.AssetManagerImpl
import com.acornui.core.assets.AssetTypes
import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.AudioManagerImpl
import com.acornui.core.cursor.CursorManager
import com.acornui.core.di.Bootstrap
import com.acornui.core.di.Owned
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.FocusManagerImpl
import com.acornui.core.graphics.Camera
import com.acornui.core.graphics.OrthographicCamera
import com.acornui.core.graphics.Window
import com.acornui.core.graphics.autoCenterCamera
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.InteractivityManagerImpl
import com.acornui.core.input.KeyInput
import com.acornui.core.input.MouseInput
import com.acornui.core.io.BufferFactory
import com.acornui.core.io.JSON_KEY
import com.acornui.core.io.file.Files
import com.acornui.core.io.file.FilesImpl
import com.acornui.core.persistance.Persistence
import com.acornui.core.popup.PopUpManager
import com.acornui.core.popup.PopUpManagerImpl
import com.acornui.core.request.RestServiceFactory
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionManagerImpl
import com.acornui.core.text.DateTimeFormatter
import com.acornui.core.text.NumberFormatter
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.TimeDriverImpl
import com.acornui.core.time.time
import com.acornui.io.file.FilesManifestSerializer
import com.acornui.js.audio.JsAudioElementMusicLoader
import com.acornui.js.audio.JsAudioElementSoundLoader
import com.acornui.js.audio.JsWebAudioSoundLoader
import com.acornui.js.audio.audioContextSupported
import com.acornui.js.cursor.JsCursorManager
import com.acornui.js.html.initializeUserInfo
import com.acornui.js.input.JsKeyInput
import com.acornui.js.input.JsMouseInput
import com.acornui.js.io.JsBufferFactory
import com.acornui.js.io.JsRestServiceFactory
import com.acornui.js.loader.JsTextLoader
import com.acornui.js.persistance.JsPersistence
import com.acornui.js.text.DateTimeFormatterImpl
import com.acornui.js.text.NumberFormatterImpl
import com.acornui.js.time.TimeProviderImpl
import com.acornui.logging.ILogger
import com.acornui.logging.Log
import com.acornui.serialization.JsonSerializer
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.HTMLElement
import org.w3c.dom.LOADING
import kotlin.browser.document
import kotlin.browser.window

/**
 * The common setup tasks to both a webgl application and a dom application backend.
 * @author nbilyk
 */
@Suppress("LeakingThis")
abstract class JsApplicationBase(
		protected val rootId: String,
		protected val config: AppConfig,
		private val onReady: Owned.(stage: Stage) -> Unit) : Disposable {

	val bootstrap = Bootstrap()

	/**
	 * The html element the application is placed in.
	 */
	protected abstract val canvas: HTMLElement

	private var frameDriver: JsApplicationRunner? = null

	init {
		if (this::memberRefTest != this::memberRefTest) println("[SEVERE] Member reference fix isn't working, check the KotlinMonkeyPatcher build step.")
		initializeUserInfo()
		initializeTime()
		initializeNumber()
		initializeString()
		initializeConfig()

		if (document.readyState == DocumentReadyState.LOADING) {
			document.addEventListener("DOMContentLoaded", {
				start()
			})
		} else {
			start()
		}

		window.addEventListener("beforeunload", { dispose() })
	}

	private fun start() {
		bootstrap.on(AppConfig) {
			initializeBootstrap()

			bootstrap.then {
				initializeStage()
				initializePopUpManager()
				initializeSpecialInteractivity()
			}
			bootstrap.then {
				bootstrap.lock()

				val stage = bootstrap[Stage]
				stage.onReady(stage)

				// Add the pop-up manager after onReady so that it is the highest index.
				stage.addElement(bootstrap[PopUpManager].view)

				frameDriver = initializeFrameDriver()
				frameDriver!!.start()
			}
		}
	}

	/**
	 * Initializes number constants and methods
	 */
	protected open fun initializeNumber() {
	}

	protected open fun initializeTime() {
		time = TimeProviderImpl()
	}

	protected open fun initializeString() {
		encodeUriComponent2 = ::encodeURIComponent
		decodeUriComponent2 = ::decodeURIComponent
	}

	protected open fun initializeConfig() {
		val path = config.rootPath + "assets/build.txt".appendParam("version", UidUtil.createUid())
		val buildVersionLoader = JsTextLoader(path)
		buildVersionLoader.then {
			config.version.build = it.toInt()
			Log.info("Build: ${config.version}")
			bootstrap[AppConfig] = config
		}
		buildVersionLoader.catch {
			Log.warn("assets/build.txt failed to load: $it")
			bootstrap[AppConfig] = config
		}
	}

	protected open fun initializeBootstrap() {
		initializeUncaughtExceptionHandler()
		initializeDebug()
		initializeLogging()
		initializeBufferFactory()
		initializeCanvas()
		initializeCss()
		initializeWindow()
		initializeMouseInput()
		initializeKeyInput()
		initializeJson()
		initializeCamera()
		initializeFiles()
		initializeRequest()
		initializeAssetManager()
		initializeTextures()
		initializeAudio()
		initializeTimeDriver()
		initializeFocusManager()
		initializeInteractivity()
		initializeCursorManager()
		initializeSelectionManager()
		initializePersistence()
		initializeTextFormatters()
		initializeComponents()
	}

	abstract fun initializeCanvas()

	protected open fun initializeCss() {
	}

	abstract fun initializeWindow()

	protected open fun initializeFrameDriver(): JsApplicationRunner {
		return JsApplicationRunnerImpl(bootstrap.injector)
	}

	protected open fun initializeUncaughtExceptionHandler() {
		window.onerror = {
			message, source, lineNo, colNo, error ->
			val msg = "Error caught: $message $lineNo $source $colNo $error"
			Log.error(msg)
			if (config.debug)
				window.alert(msg)
		}
	}

	protected open fun initializeDebug() {
		config.debug = config.debug || (window.location.search.contains(Regex("""(?:&|\?)debug=(true|1)""")))
		if (config.debug) {
			println("Debug mode")
			assertionsEnabled = true
		}
	}

	protected open fun initializeLogging() {
		if (config.debug) {
			Log.level = ILogger.DEBUG
		} else {
			Log.level = ILogger.WARN
		}
	}

	protected open fun initializeBufferFactory() {
		BufferFactory.instance = JsBufferFactory()
	}

	protected open fun initializeMouseInput() {
		bootstrap[MouseInput] = JsMouseInput(canvas)
	}

	protected open fun initializeKeyInput() {
		bootstrap[KeyInput] = JsKeyInput(canvas)
	}

	protected open fun initializeJson() {
		bootstrap[JSON_KEY] = JsonSerializer
	}

	protected open fun initializeCamera() {
		bootstrap.on(Window) {
			val camera = OrthographicCamera()
			bootstrap[Camera] = camera
			bootstrap[Window].autoCenterCamera(camera)
		}
	}

	protected open fun initializeFiles() {
		bootstrap.waitFor(Files)
		bootstrap.on(JSON_KEY) {
			val json = bootstrap[JSON_KEY]
			val path = config.rootPath + config.assetsManifestPath.appendParam("version", config.version.toVersionString())
			val manifestLoader = JsTextLoader(path)
			manifestLoader.then {
				val manifest = json.read(it, FilesManifestSerializer)
				this[Files] = FilesImpl(manifest)
			}
		}
	}

	protected open fun initializeRequest() {
		RestServiceFactory.instance = JsRestServiceFactory
	}

	protected open fun initializeAssetManager() {
		bootstrap.on(Files) {
			val assetManager = AssetManagerImpl(config.rootPath, bootstrap[Files], appendVersion = true)
			assetManager.setLoaderFactory(AssetTypes.TEXT, { path: String, estimatedBytesTotal: Int ->  JsTextLoader(path, estimatedBytesTotal) })
			this[AssetManager] = assetManager
		}
	}

	abstract fun initializeTextures()

	protected open fun initializeAudio() {
		val audioManager = AudioManagerImpl()
		bootstrap[AudioManager] = audioManager

		bootstrap.on(AssetManager) {
			// JS Audio doesn't need to be updated like OpenAL audio does, so we don't add it to the TimeDriver.
			val assetManager = bootstrap[AssetManager]

			if (audioContextSupported) {
				assetManager.setLoaderFactory(AssetTypes.SOUND, { path: String, estimatedBytesTotal: Int ->  JsWebAudioSoundLoader(path, estimatedBytesTotal, audioManager) })
//				assetManager.setLoaderFactory(AssetTypes.MUSIC, { JsWebAudioMusicLoader(audioManager) })
			} else {
				assetManager.setLoaderFactory(AssetTypes.SOUND, { path: String, estimatedBytesTotal: Int ->  JsAudioElementSoundLoader(path, estimatedBytesTotal, audioManager) })
			}
			assetManager.setLoaderFactory(AssetTypes.MUSIC, { path: String, estimatedBytesTotal: Int ->  JsAudioElementMusicLoader(path, estimatedBytesTotal, audioManager) })
		}
	}

	protected open fun initializeTimeDriver() {
		val timeDriver = TimeDriverImpl()
		timeDriver.activate()
		bootstrap[TimeDriver] = timeDriver
	}

	protected open fun initializeInteractivity() {
		bootstrap.on(MouseInput, KeyInput, FocusManager) {
			bootstrap[InteractivityManager] = InteractivityManagerImpl(bootstrap[MouseInput], bootstrap[KeyInput], bootstrap[FocusManager])
		}
	}

	protected open fun initializeFocusManager() {
		bootstrap[FocusManager] = FocusManagerImpl()
	}

	protected open fun initializeCursorManager() {
		bootstrap[CursorManager] = JsCursorManager(canvas)
	}

	protected open fun initializePersistence() {
		bootstrap[Persistence] = JsPersistence(config.version)
	}

	protected open fun initializeSelectionManager() {
		bootstrap[SelectionManager] = SelectionManagerImpl()
	}

	protected open fun initializeTextFormatters() {
		bootstrap[NumberFormatter.FACTORY_KEY] = { NumberFormatterImpl(it) }
		bootstrap[DateTimeFormatter.FACTORY_KEY] = { DateTimeFormatterImpl(it) }
	}

	/**
	 * The last chance to set dependencies on the application scope.
	 */
	abstract fun initializeComponents()

	abstract fun initializeStage()

	protected open fun initializePopUpManager() {
		bootstrap[PopUpManager] = PopUpManagerImpl(bootstrap[Stage])
	}

	protected abstract fun initializeSpecialInteractivity()

	private fun memberRefTest() {}

	override fun dispose() {
		Log.info("Application#dispose")
		frameDriver?.stop()
		bootstrap.dispose()
	}
}

private external fun encodeURIComponent(str: String): String
private external fun decodeURIComponent(str: String): String

fun Int.toRadix(radix: Int): String {
	val d: dynamic = this
	return d.toString(radix)
}