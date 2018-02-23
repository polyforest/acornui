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

package com.acornui.jvm

import com.acornui.assertionsEnabled
import com.acornui.async.launch
import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import com.acornui.component.*
import com.acornui.component.scroll.ScrollArea
import com.acornui.component.scroll.ScrollRect
import com.acornui.component.text.EditableTextField
import com.acornui.component.text.TextArea
import com.acornui.component.text.TextField
import com.acornui.component.text.TextInput
import com.acornui.core.*
import com.acornui.core.assets.*
import com.acornui.core.audio.AudioManager
import com.acornui.core.cursor.CursorManager
import com.acornui.core.di.*
import com.acornui.core.focus.FakeFocusMouse
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
import com.acornui.core.input.interaction.JvmClickDispatcher
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
import com.acornui.gl.component.*
import com.acornui.gl.component.text.*
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.io.file.FilesManifestSerializer
import com.acornui.jvm.audio.NoAudioException
import com.acornui.jvm.audio.OpenAlAudioManager
import com.acornui.jvm.audio.OpenAlMusicLoader
import com.acornui.jvm.audio.OpenAlSoundLoader
import com.acornui.jvm.cursor.JvmCursorManager
import com.acornui.jvm.graphics.GlfwWindowImpl
import com.acornui.jvm.graphics.JvmGl20Debug
import com.acornui.jvm.graphics.JvmTextureLoader
import com.acornui.jvm.graphics.LwjglGl20
import com.acornui.jvm.input.JvmClipboardDispatcher
import com.acornui.jvm.input.JvmMouseInput
import com.acornui.jvm.input.LwjglKeyInput
import com.acornui.jvm.io.JvmBufferFactory
import com.acornui.jvm.io.JvmRestServiceFactory
import com.acornui.jvm.loader.JvmTextLoader
import com.acornui.jvm.loader.WorkScheduler
import com.acornui.jvm.persistance.LwjglPersistence
import com.acornui.jvm.text.DateTimeFormatterImpl
import com.acornui.jvm.text.NumberFormatterImpl
import com.acornui.jvm.time.TimeProviderImpl
import com.acornui.logging.ILogger
import com.acornui.logging.Log
import com.acornui.serialization.JsonSerializer
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWWindowRefreshCallback
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale as LocaleJvm

/**
 * @author nbilyk
 */
@Suppress("unused")
open class LwjglApplication : ApplicationBase() {

	protected suspend fun config(): AppConfig = get(AppConfig)

	// If accessing the window id, use bootstrap.on(Window) { }
	private var _windowId: Long = -1L

	private suspend fun getWindowId(): Long {
		get(Window) // Ensure that the Window has been set.
		return _windowId
	}

	companion object {
		init {
			lineSeparator = System.lineSeparator()

			encodeUriComponent2 = {
				str ->
				URLEncoder.encode(str, "UTF-8")
			}
			decodeUriComponent2 = {
				str ->
				URLDecoder.decode(str, "UTF-8")
			}

			time = TimeProviderImpl()
			BufferFactory.instance = JvmBufferFactory()
		}
	}

	fun start(config: AppConfig = AppConfig(),
			  onReady: Owned.() -> Unit) {

		initializeConfig(config)
		launch {
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

			run(scope.injector)
		}
	}

	protected open fun initializeConfig(config: AppConfig) {
		val buildVersion = File("assets/build.txt")
		val build = if (buildVersion.exists()) buildVersion.readText().toInt() else config.version.build

		val finalConfig = config.copy(
				version = config.version.copy(build = build),
				debug = config.debug || System.getProperty("debug")?.toLowerCase() == "true"
		)
		if (finalConfig.debug) {
			assertionsEnabled = true
		}
		if (finalConfig.debug) {
			Log.level = ILogger.DEBUG
		} else {
			Log.level = ILogger.INFO
		}

		println("LWJGL Version: ${Version.getVersion()}")

		Log.info("Config $finalConfig")
		set(AppConfig, finalConfig)
	}

