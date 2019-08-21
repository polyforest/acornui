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

import com.acornui.*
import com.acornui.asset.Loaders
import com.acornui.asset.load
import com.acornui.async.globalLaunch
import com.acornui.async.uiThread
import com.acornui.audio.AudioManager
import com.acornui.component.*
import com.acornui.component.text.BitmapFontRegistry
import com.acornui.cursor.CursorManager
import com.acornui.di.*
import com.acornui.error.stack
import com.acornui.file.FileIoManager
import com.acornui.focus.FakeFocusMouse
import com.acornui.focus.FocusManager
import com.acornui.focus.FocusManagerImpl
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.GlStateImpl
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture
import com.acornui.graphic.Window
import com.acornui.input.*
import com.acornui.input.interaction.ContextMenuManager
import com.acornui.input.interaction.JvmClickDispatcher
import com.acornui.input.interaction.UndoDispatcher
import com.acornui.io.*
import com.acornui.io.file.Files
import com.acornui.io.file.FilesImpl
import com.acornui.io.file.FilesManifest
import com.acornui.jvm.audio.NoAudioException
import com.acornui.jvm.audio.OpenAlAudioManager
import com.acornui.jvm.audio.registerDefaultMusicDecoders
import com.acornui.jvm.audio.registerDefaultSoundDecoders
import com.acornui.jvm.cursor.JvmCursorManager
import com.acornui.jvm.files.JvmFileIoManager
import com.acornui.jvm.glfw.GlfwWindowImpl
import com.acornui.jvm.input.GlfwMouseInput
import com.acornui.jvm.input.JvmClipboard
import com.acornui.jvm.input.LwjglKeyInput
import com.acornui.jvm.opengl.JvmGl20Debug
import com.acornui.jvm.opengl.LwjglGl20
import com.acornui.jvm.opengl.loadRgbData
import com.acornui.jvm.opengl.loadTexture
import com.acornui.jvm.persistence.LwjglPersistence
import com.acornui.logging.Log
import com.acornui.persistence.Persistence
import com.acornui.selection.SelectionManager
import com.acornui.selection.SelectionManagerImpl
import com.acornui.serialization.jsonParse
import com.acornui.time.FrameDriver
import com.acornui.time.nowMs
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWWindowRefreshCallback
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
		uiThread = Thread.currentThread()
		Thread.currentThread().setUncaughtExceptionHandler { _, exception ->
			uncaughtExceptionHandler(exception)
		}
		println("LWJGL Version: ${LwjglVersion.getVersion()}")
	}

	fun start(appConfig: AppConfig = AppConfig(),
			  onReady: Owned.() -> Unit) {

		set(AppConfig, appConfig)

		var injector: Injector? = null
		globalLaunch {
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
		val manifestJson = get(Loaders.textLoader).load(config().rootPath + config().assetsManifestPath)
		val manifest = jsonParse(FilesManifest.serializer(), manifestJson)
		FilesImpl(manifest)
	}

	private val audioManagerTask by task(AudioManager) {
		val audioManager = OpenAlAudioManager()
		// Audio
		try {
			registerDefaultMusicDecoders()
			registerDefaultSoundDecoders()
			FrameDriver.addChild(audioManager)
		} catch (e: NoAudioException) {
			Log.warn("No Audio device found.")
		}
		audioManager
	}
	
	protected open val interactivityTask by task(InteractivityManager) {
		InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager))
	}

	protected open val cursorManagerTask by task(CursorManager) {
		JvmCursorManager(getWindowId())
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

	protected open val textureLoader by task(Loaders.textureLoader) {
		val gl = get(Gl20)
		val glState = get(GlState)

		object : Loader<Texture> {
			override val defaultInitialTimeEstimate: Float
				get() = Bandwidth.downBpsInv * 100_000

			override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Float): Texture {
				return loadTexture(gl, glState, requestData, progressReporter, initialTimeEstimate)
			}
		}
	}

	protected open val rgbDataLoader by task(Loaders.rgbDataLoader) {
		object : Loader<RgbData> {
			override val defaultInitialTimeEstimate: Float
				get() = Bandwidth.downBpsInv * 100_000

			override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Float): RgbData {
				return loadRgbData(requestData, progressReporter, initialTimeEstimate)
			}
		}
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
	private val stage = inject(Stage)

	private val refreshCallback = object : GLFWWindowRefreshCallback() {
		override fun invoke(windowId: Long) {
			window.requestRender()
			tick(0f)
		}
	}

	init {
		Log.info("Application#startIndex")
		stage.activate()

		// The window has been damaged.
		GLFW.glfwSetWindowRefreshCallback(windowId, refreshCallback)
		var lastFrameMs = nowMs()
		val frameTimeMs = 1000 / inject(AppConfig).frameRate
		while (!window.isCloseRequested()) {
			// Poll for window events. Input callbacks will be invoked at this time.
			val now = nowMs()
			val dT = (now - lastFrameMs) / 1000f
			lastFrameMs = now
			tick(dT)
			val sleepTime = lastFrameMs + frameTimeMs - nowMs()
			if (sleepTime > 0) Thread.sleep(sleepTime)
			GLFW.glfwPollEvents()
		}
		GLFW.glfwSetWindowRefreshCallback(windowId, null)
	}

	private fun tick(dT: Float) {
		FrameDriver.update(dT)
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

