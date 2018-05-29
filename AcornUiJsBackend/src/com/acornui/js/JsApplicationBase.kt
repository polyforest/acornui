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
import com.acornui.async.awaitOrNull
import com.acornui.async.launch
import com.acornui.browser.appendParam
import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import com.acornui.component.Stage
import com.acornui.component.UiComponent
import com.acornui.core.*
import com.acornui.core.assets.*
import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.AudioManagerImpl
import com.acornui.core.cursor.CursorManager
import com.acornui.core.di.*
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.FocusManagerImpl
import com.acornui.core.graphics.Camera
import com.acornui.core.graphics.OrthographicCamera
import com.acornui.core.graphics.Window
import com.acornui.core.graphics.autoCenterCamera
import com.acornui.core.i18n.Locale
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.InteractivityManagerImpl
import com.acornui.core.input.KeyInput
import com.acornui.core.input.MouseInput
import com.acornui.core.input.interaction.ContextMenuManager
import com.acornui.core.input.interaction.UndoDispatcher
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
import com.acornui.file.FileIoManager
import com.acornui.io.file.FilesManifestSerializer
import com.acornui.js.audio.JsAudioElementMusicLoader
import com.acornui.js.audio.JsAudioElementSoundLoader
import com.acornui.js.audio.JsWebAudioSoundLoader
import com.acornui.js.audio.audioContextSupported
import com.acornui.js.cursor.JsCursorManager
import com.acornui.js.file.JsFileIoManager
import com.acornui.js.input.JsClipboardDispatcher
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
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * The common setup tasks to both a webgl application and a dom application backend.
 */
@Suppress("unused")
abstract class JsApplicationBase : ApplicationBase() {