	/**
	 * Sets the UserInfo dependency.
	 */
	protected open val userInfoTask by BootTask {
		val u = UserInfo(
				isOpenGl = true,
				isDesktop = true,
				isTouchDevice = false,
				languages = listOf(Locale(LocaleJvm.getDefault().toLanguageTag()))
		)
		userInfo = u
		set(UserInfo, u)
	}


	/**
	 * Sets the [JSON_KEY] dependency.
	 */
	protected open val jsonTask by BootTask {
		set(JSON_KEY, JsonSerializer)
	}

	/**
	 * Sets the [Gl20] dependency.
	 */
	protected open val glTask by BootTask {
		set(Gl20, if (config().debug) JvmGl20Debug() else LwjglGl20())
	}

	/**
	 * Sets the [Window] dependency.
	 */
	protected open val windowTask by BootTask {
		val config = config()
		val window = GlfwWindowImpl(config.window, config.gl, get(Gl20), config.debug)
		_windowId = window.windowId
		set(Window, window)
	}

	protected open val glStateTask by BootTask {
		get(Window) // Shaders need a window to be created first.
		set(GlState, GlState(get(Gl20)))
	}

	protected open val mouseInputTask by BootTask {
		set(MouseInput, JvmMouseInput(getWindowId()))
	}

	protected open val keyInputTask by BootTask {
		set(KeyInput, LwjglKeyInput(getWindowId()))
	}

	protected open val cameraTask by BootTask {
		val camera = OrthographicCamera()
		set(Camera, camera)
		get(Window).autoCenterCamera(camera)
	}

	protected open val filesTask by BootTask {
		val manifestFile = File(config().rootPath + config().assetsManifestPath)
		if (!manifestFile.exists()) throw FileNotFoundException(config().rootPath + config().assetsManifestPath)
		val reader = FileReader(manifestFile)
		val jsonStr = reader.readText()
		val files = FilesImpl(JsonSerializer.read(jsonStr, FilesManifestSerializer))
		set(Files, files)
	}

	protected open val requestTask by BootTask {
		set(RestServiceFactory, JvmRestServiceFactory)
	}

	protected open val focusManagerTask by BootTask {
		set(FocusManager, FocusManagerImpl())
	}

	private fun <T> ioWorkScheduler(timeDriver: TimeDriver): WorkScheduler<T> = { asyncThread(timeDriver, it) }

	protected open val assetManagerTask by BootTask {
		val gl20 = get(Gl20)
		val glState = get(GlState)
		val timeDriver = get(TimeDriver)

		val loaders = HashMap<AssetType<*>, LoaderFactory<*>>()
		loaders[AssetType.TEXTURE] = { path, _ -> JvmTextureLoader(path, gl20, glState, ioWorkScheduler(timeDriver)) }
		loaders[AssetType.TEXT] = { path, _ -> JvmTextLoader(path, Charsets.UTF_8, ioWorkScheduler(timeDriver)) }

		// Audio
		try {
			val audioManager = OpenAlAudioManager()
			set(AudioManager, audioManager)
			timeDriver.addChild(audioManager)
			OpenAlSoundLoader.registerDefaultDecoders()
			loaders[AssetType.SOUND] = { path, _ -> OpenAlSoundLoader(path, audioManager, ioWorkScheduler(timeDriver)) }

			OpenAlMusicLoader.registerDefaultDecoders()
			loaders[AssetType.MUSIC] = { path, _ -> OpenAlMusicLoader(path, audioManager) }
		} catch (e: NoAudioException) {
			Log.warn("No Audio device found.")
		}

		set(AssetManager, AssetManagerImpl(config().rootPath, get(Files), loaders))
	}

