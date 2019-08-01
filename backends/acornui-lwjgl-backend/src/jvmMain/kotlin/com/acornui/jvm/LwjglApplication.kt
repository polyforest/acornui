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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.acornui.jvm

import com.acornui.async.launch
import com.acornui.component.*
import com.acornui.component.text.BitmapFontRegistry
import com.acornui.core.AppConfig
import com.acornui.core.ApplicationBase
import com.acornui.core.Version
import com.acornui.core.asset.AssetManager
import com.acornui.core.asset.AssetManagerImpl
import com.acornui.core.asset.AssetType
import com.acornui.core.asset.LoaderFactory
import com.acornui.core.audio.AudioManager
import com.acornui.core.cursor.CursorManager
import com.acornui.core.debug
import com.acornui.core.di.*
import com.acornui.core.focus.FakeFocusMouse
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.FocusManagerImpl
import com.acornui.core.graphic.Window
import com.acornui.core.input.*
import com.acornui.core.input.interaction.ContextMenuManager
import com.acornui.core.input.interaction.JvmClickDispatcher
import com.acornui.core.input.interaction.UndoDispatcher
import com.acornui.core.io.file.Files
import com.acornui.core.io.file.FilesImpl
import com.acornui.core.persistence.Persistence
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionManagerImpl
import com.acornui.core.time.TimeDriver
import com.acornui.core.time.TimeDriverImpl
import com.acornui.core.time.time
import com.acornui.error.stack
import com.acornui.file.FileIoManager
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.GlStateImpl
import com.acornui.io.file.FilesManifest
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
import com.acornui.jvm.input.GlfwMouseInput
import com.acornui.jvm.input.JvmClipboard
import com.acornui.jvm.input.LwjglKeyInput
import com.acornui.jvm.loader.JvmBinaryLoader
import com.acornui.jvm.loader.JvmTextLoader
import com.acornui.jvm.persistence.LwjglPersistence
import com.acornui.logging.Log
import com.acornui.serialization.parseJson
import com.acornui.uncaughtExceptionHandler
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWWindowRefreshCallback
import java.io.File
import java.io.FileNotFoundException
import org.lwjgl.Version as LwjglVersion

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

	init {
		Thread.currentThread().setUncaughtExceptionHandler { _, exception ->
			uncaughtExceptionHandler(exception)
		}
		println("LWJGL Version: ${LwjglVersion.getVersion()}")
	}

	fun start(appConfig: AppConfig = AppConfig(),
			  onReady: Owned.() -> Unit) {

		set(AppConfig, appConfig)

		var injector: Injector? = null
		launch {
			awaitAll()
			injector = createInjector()
		}

		while (true) {
			if (injector != null) {
				val owner = OwnedImpl(injector!!)
				owner.initializeSpecialInteractivity()
				owner.stage.onReady()
				JvmApplicationRunner(owner.injector, _windowId)
				owner.dispose()
				dispose()
				break
			} else {
				Thread.sleep(10L)
			}
		}

		//System.exit(-1)
	}

	/**
	 * Sets the [Gl20] dependency.
	 */
	protected open val glTask by task(Gl20) {
		if (debug) JvmGl20Debug() else LwjglGl20()
	}

	/**
	 * Sets the [Window] dependency.
	 */
	protected open val windowTask by task(Window) {
		val config = config()
		val window = GlfwWindowImpl(config.window, config.gl, get(Gl20), debug)
		_windowId = window.windowId
		window
	}

	protected open val uncaughtExceptionHandlerTask by task(uncaughtExceptionHandlerKey) {
		val version = get(Version)
		val window = get(Window)
		uncaughtExceptionHandler = {
			val message = it.stack + "\n${version.toVersionString()}"
			Log.error(message)
			if (debug)
				window.alert(message)
			System.exit(1)
		}
		uncaughtExceptionHandler
	}

	protected open val glStateTask by task(GlState) {
		get(Window) // Shaders need a window to be created first.
		GlStateImpl(get(Gl20), get(Window))
	}

	protected open val focusManagerTask by task(FocusManager) {
		FocusManagerImpl()
	}

	protected open val mouseInputTask by task(MouseInput) {
		GlfwMouseInput(getWindowId(), get(Window))
	}

	protected open val keyInputTask by task(KeyInput) {
		LwjglKeyInput(getWindowId())
	}

	override val filesTask by task(Files) {
		val manifestFile = File(config().rootPath + config().assetsManifestPath)
		if (!manifestFile.exists()) throw FileNotFoundException(manifestFile.absolutePath)
		val manifest = parseJson(manifestFile.readText(), FilesManifest.serializer())
		FilesImpl(manifest)
	}

	private val audioManagerTask by task(AudioManager) {
		val timeDriver = get(TimeDriver)
		// TODO: optional dependency task
		val audioManager = OpenAlAudioManager()
		timeDriver.addChild(audioManager)
		OpenAlSoundLoader.registerDefaultDecoders()

		// Audio
		try {
			OpenAlMusicLoader.registerDefaultDecoders()
		} catch (e: NoAudioException) {
			Log.warn("No Audio device found.")
		}
		audioManager
	}

	override val assetManagerTask by task(AssetManager) {
		val gl20 = get(Gl20)
		val glState = get(GlState)
		val audioManager = get(AudioManager) as OpenAlAudioManager

		val loaders = HashMap<AssetType<*>, LoaderFactory<*>>()
		loaders[AssetType.TEXTURE] = { path, _ -> JvmTextureLoader(path, gl20, glState) }
		loaders[AssetType.TEXT] = { path, _ -> JvmTextLoader(path, Charsets.UTF_8) }
		loaders[AssetType.BINARY] = { path, _ -> JvmBinaryLoader(path) }

		// Audio
		try {
			loaders[AssetType.SOUND] = { path, _ -> OpenAlSoundLoader(path, audioManager) }
			loaders[AssetType.MUSIC] = { path, _ -> OpenAlMusicLoader(path, audioManager) }
		} catch (e: NoAudioException) {
			Log.warn("No Audio device found.")
		}
		AssetManagerImpl(config().rootPath, get(Files), loaders)
	}

	protected open val interactivityTask by task(InteractivityManager) {
		InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager))
	}

	protected open val timeDriverTask by task(TimeDriver) {
		TimeDriverImpl(config().timeDriverConfig)
	}

	protected open val cursorManagerTask by task(CursorManager) {
		JvmCursorManager(get(AssetManager), getWindowId())
	}

	protected open val selectionManagerTask by task(SelectionManager) {
		SelectionManagerImpl()
	}

	protected open val persistenceTask by task(Persistence) {
		LwjglPersistence(get(Version), config().window.title)
	}

	protected open val fileReadWriteManagerTask by task(FileIoManager) {
		JvmFileIoManager()
	}

	protected open val componentsTask by task(HtmlComponent.FACTORY_KEY) {
		{ owner ->
			object : UiComponentImpl(owner), HtmlComponent {
				override val boxStyle = BoxStyle()
				override var html: String = ""
			}
		}
	}

	protected open val clipboardTask by task(Clipboard) {
		JvmClipboard(
				get(KeyInput),
				get(FocusManager),
				get(InteractivityManager),
				getWindowId()
		)
	}

	protected open fun Owned.initializeSpecialInteractivity() {
		own(JvmClickDispatcher(injector))
		own(FakeFocusMouse(injector))
		own(UndoDispatcher(injector))
		own(ContextMenuManager(injector))
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
				if (stage.visible)
					stage.render()
				window.renderEnd()
			}
		}
	}
}