	protected abstract val isOpenGl: Boolean

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
	if (!!existing) {
		return existing;
	}
	var newBind = this.uncachedBind.apply(this, arguments);
	receiver.__bindingCache[this] = newBind;
	return newBind;
};
		""")


		@Suppress("LeakingThis")
		if (this::memberRefTest != this::memberRefTest) println("[SEVERE] Member reference fix isn't working.")
		time = TimeProviderImpl()
		encodeUriComponent2 = ::encodeURIComponent
		decodeUriComponent2 = ::decodeURIComponent

		window.onbeforeunload = { dispose(); null }
	}

	fun start(appConfig: AppConfig, onReady: Owned.() -> Unit) {
		launch {
			initializeConfig(appConfig)
			contentLoad()

			awaitAll()
			val injector = createInjector()
			stage = createStage(OwnedImpl(injector))
			val popUpManager = createPopUpManager(stage)
			val scope = stage.createScope(
					listOf(
							Stage to stage,
							PopUpManager to popUpManager
					)
			)
			initializeSpecialInteractivity(scope)
			scope.onReady()

			// Add the pop-up manager after onReady so that it is the highest index.
			stage.addElement(popUpManager.view)

			frameDriver = initializeFrameDriver(scope.injector)
			frameDriver!!.start()
		}
	}

	protected open suspend fun initializeConfig(appConfig: AppConfig) {
		// Copy the app config and set the build number and debug value.
		val path = appConfig.rootPath + "assets/build.txt".appendParam("version", UidUtil.createUid())
		val buildVersionLoader = JsTextLoader(path)
		val debug = appConfig.debug || (window.location.search.contains(Regex("""(?:&|\?)debug=(true|1)""")))
		val build = buildVersionLoader.awaitOrNull()
		val finalConfig = if (build != null) {
			appConfig.copy(debug = debug, version = appConfig.version.copy(build = build.toInt()))
		} else {
			Log.warn("assets/build.txt failed to load")
			appConfig.copy(debug = debug)
		}

		// Uncaught exception handler
		window.onerror = {
			message, source, lineNo, colNo, error ->
			val msg = "Error: $message $lineNo $source $colNo $error"
			Log.error(msg)
			if (finalConfig.debug)
				window.alert(msg)
		}

		// _assert
		assertionsEnabled = finalConfig.debug

		Log.info("Config $finalConfig")
		set(AppConfig, finalConfig)
	}

	protected open val userInfoTask by BootTask {
		val isTouchDevice = js("""'ontouchstart' in window || !!navigator.maxTouchPoints;""") as? Boolean ?: false

		val isMobile = js("""
			var check = false;
  (function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4))) check = true;})(navigator.userAgent||navigator.vendor||window.opera);
  check;
		""") as? Boolean ?: false

		@Suppress("USELESS_ELVIS")
		val languages = window.navigator.languages ?: arrayOf(window.navigator.language)

		@Suppress("SENSELESS_COMPARISON") // window.navigator.languages can be null.
		val uI = UserInfo(
				isTouchDevice = isTouchDevice,
				isBrowser = true,
				isOpenGl = isOpenGl,
				isMobile = isMobile,
				userAgent = window.navigator.userAgent,
				platformStr = window.navigator.platform,
				languages = languages.map { Locale(it) }
		)

		userInfo = uI
		set(UserInfo, uI)
	}

	private suspend fun contentLoad() {
		suspendCoroutine<Unit> {
			cont ->
			if (document.readyState == DocumentReadyState.LOADING) {
				document.addEventListener("DOMContentLoaded", {
					cont.resume(Unit)
				})
			} else {
				cont.resume(Unit)
			}
		}
	}

	abstract val canvasTask: suspend () -> Unit
	abstract val windowTask: suspend () -> Unit
	abstract val componentsTask: suspend () -> Unit

	protected open suspend fun initializeFrameDriver(injector: Injector): JsApplicationRunner {
		return JsApplicationRunnerImpl(injector)
	}

	protected open val loggingTask by BootTask {
		if (get(AppConfig).debug) {
			Log.level = ILogger.DEBUG
		} else {
			Log.level = ILogger.WARN
		}
	}

	protected open val bufferTask by BootTask {
		BufferFactory.instance = JsBufferFactory()
	}

	protected open val mouseInputTask by BootTask {
		set(MouseInput, JsMouseInput(get(CANVAS)))
	}

	protected open val keyInputTask by BootTask {
		set(KeyInput, JsKeyInput(get(CANVAS)))
	}

	protected open val jsonTask by BootTask {
		set(JSON_KEY, JsonSerializer)
	}

	protected open val cameraTask by BootTask {
		val camera = OrthographicCamera()
		set(Camera, camera)
		get(Window).autoCenterCamera(camera)
	}

	protected open val filesTask by BootTask {
		val json = get(JSON_KEY)
		val config = get(AppConfig)
		val path = config.rootPath + config.assetsManifestPath.appendParam("version", config.version.toVersionString())

		val it = JsTextLoader(path).await()
		val manifest = json.read(it, FilesManifestSerializer)
		set(Files, FilesImpl(manifest))
	}

	protected open val requestTask by BootTask {
		set(RestServiceFactory, JsRestServiceFactory)
	}

	protected open val assetManagerTask by BootTask {
		val config = get(AppConfig)
		val loaders = HashMap<AssetType<*>, LoaderFactory<*>>()
		addAssetLoaders(loaders)
		set(AssetManager, AssetManagerImpl(config.rootPath, get(Files), loaders, appendVersion = true))
	}

	protected open fun addAssetLoaders(loaders: HashMap<AssetType<*>, LoaderFactory<*>>) {
		loaders[AssetType.TEXT] = { path: String, estimatedBytesTotal: Int -> JsTextLoader(path, estimatedBytesTotal) }

		// JS Audio doesn't need to be updated like OpenAL audio does, so we don't add it to the TimeDriver.
		val audioManager = AudioManagerImpl()
		set(AudioManager, audioManager)
		loaders[AssetType.SOUND] = if (audioContextSupported) {
			{ path: String, estimatedBytesTotal: Int -> JsWebAudioSoundLoader(path, estimatedBytesTotal, audioManager) }
		} else {
			{ path: String, estimatedBytesTotal: Int -> JsAudioElementSoundLoader(path, estimatedBytesTotal, audioManager) }
		}
		loaders[AssetType.MUSIC] = { path: String, estimatedBytesTotal: Int -> JsAudioElementMusicLoader(path, estimatedBytesTotal, audioManager) }
	}

	protected open val timeDriverTask by BootTask {
		set(TimeDriver, TimeDriverImpl())
	}

	protected open val interactivityTask by BootTask {
		set(InteractivityManager, InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager)))
	}

	protected open val focusManagerTask by BootTask {
		set(FocusManager, FocusManagerImpl())
	}

	protected open val cursorManagerTask by BootTask {
		set(CursorManager, JsCursorManager(get(CANVAS)))
	}

	protected open val persistenceTask by BootTask {
		set(Persistence, JsPersistence(get(AppConfig).version))
	}

	protected open val selectionManagerTask by BootTask {
		set(SelectionManager, SelectionManagerImpl())
	}

	protected open val textFormattersTask by BootTask {
		set(NumberFormatter.FACTORY_KEY, { NumberFormatterImpl(it) })
		set(DateTimeFormatter.FACTORY_KEY, { DateTimeFormatterImpl(it) })
	}

	protected open val fileIoManagerTask by BootTask {
		set(FileIoManager, JsFileIoManager())
	}

	abstract suspend fun createStage(owner: Owned): Stage

	protected open suspend fun createPopUpManager(root: UiComponent): PopUpManager {
		return PopUpManagerImpl(root)
	}

	protected open suspend fun initializeSpecialInteractivity(owner: Owned) {
		owner.own(JsClipboardDispatcher(owner.inject(CANVAS), owner.injector))
		owner.own(UndoDispatcher(owner.injector))
		owner.own(ContextMenuManager(owner))
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

private external fun encodeURIComponent(str: String): String
private external fun decodeURIComponent(str: String): String

fun Int.toRadix(radix: Int): String {
	val d: dynamic = this
	return d.toString(radix)
}