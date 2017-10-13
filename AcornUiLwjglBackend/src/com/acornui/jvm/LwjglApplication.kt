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
import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import com.acornui.component.*
import com.acornui.component.scroll.ScrollArea
import com.acornui.component.scroll.ScrollRect
import com.acornui.component.text.EditableTextField
import com.acornui.component.text.TextArea
import com.acornui.component.text.TextField
import com.acornui.component.text.TextInput
import com.acornui.core.AppConfig
import com.acornui.core.UserInfo
import com.acornui.core.assets.AssetManager
import com.acornui.core.assets.AssetManagerImpl
import com.acornui.core.assets.AssetTypes
import com.acornui.core.audio.AudioManager
import com.acornui.core.cursor.CursorManager
import com.acornui.core.di.*
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.FocusManagerImpl
import com.acornui.core.focus.fakeFocusMouse
import com.acornui.core.graphics.Camera
import com.acornui.core.graphics.OrthographicCamera
import com.acornui.core.graphics.Window
import com.acornui.core.graphics.autoCenterCamera
import com.acornui.core.i18n.Locale
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.InteractivityManagerImpl
import com.acornui.core.input.KeyInput
import com.acornui.core.input.MouseInput
import com.acornui.core.input.interaction.clickDispatcher
import com.acornui.core.io.BufferFactory
import com.acornui.core.io.JSON_KEY
import com.acornui.core.io.file.Files
import com.acornui.core.io.file.FilesImpl
import com.acornui.core.lineSeparator
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
import com.acornui.jvm.input.JvmMouseInput
import com.acornui.jvm.input.LwjglKeyInput
import com.acornui.jvm.io.JvmBufferFactory
import com.acornui.jvm.io.JvmRestServiceFactory
import com.acornui.jvm.loader.JvmTextLoader
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
open class LwjglApplication(
		private val config: AppConfig = AppConfig(),
		onReady: Owned.(stage: Stage) -> Unit
) {

	val bootstrap = Bootstrap()

	// If accessing the window id, use bootstrap.on(Window) { }
	protected var windowId: Long = -1

	init {
		initializeUserInfo()

		initializeConfig()
		initializeLwjglNative()
		bootstrap.on(AppConfig) {
			initializeBootstrap()
			bootstrap.then {
				bootstrap.lock()
				val stage = bootstrap[Stage]
				stage.onReady(stage)
				stage.addElement(bootstrap[PopUpManager].view)
				bootstrap.run()
			}
		}
	}

	protected open fun initializeUserInfo() {
		UserInfo.isDesktop = true
		UserInfo.isOpenGl = true
		UserInfo.isTouchDevice = false
		UserInfo.languages = listOf(Locale(LocaleJvm.getDefault().toLanguageTag()))
		println("UserInfo: $UserInfo")
	}

	protected open fun initializeConfig() {
		val buildVersion = File("assets/build.txt")
		if (buildVersion.exists()) {
			config.version.build = buildVersion.readText().toInt()
		}
		Log.info("Build ${config.version.toVersionString()}")
		bootstrap[AppConfig] = config
	}

	/**
	 * If the native libraries for lwjgl aren't included in the jar, set the path using
	 * `System.setProperty("org.lwjgl.librarypath", "your/natives/path")`
	 */
	protected open fun initializeLwjglNative() {
		println("LWJGL Version: ${Version.getVersion()}")
	}

	protected open fun initializeBootstrap() {
		initializeDebug()
		initializeSystem()
		initializeLogging()
		initializeNumber()
		initializeString()
		initializeTime()
		initializeBufferFactory()
		initializeJson()
		initializeGl()
		initializeWindow()
		initializeGlState()
		initializeMouseInput()
		initializeKeyInput()
		initializeCamera()
		initializeFiles()
		initializeRequest()
		initializeTimeDriver()
		initializeAssetManager()
		initializeTextures()
		initializeAudio()
		initializeFocusManager()
		initializeInteractivity()
		initializeCursorManager()
		initializeSelectionManager()
		initializePersistence()
		initializeTextFormatters()
		initializeComponents()

		bootstrap.then {
			initializeStage()
			initializePopUpManager()
			initializeSpecialInteractivity()
		}
	}

	protected open fun initializeDebug() {
		config.debug = config.debug || System.getProperty("debug")?.toLowerCase() == "true"
		if (config.debug) {
			assertionsEnabled = true
			Log.info("Assertions enabled")
		}
	}

	protected open fun initializeSystem() {
		lineSeparator = System.lineSeparator()
	}

	protected open fun initializeLogging() {
		if (config.debug) {
			Log.level = ILogger.DEBUG
		} else {
			Log.level = ILogger.INFO
		}
	}

	/**
	 * Initializes number constants and methods
	 */
	protected open fun initializeNumber() {
	}

	protected open fun initializeString() {
		encodeUriComponent2 = {
			str ->
			URLEncoder.encode(str, "UTF-8")
		}
		decodeUriComponent2 = {
			str ->
			URLDecoder.decode(str, "UTF-8")
		}
	}

	protected open fun initializeTime() {
		time = TimeProviderImpl()
	}

	protected open fun initializeBufferFactory() {
		BufferFactory.instance = JvmBufferFactory()
	}

	private fun initializeJson() {
		bootstrap[JSON_KEY] = JsonSerializer
	}

	protected open fun initializeGl() {
		bootstrap[Gl20] = if (config.debug) JvmGl20Debug() else LwjglGl20()
	}

	protected open fun initializeWindow() {
		bootstrap.on(Gl20) {
			val window = GlfwWindowImpl(config.window, config.gl, bootstrap[Gl20], config.debug)
			windowId = window.windowId
			bootstrap[Window] = window
		}
	}

	protected open fun initializeGlState() {
		bootstrap.on(Gl20) {
			bootstrap[GlState] = GlState(bootstrap[Gl20])
		}
	}

	protected open fun initializeMouseInput() {
		bootstrap.on(Window) {
			bootstrap[MouseInput] = JvmMouseInput(windowId)
		}
	}

	protected open fun initializeKeyInput() {
		bootstrap.on(Window) {
			bootstrap[KeyInput] = LwjglKeyInput(windowId)
		}
	}

	protected open fun initializeCamera() {
		bootstrap.on(Window) {
			val camera = OrthographicCamera()
			bootstrap[Camera] = camera
			bootstrap[Window].autoCenterCamera(camera)
		}
	}

	protected open fun initializeFiles() {
		val manifestFile = File(config.rootPath + config.assetsManifestPath)
		if (!manifestFile.exists()) throw FileNotFoundException(config.rootPath + config.assetsManifestPath)
		val reader = FileReader(manifestFile)
		val jsonStr = reader.readText()
		val files = FilesImpl(JsonSerializer.read(jsonStr, FilesManifestSerializer))
		bootstrap[Files] = files
	}

	protected open fun initializeRequest() {
		RestServiceFactory.instance = JvmRestServiceFactory
	}

	protected open fun initializeFocusManager() {
		bootstrap[FocusManager] = FocusManagerImpl()
	}

	protected open fun initializeAssetManager() {
		bootstrap.on(Files, TimeDriver) {
			val assetManager = AssetManagerImpl(config.rootPath, bootstrap[Files])
			val isAsync = true
			assetManager.setLoaderFactory(AssetTypes.TEXT, { path, _ -> JvmTextLoader(path, Charsets.UTF_8, isAsync) })
			bootstrap[AssetManager] = assetManager
		}
	}

	protected open fun initializeTextures() {
		bootstrap.on(AssetManager, Gl20, GlState, TimeDriver) {
			val isAsync = true
			bootstrap[AssetManager].setLoaderFactory(AssetTypes.TEXTURE, { path, _ ->  JvmTextureLoader(path, bootstrap[Gl20], bootstrap[GlState], isAsync) })
		}

	}

	protected open fun initializeAudio() {
		try {
			val audioManager = OpenAlAudioManager()
			bootstrap[AudioManager] = audioManager
			bootstrap.on(AssetManager, TimeDriver) {
				val timeDriver = bootstrap[TimeDriver]
				timeDriver.addChild(audioManager)
				val assetManager = bootstrap[AssetManager]
				val isAsync = true
				OpenAlSoundLoader.registerDefaultDecoders()
				assetManager.setLoaderFactory(AssetTypes.SOUND, { path, _ -> OpenAlSoundLoader(path, audioManager, isAsync) })

				OpenAlMusicLoader.registerDefaultDecoders()
				assetManager.setLoaderFactory(AssetTypes.MUSIC, { path, _ -> OpenAlMusicLoader(path, audioManager) })
			}
		} catch (e: NoAudioException) {
			Log.warn("No Audio device found.")
		}
	}

	protected open fun initializeInteractivity() {
		bootstrap.on(MouseInput, KeyInput, FocusManager) {
			bootstrap[InteractivityManager] = InteractivityManagerImpl(bootstrap[MouseInput], bootstrap[KeyInput], bootstrap[FocusManager])
		}
	}

	protected open fun initializeTimeDriver() {
		val timeDriver = TimeDriverImpl()
		timeDriver.activate()
		bootstrap[TimeDriver] = timeDriver
	}

	protected open fun initializeCursorManager() {
		bootstrap.on(AssetManager, Window) {
			bootstrap[CursorManager] = JvmCursorManager(bootstrap[AssetManager], windowId)
		}
	}

	protected open fun initializeSelectionManager() {
		bootstrap[SelectionManager] = SelectionManagerImpl()
	}

	protected open fun initializePersistence() {
		bootstrap[Persistence] = LwjglPersistence(config.version, config.window.title)
	}

	protected open fun initializeTextFormatters() {
		bootstrap[NumberFormatter.FACTORY_KEY] = { NumberFormatterImpl(it) }
		bootstrap[DateTimeFormatter.FACTORY_KEY] = { DateTimeFormatterImpl(it) }
	}

	/**
	 * The last chance to set dependencies on the application scope.
	 */
	protected open fun initializeComponents() {
		bootstrap[NativeComponent.FACTORY_KEY] = { NativeComponentDummy }
		bootstrap[NativeContainer.FACTORY_KEY] = { NativeContainerDummy }
		bootstrap[TextField.FACTORY_KEY] = ::GlTextField
		bootstrap[EditableTextField.FACTORY_KEY] = ::GlEditableTextField
		bootstrap[TextInput.FACTORY_KEY] = ::GlTextInput
		bootstrap[TextArea.FACTORY_KEY] = ::GlTextArea
		bootstrap[TextureComponent.FACTORY_KEY] = ::GlTextureComponent
		bootstrap[ScrollArea.FACTORY_KEY] = ::GlScrollArea
		bootstrap[ScrollRect.FACTORY_KEY] = ::GlScrollRect
		bootstrap[Rect.FACTORY_KEY] = ::GlRect
	}

	protected open fun initializeStage() {
		bootstrap[Stage] = GlStageImpl(OwnedImpl(null, bootstrap.injector))
	}

	protected open fun initializePopUpManager() {
		bootstrap[PopUpManager] = PopUpManagerImpl(bootstrap[Stage])
	}

	protected open fun initializeSpecialInteractivity() {
		val clickDispatcher = bootstrap.clickDispatcher()
		clickDispatcher.init()
		bootstrap.fakeFocusMouse()
	}

	open fun Scoped.run() {
		JvmApplicationRunner(injector, windowId)
		dispose()
	}

	private fun dispose() {
		println("Application#dispose")
		// Should be disposed in the reverse order they were created.
		BitmapFontRegistry.dispose()
		bootstrap.dispose()
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
		Log.info("Application#start")

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
			stage.render()
			window.renderEnd()
		}
		// TODO: capped frame rates?
	}

	companion object {

		/**
		 * The maximum number of update() calls before a render is required.
		 */
		private val MAX_FRAME_SKIP = 10
	}

}