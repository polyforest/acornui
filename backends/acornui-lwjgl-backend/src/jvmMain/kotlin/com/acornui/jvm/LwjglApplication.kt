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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.acornui.jvm

import com.acornui.assertionsEnabled
import com.acornui.async.coroutineDebugMode
import com.acornui.async.launch
import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import com.acornui.component.*
import com.acornui.component.text.BitmapFontRegistry
import com.acornui.core.*
import com.acornui.core.asset.AssetManager
import com.acornui.core.asset.AssetManagerImpl
import com.acornui.core.asset.AssetType
import com.acornui.core.asset.LoaderFactory
import com.acornui.core.audio.AudioManager
import com.acornui.core.cursor.CursorManager
import com.acornui.core.di.*
import com.acornui.core.focus.FakeFocusMouse
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.FocusManagerImpl
import com.acornui.core.graphic.Camera
import com.acornui.core.graphic.OrthographicCamera
import com.acornui.core.graphic.Window
import com.acornui.core.graphic.autoCenterCamera
import com.acornui.core.i18n.I18n
import com.acornui.core.i18n.I18nImpl
import com.acornui.core.i18n.Locale
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.InteractivityManagerImpl
import com.acornui.core.input.KeyInput
import com.acornui.core.input.MouseInput
import com.acornui.core.input.interaction.ContextMenuManager
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
import com.acornui.core.text.dateTimeFormatterProvider
import com.acornui.core.text.numberFormatterProvider
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.TimeDriverImpl
import com.acornui.core.time.time
import com.acornui.file.FileIoManager
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.GlStateImpl
import com.acornui.io.file.FilesManifestSerializer
import com.acornui.jvm.audio.NoAudioException
import com.acornui.jvm.audio.OpenAlAudioManager
import com.acornui.jvm.audio.OpenAlMusicLoader
import com.acornui.jvm.audio.OpenAlSoundLoader
import com.acornui.jvm.cursor.JvmCursorManager
import com.acornui.jvm.files.JvmFileIoManager
import com.acornui.jvm.graphic.GlfwWindowImpl
import com.acornui.jvm.graphic.JvmGl20Debug
import com.acornui.jvm.graphic.JvmTextureLoader
import com.acornui.jvm.graphic.LwjglGl20
import com.acornui.jvm.input.JvmClipboardDispatcher
import com.acornui.jvm.input.JvmMouseInput
import com.acornui.jvm.input.LwjglKeyInput
import com.acornui.jvm.io.JvmBufferFactory
import com.acornui.jvm.io.JvmRestServiceFactory
import com.acornui.jvm.loader.JvmBinaryLoader
import com.acornui.jvm.loader.JvmTextLoader
import com.acornui.jvm.loader.WorkScheduler
import com.acornui.jvm.persistance.LwjglPersistence
import com.acornui.jvm.text.DateTimeFormatterImpl
import com.acornui.jvm.text.NumberFormatterImpl
import com.acornui.jvm.time.TimeProviderImpl
import com.acornui.logging.Logger
import com.acornui.logging.Log
import com.acornui.math.MinMax
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

	// If accessing the window id, use bootstrap.on(Window) { }
	private var _windowId: Long = -1L

	protected suspend fun getWindowId(): Long {
		get(Window) // Ensure that the Window has been set.
		return _windowId
	}

	companion object {
		init {
			lineSeparator = System.lineSeparator()

			encodeUriComponent2 = { str ->
				URLEncoder.encode(str, "UTF-8")
			}
			decodeUriComponent2 = { str ->
				URLDecoder.decode(str, "UTF-8")
			}

			time = TimeProviderImpl()
			BufferFactory.instance = JvmBufferFactory()
		}
	}

	fun start(config: AppConfig = AppConfig(),
			  onReady: Owned.() -> Unit) {

		initializeConfig(config)
		var injector: Injector? = null
		launch {
			awaitAll()
			injector = createInjector()
		}

		while (true) {
			if (injector != null) {
				stage = createStage(OwnedImpl(injector!!))
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

				JvmApplicationRunner(scope.injector, _windowId)
				dispose()
				break
			} else {
				Thread.sleep(10L)
			}
		}

		//System.exit(-1)
	}

	protected open fun initializeConfig(config: AppConfig) {
		val buildVersion = File("assets/build.txt")
		val build = if (buildVersion.exists()) buildVersion.readText().toInt() else config.version.build

		val finalConfig = config.copy(
				version = config.version.copy(build = build),
				debug = config.debug || System.getProperty("debug")?.toLowerCase() == "true",
				debugCoroutines = config.debugCoroutines || System.getProperty("debugCoroutines")?.toLowerCase() == "true"
		)
		if (finalConfig.debug) assertionsEnabled = true
		if (finalConfig.debugCoroutines) coroutineDebugMode = true

		if (finalConfig.debug) {
			Log.level = Logger.DEBUG
		} else {
			Log.level = Logger.INFO
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
				isDesktop = true,
				isTouchDevice = false,
				userAgent = "glfw",
				platformStr = System.getProperty("os.name") ?: UserInfo.UNKNOWN_PLATFORM,
				systemLocale = listOf(Locale(LocaleJvm.getDefault().toLanguageTag()))
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
		set(GlState, GlStateImpl(get(Gl20), get(Window)))
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
		if (!manifestFile.exists()) throw FileNotFoundException(manifestFile.absolutePath)
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

	private fun <T> ioWorkScheduler(timeDriver: TimeDriver): WorkScheduler<T> = { asyncThread(timeDriver, work = it) }

	protected open val assetManagerTask by BootTask {
		val gl20 = get(Gl20)
		val glState = get(GlState)
		val timeDriver = get(TimeDriver)

		val loaders = HashMap<AssetType<*>, LoaderFactory<*>>()
		loaders[AssetType.TEXTURE] = { path, _ -> JvmTextureLoader(path, gl20, glState, ioWorkScheduler(timeDriver)) }
		loaders[AssetType.TEXT] = { path, _ -> JvmTextLoader(path, Charsets.UTF_8, ioWorkScheduler(timeDriver)) }
		loaders[AssetType.BINARY] = { path, _ -> JvmBinaryLoader(path, ioWorkScheduler(timeDriver)) }

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
		set(TimeDriver, TimeDriverImpl(config().timeDriverConfig))
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

	protected open val i18nTask by BootTask {
		get(UserInfo)
		set(I18n, I18nImpl())
	}

	protected open val formattersTask by BootTask {
		numberFormatterProvider = { NumberFormatterImpl() }
		dateTimeFormatterProvider = { DateTimeFormatterImpl() }
	}

	protected open val fileReadWriteManagerTask by BootTask {
		set(FileIoManager, JvmFileIoManager())
	}

	/**
	 * The last chance to set dependencies on the application scope.
	 */
	protected open val componentsTask by BootTask {
		set(HtmlComponent.FACTORY_KEY, {
			object : UiComponentImpl(it), HtmlComponent {

				override val boxStyle = BoxStyle()
				override var html: String = ""
			}
		})
	}

	protected open fun createStage(owned: Owned): Stage {
		return GlStageImpl(owned)
	}

	protected open fun createPopUpManager(root: UiComponent): PopUpManager {
		return PopUpManagerImpl(root)
	}

	protected open fun initializeSpecialInteractivity(owner: Owned) {
		owner.own(JvmClickDispatcher(owner.injector))
		owner.own(FakeFocusMouse(owner.injector))
		owner.own(JvmClipboardDispatcher(owner.injector, _windowId))
		owner.own(UndoDispatcher(owner.injector))
		owner.own(ContextMenuManager(owner))
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
	private val timeDriver = inject(TimeDriver)
	private val stage = inject(Stage)
	private val frameTime = inject(AppConfig).frameTime

	private val refreshCallback = object : GLFWWindowRefreshCallback() {
		override fun invoke(windowId: Long) {
			window.requestRender()
			tick()
		}
	}

	private val viewport = MinMax()

	init {
		Log.info("Application#startIndex")
		stage.activate()
		timeDriver.activate()

		// The window has been damaged.
		GLFW.glfwSetWindowRefreshCallback(windowId, refreshCallback)
		var timeMs = time.nowMs()
		val frameTimeMs = frameTime * 1000f
		while (!window.isCloseRequested()) {
			// Poll for window events. Input callbacks will be invoked at this time.
			tick()
			val dT = time.nowMs() - timeMs
			val sleepTime = maxOf(0L, (frameTimeMs - dT).toLong())
			if (sleepTime > 0) Thread.sleep(sleepTime)
			timeMs += dT + sleepTime
			GLFW.glfwPollEvents()
		}
		GLFW.glfwSetWindowRefreshCallback(windowId, null)
	}

	private fun tick() {
		timeDriver.update()
		if (window.shouldRender(true)) {
			stage.update()
			if (window.width > 0f && window.height > 0f) {
				window.renderBegin()
				if (stage.visible) {
					stage.render(viewport.set(0f, 0f, window.width, window.height))
				}
				window.renderEnd()
			}
		}
	}
}