	protected open val interactivityTask by BootTask {
		set(InteractivityManager, InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager)))
	}

	protected open val timeDriverTask by BootTask {
		set(TimeDriver, TimeDriverImpl())
	}

	protected open val cursorManagerTask by BootTask {
		set(CursorManager, JvmCursorManager(get(AssetManager), getWindowId()))
	}

	protected open val selectionManagerTask by BootTask {
		set(SelectionManager, SelectionManagerImpl())
	}

	protected open val persistenceTask by BootTask {
		set(Persistence, LwjglPersistence(config().version, config().window.title))
	}

	protected open val formattersTask by BootTask {
		set(NumberFormatter.FACTORY_KEY, { NumberFormatterImpl(it) })
		set(DateTimeFormatter.FACTORY_KEY, { DateTimeFormatterImpl(it) })
	}

	/**
	 * The last chance to set dependencies on the application scope.
	 */
	protected open val componentsTask by BootTask {
		set(NativeComponent.FACTORY_KEY, { NativeComponentDummy })
		set(NativeContainer.FACTORY_KEY, { NativeContainerDummy })
		set(TextField.FACTORY_KEY, ::GlTextField)
		set(EditableTextField.FACTORY_KEY, ::GlEditableTextField)
		set(TextInput.FACTORY_KEY, ::GlTextInput)
		set(TextArea.FACTORY_KEY, ::GlTextArea)
		set(TextureComponent.FACTORY_KEY, ::GlTextureComponent)
		set(ScrollArea.FACTORY_KEY, ::GlScrollArea)
		set(ScrollRect.FACTORY_KEY, ::GlScrollRect)
		set(Rect.FACTORY_KEY, ::GlRect)

		set(HtmlComponent.FACTORY_KEY, { object : UiComponentImpl(it), HtmlComponent {

			override val boxStyle = BoxStyle()
			override var html: String = ""
		} })
	}

	protected open fun createStage(owned: Owned): Stage {
		return GlStageImpl(owned)
	}

	protected open fun createPopUpManager(root: UiComponent): PopUpManager {
		return PopUpManagerImpl(root)
	}

	protected open fun initializeSpecialInteractivity(owner: Owned) {
		JvmClickDispatcher(owner.injector)
		FakeFocusMouse(owner.injector)
		JvmClipboardDispatcher(owner.injector)
		UndoDispatcher(owner.injector)
	}

	open suspend fun run(injector: Injector) {
		JvmApplicationRunner(injector, getWindowId())
		dispose()
	}


	override fun dispose() {
		BitmapFontRegistry.dispose()
		super.dispose()
	}
}

class JvmApplicationRunner(
		override val injector: Injector,
		windowId: Long
) : Scoped {

	private val window = inject(Window)
	private val appConfig = inject(AppConfig)
	private val timeDriver = inject(TimeDriver)
	private val stage = inject(Stage)

	private val refreshCallback = object : GLFWWindowRefreshCallback() {
		override fun invoke(windowId: Long) {
			window.requestRender()
			tick()
		}
	}

	init {
		Log.info("Application#startIndex")

		stage.activate()

		// The window has been damaged.
		GLFW.glfwSetWindowRefreshCallback(windowId, refreshCallback)
		var timeMs = time.nowMs()
		while (!window.isCloseRequested()) {
			// Poll for window events. Input callbacks will be invoked at this time.
			GLFW.glfwPollEvents()
			tick()
			val t = time.nowMs()
			val sleepTime = (appConfig.stepTime * 1000f - (t - timeMs)).toLong()
			if (sleepTime > 0) Thread.sleep(sleepTime)
			timeMs = t
		}
		GLFW.glfwSetWindowRefreshCallback(windowId, null)
	}

	private var nextTick: Long = 0

	private fun tick() {
		val stepTimeFloat = appConfig.stepTime
		val stepTimeMs = 1000 / appConfig.frameRate
		var loops = 0
		val now: Long = time.msElapsed()
		// Do a best attempt to keep the time driver in sync, but stage updates and renders may be sacrificed.
		while (now > nextTick) {
			nextTick += stepTimeMs
			timeDriver.update(stepTimeFloat)
			if (++loops > MAX_FRAME_SKIP) {
				// If we're too far behind, break and reset.
				nextTick = time.msElapsed() + stepTimeMs
				break
			}
		}
		if (window.shouldRender(true)) {
			stage.update()
			window.renderBegin()
			if (stage.visible)
				stage.render()
			window.renderEnd()
		}
	}

	companion object {

		/**
		 * The maximum number of update() calls before a render is required.
		 */
		private val MAX_FRAME_SKIP = 10
	}

}