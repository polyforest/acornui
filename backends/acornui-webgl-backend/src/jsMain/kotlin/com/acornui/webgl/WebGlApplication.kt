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

package com.acornui.webgl

import com.acornui.*
import com.acornui.asset.Loaders
import com.acornui.component.HtmlComponent
import com.acornui.component.Stage
import com.acornui.di.Context
import com.acornui.di.contextKey
import com.acornui.error.stack
import com.acornui.file.FileIoManager
import com.acornui.focus.FocusManager
import com.acornui.focus.FocusManagerImpl
import com.acornui.gl.core.CachedGl20
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.Gl20CachedImpl
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture
import com.acornui.graphic.Window
import com.acornui.input.InteractivityManager
import com.acornui.input.SoftKeyboardManager
import com.acornui.input.SoftKeyboardManagerImpl
import com.acornui.io.Bandwidth
import com.acornui.io.Loader
import com.acornui.io.RequestSettings
import com.acornui.io.UrlRequestData
import com.acornui.js.BrowserApplicationBase
import com.acornui.js.file.JsFileIoManager
import com.acornui.js.html.JsHtmlComponent
import com.acornui.js.html.WebGl
import com.acornui.js.input.JsClickDispatcher
import com.acornui.logging.Log
import com.acornui.system.userInfo
import kotlinx.coroutines.Job
import org.khronos.webgl.WebGLContextAttributes
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.clear
import kotlin.time.seconds

/**
 * An Acorn UI application for browser-based web gl.
 * @author nbilyk
 */
@Suppress("unused")
open class WebGlApplication(mainContext: MainContext, private val rootId: String) : BrowserApplicationBase(mainContext) {

	private val rootElement: HTMLElement by lazy {
		(document.getElementById(rootId) as HTMLElement?) ?: throw Exception("The root element with id $rootId could not be found.")
	}

	override val canvasTask by task(CANVAS) {
		val root = rootElement
		root.style.setProperty("-webkit-tap-highlight-color", "rgba(0,0,0,0)")
		root.clear()
		val canvas = document.createElement("canvas") as HTMLCanvasElement
		canvas.tabIndex = 0
		canvas.style.setProperty("-webkit-tap-highlight-color", "rgba(0,0,0,0)")
		canvas.style.apply {
			width = "100%"
			height = "100%"
		}
		root.appendChild(canvas)
		canvas
	}

	override val windowTask by task(Window) {
		WebGlWindowImpl(get(CANVAS), config().window)
	}

	protected open val glTask by task(CachedGl20) {
		val config = config()
		val glConfig = config.gl
		val attributes = WebGLContextAttributes()
		attributes.alpha = glConfig.alpha
		attributes.antialias = glConfig.antialias
		attributes.depth = glConfig.depth
		attributes.stencil = glConfig.stencil
		attributes.premultipliedAlpha = false
		attributes.preserveDrawingBuffer = true

		val context = WebGl.getContext(get(CANVAS), attributes)
				?: throw Exception("Browser does not support WebGL") // TODO: Make this a better UX

		val gl = Gl20CachedImpl(if (glDebug) WebGl20Debug(context) else WebGl20(context))
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
			if (isDebug)
				window.alert(message)
		}
		uncaughtExceptionHandler
	}

	override val componentsTask by task(HtmlComponent.FACTORY_KEY) {
		{ JsHtmlComponent(it, rootElement) }
	}

	protected val focusManagerTask by task(FocusManager) {
		FocusManagerImpl(get(InteractivityManager))
	}

	protected open val fileIoManagerTask by task(FileIoManager) {
		JsFileIoManager(rootElement)
	}

	protected open val softKeyboardManagerTask by task(SoftKeyboardManager) {
		SoftKeyboardManagerImpl(get(FocusManager), get(InteractivityManager), get(CANVAS))
	}

	protected open val textureLoader by task(Loaders.textureLoader) {
		val gl = get(CachedGl20)

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
		val gl = get(CachedGl20)

		val defaultSettings = get(defaultRequestSettingsKey)

		object : Loader<RgbData> {
			override val requestSettings: RequestSettings =
					defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 100_000)

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): RgbData {
				return loadTexture(gl, requestData, settings).rgbData
			}
		}
	}

	override suspend fun initializeSpecialInteractivity(owner: Context) {
		super.initializeSpecialInteractivity(owner)
		JsClickDispatcher(owner, get(CANVAS))
	}

	override suspend fun onStageCreated(stage: Stage) {
		super.onStageCreated(stage)
		focusCanvasOnStageFocus(get(CANVAS), stage)
	}

	/**
	 * When the focused element changes, make sure the document's active element is the canvas.
	 */
	protected open fun focusCanvasOnStageFocus(canvas: HTMLCanvasElement, stage: Stage) {
//		var canvasHasFocus = false
//		canvas.addEventListener("blur", {
//			canvasHasFocus = false
//		})
//		canvas.addEventListener("focus", {
//			canvasHasFocus = true
//		})
//		stage.focusEvent(true).add {
//			if (!canvasHasFocus) {
//				canvas.focus()
//			}
//		}
		canvas.focus()
	}

	companion object {
		protected val CANVAS = contextKey<HTMLCanvasElement, HTMLElement>(BrowserApplicationBase.CANVAS)
	}
}

@Suppress("unused")
fun MainContext.webGlApplication(rootId: String, appConfig: AppConfig = AppConfig(), onReady: Stage.() -> Unit): Job =
		WebGlApplication(this, rootId).startAsync(appConfig, onReady)

/**
 * A flag for enabling getError() gl checks.
 * This is separate from [isDebug] because of the extremely high performance cost.
 */
val glDebug: Boolean by lazy {
	if (!userInfo.isBrowser) false
	else window.location.search.contains(Regex("""[&?]glDebug=(true|1)"""))
}