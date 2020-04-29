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
import com.acornui.audio.AudioManager
import com.acornui.audio.Music
import com.acornui.audio.SoundFactory
import com.acornui.browser.Location
import com.acornui.component.BoxStyle
import com.acornui.component.HtmlComponent
import com.acornui.component.Stage
import com.acornui.component.UiComponentImpl
import com.acornui.cursor.CursorManager
import com.acornui.di.Context
import com.acornui.error.stack
import com.acornui.file.FileIoManager
import com.acornui.focus.FocusManager
import com.acornui.focus.FocusManagerImpl
import com.acornui.gl.core.CachedGl20
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.Gl20CachedImpl
import com.acornui.gl.core.WrappedGl20
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture
import com.acornui.graphic.Window
import com.acornui.input.*
import com.acornui.input.interaction.ContextMenuManager
import com.acornui.input.interaction.JvmClickDispatcher
import com.acornui.input.interaction.UndoDispatcher
import com.acornui.io.*
import com.acornui.logging.Log
import com.acornui.lwjgl.audio.*
import com.acornui.lwjgl.browser.JvmLocation
import com.acornui.lwjgl.cursor.JvmCursorManager
import com.acornui.lwjgl.files.JvmFileIoManager
import com.acornui.lwjgl.glfw.GlfwWindowImpl
import com.acornui.lwjgl.input.GlfwKeyInput
import com.acornui.lwjgl.input.GlfwMouseInput
import com.acornui.lwjgl.input.JvmClipboard
import com.acornui.lwjgl.opengl.LwjglGl20
import com.acornui.lwjgl.opengl.getErrorString
import com.acornui.lwjgl.opengl.loadTexture
import com.acornui.persistence.JvmPersistence
import com.acornui.persistence.Persistence
import com.acornui.time.FrameDriverRo
import kotlinx.coroutines.Job
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import kotlin.system.exitProcess
import kotlin.time.seconds
import org.lwjgl.Version as LwjglVersion

/**
 * @author nbilyk
 */
@Suppress("unused")
open class LwjglApplication(mainContext: MainContext) : ApplicationBase(mainContext) {

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
	}

	override suspend fun onStageCreated(stage: Stage) {
		super.onStageCreated(stage)
		initializeSpecialInteractivity(stage)
	}

	override suspend fun createLooper(owner: Context): ApplicationLooper {
		val pollEvents = mainContext.looper.pollEvents
		if (!pollEvents.contains(GLFW::glfwPollEvents))
			pollEvents.add(GLFW::glfwPollEvents)
		return super.createLooper(owner)
	}

	/**
	 * Sets the [Window] dependency.
	 */
	private val windowTask by task(Window) {
		val config = config()
		val window = GlfwWindowImpl(config.window, config.gl)
		_windowId = window.windowId
		window
	}
	
	private val locationTask by task(Location) {
		JvmLocation()
	}

	/**
	 * Sets the [CachedGl20] dependency.
	 */
	protected open val glTask by task(CachedGl20) {
		val config = config()
		val window = get(Window)
		val innerGl = WrappedGl20(
				wrapped = LwjglGl20(),
				before = {
					window.makeCurrent()
				},
				after = if (debug) { {
					val errorCode = GL11.glGetError()
					if (errorCode != GL11.GL_NO_ERROR) {
						error("GL ERROR: code: $errorCode ${getErrorString(errorCode)}")
					}
				} } else { {} }
		)
//		val gl = Gl20CachedImpl(if (debug) JvmGl20Debug() else LwjglGl20())
		val gl = Gl20CachedImpl(innerGl)
		// Clear as soon as possible to avoid frames of black
		gl.clearColor(config.window.backgroundColor)
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
		gl
	}

	protected open val uncaughtExceptionHandlerTask by task(uncaughtExceptionHandlerKey) {
		val version = get(Version)
		val window = get(Window)
		uncaughtExceptionHandler = {
			val message = it.stack + "\n${version.toVersionString()}"
			Log.error(message)
			if (debug)
				window.alert(message)
			exitProcess(1)
		}
		uncaughtExceptionHandler
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

	private val audioManagerTask by task(AudioManager, isOptional = true) {
		val audioManager = OpenAlAudioManager(get(FrameDriverRo))
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

	protected open val soundLoaderTask by task(Loaders.soundLoader) {
		val defaultSettings = get(defaultRequestSettingsKey)
		val audioManager = get(AudioManager) as OpenAlAudioManager
		object : Loader<SoundFactory> {
			override val requestSettings: RequestSettings =
					defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 100_000)

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): SoundFactory {
				return loadOpenAlSoundFactory(audioManager, requestData, settings)
			}
		}
	}

	protected open val musicLoaderTask by task(Loaders.musicLoader) {
		val defaultSettings = get(defaultRequestSettingsKey)
		val audioManager = get(AudioManager) as OpenAlAudioManager
		object : Loader<Music> {
			override val requestSettings: RequestSettings =
					defaultSettings.copy(initialTimeEstimate = 0.seconds) // Audio element is immediately returned.

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): Music {
				return loadOpenAlMusic(audioManager, requestData, settings)
			}
		}
	}

	protected open val interactivityTask by task(InteractivityManager) {
		InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager))
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
		val defaultSettings = get(defaultRequestSettingsKey)
		object : Loader<Texture> {
			override val requestSettings: RequestSettings =
					defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 100_000)

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): Texture {
				return loadTexture(gl, requestData, settings)
			}
		}
	}

	protected open val rgbDataLoader by task(Loaders.rgbDataLoader) {
		val defaultSettings = get(defaultRequestSettingsKey)

		object : Loader<RgbData> {
			override val requestSettings: RequestSettings =
					defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 100_000)

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): RgbData {
				return loadRgbData(requestData, settings)
			}
		}
	}

	protected open val cursorManagerTask by task(CursorManager) {
		JvmCursorManager(getWindowId(), get(Loaders.rgbDataLoader), applicationScope)
	}

	protected open fun initializeSpecialInteractivity(owner: Context) {
		JvmClickDispatcher(owner)
		UndoDispatcher(owner)
		ContextMenuManager(owner)
	}

	companion object {
		init {
			Thread.currentThread().setUncaughtExceptionHandler { _, exception ->
				uncaughtExceptionHandler(exception)
			}
			println("LWJGL Version: ${LwjglVersion.getVersion()}")
		}
	}
}

@Suppress("unused")
fun MainContext.lwjglApplication(appConfig: AppConfig = AppConfig(), onReady: Stage.() -> Unit): Job =
		LwjglApplication(this).startAsync(appConfig, onReady)