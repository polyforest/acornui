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

package com.acornui.lwjgl

import com.acornui.*
import com.acornui.asset.Loaders
import com.acornui.async.uiThread
import com.acornui.audio.AudioManager
import com.acornui.component.BoxStyle
import com.acornui.component.HtmlComponent
import com.acornui.component.UiComponentImpl
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
import com.acornui.io.file.FilesManifest
import com.acornui.logging.Log
import com.acornui.lwjgl.audio.NoAudioException
import com.acornui.lwjgl.audio.OpenAlAudioManager
import com.acornui.lwjgl.audio.registerDefaultMusicDecoders
import com.acornui.lwjgl.audio.registerDefaultSoundDecoders
import com.acornui.lwjgl.cursor.JvmCursorManager
import com.acornui.lwjgl.files.JvmFileIoManager
import com.acornui.lwjgl.glfw.GlfwWindowImpl
import com.acornui.lwjgl.input.GlfwKeyInput
import com.acornui.lwjgl.input.GlfwMouseInput
import com.acornui.lwjgl.input.JvmClipboard
import com.acornui.lwjgl.opengl.JvmGl20Debug
import com.acornui.lwjgl.opengl.LwjglGl20
import com.acornui.lwjgl.opengl.loadTexture
import com.acornui.persistence.JvmPersistence
import com.acornui.persistence.Persistence
import com.acornui.time.start
import kotlinx.coroutines.GlobalScope
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWWindowRefreshCallback
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.seconds
import org.lwjgl.Version as LwjglVersion

/**
 * @author nbilyk
 */
@Suppress("unused")
open class LwjglApplication(manifest: FilesManifest? = null) : ApplicationBase(manifest) {

	// If accessing the window id, use bootstrap.on(Window) { }
	private var _windowId: Long = -1L

	protected suspend fun getWindowId(): Long {
		get(Window) // Ensure that the Window has been set.
		return _windowId
	}

	init {
		if (macFirstThreadRestart()) {
			println("Restarting JVM with -XstartOnFirstThread")
			exitProcess(0)
		}
		uiThread = Thread.currentThread()
		Thread.currentThread().setUncaughtExceptionHandler { _, exception ->
			uncaughtExceptionHandler(exception)
		}
		println("LWJGL Version: ${LwjglVersion.getVersion()}")
	}

	override suspend fun start(appConfig: AppConfig, onReady: Owned.() -> Unit) {
		set(AppConfig, appConfig)
		val injector = createInjector()
		val owner = OwnedImpl(injector)
		owner.initializeSpecialInteractivity()
		owner.onReady()
		LwjglApplicationRunner(owner.injector, _windowId).run()
		owner.dispose()
		dispose()
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
	private val windowTask by task(Window) {
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
		GlfwMouseInput(getWindowId(), get(Window) as GlfwWindowImpl)
	}

	protected open val keyInputTask by task(KeyInput) {
		GlfwKeyInput(getWindowId())
	}

	private val audioManagerTask by task(AudioManager) {
		val audioManager = OpenAlAudioManager()
		// Audio
		try {
			registerDefaultMusicDecoders()
			registerDefaultSoundDecoders()
			audioManager.start()
		} catch (e: NoAudioException) {
			Log.warn("No Audio device found.")
		}
		audioManager
	}

	protected open val interactivityTask by task(InteractivityManager) {
		InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager))
	}

	protected open val cursorManagerTask by task(CursorManager) {
		JvmCursorManager(getWindowId(), GlobalScope) // TODO: Acorn scope
	}

	protected open val persistenceTask by task(Persistence) {
		JvmPersistence(get(Version), config().window.title)
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
			override val defaultInitialTimeEstimate: Duration
				get() = Bandwidth.downBpsInv.seconds * 100_000

			override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration): Texture {
				return loadTexture(gl, glState, requestData, progressReporter, initialTimeEstimate)
			}
		}
	}

	protected open val rgbDataLoader by task(Loaders.rgbDataLoader) {
		object : Loader<RgbData> {
			override val defaultInitialTimeEstimate: Duration
				get() = Bandwidth.downBpsInv.seconds * 100_000

			override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration): RgbData {
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

private class LwjglApplicationRunner(
		injector: Injector,
		private val windowId: Long
) : JvmApplicationRunner(injector) {

	private val window = inject(Window)

	private val refreshCallback = object : GLFWWindowRefreshCallback() {
		override fun invoke(windowId: Long) {
			// The window has been damaged.
			window.requestRender()
			tick(0f)
		}
	}

	override fun run() {
		GLFW.glfwSetWindowRefreshCallback(windowId, refreshCallback)
		super.run()
		GLFW.glfwSetWindowRefreshCallback(windowId, null)
	}

	override fun pollEvents() {
		super.pollEvents()
		GLFW.glfwPollEvents()
	}
}

