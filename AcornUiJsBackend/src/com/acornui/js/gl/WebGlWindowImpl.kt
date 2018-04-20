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

package com.acornui.js.gl

import com.acornui.core.WindowConfig
import com.acornui.core.browser.Location
import com.acornui.core.graphics.Window
import com.acornui.gl.core.Gl20
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.js.window.JsLocation
import com.acornui.logging.Log
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window

/**
 * @author nbilyk
 */
open class WebGlWindowImpl(
		private val canvas: HTMLCanvasElement,
		config: WindowConfig,
		private val gl: Gl20) : Window {

	override val isActiveChanged: Signal1<Boolean> = Signal1()
	override val isVisibleChanged: Signal1<Boolean> = Signal1()
	override val sizeChanged: Signal3<Float, Float, Boolean> = Signal3()
	override val scaleChanged: Signal2<Float, Float> = Signal2()

	private var _width: Float = 0f
	private var _height: Float = 0f
	private var sizeIsDirty: Boolean = true

	// Visibility properties
	private var _isVisible: Boolean = true
	private var hiddenProp: String? = null
	private val hiddenPropEventMap = hashMapOf(
			"hidden" to "visibilitychange",
			"mozHidden" to "mozvisibilitychange",
			"webkitHidden" to "webkitvisibilitychange",
			"msHidden" to "msvisibilitychange")

	private val visibilityChangeHandler = {
		event: Event? ->
		isVisible(document[hiddenProp!!] != true)
	}

	// TODO: Study context loss
	// getExtension( 'WEBGL_lose_context' ).loseContext();
	private val webGlContextRestoredHandler = {
		event: Event ->
		//		event.preventDefault()
		Log.info("WebGL context lost")
		requestRender()
	}

	private val blurHandler = {
		event: Event ->
		isActive(false)
	}

	private val focusHandler = {
		event: Event ->
		isActive(true)
	}

	private val resizeHandler = {
		event: Event ->
		setSize(canvas.offsetWidth.toFloat(), canvas.offsetHeight.toFloat(), true)
	}

	init {
		setSize(canvas.offsetWidth.toFloat(), canvas.offsetHeight.toFloat(), true)

		window.addEventListener("resize", resizeHandler)
		canvas.addEventListener("webglcontextrestored", webGlContextRestoredHandler)
		window.addEventListener("blur", blurHandler)
		window.addEventListener("focus", focusHandler)

		canvas.addEventListener("selectstart", { it.preventDefault() })
		if (config.title.isNotEmpty())
			document.title = config.title

		watchForVisibilityChanges()
	}

	private fun watchForVisibilityChanges() {
		hiddenProp = null
		if (js("'hidden' in document")) {
			hiddenProp = "hidden"
		} else if (js("'mozHidden' in document")) {
			hiddenProp = "mozHidden"
		} else if (js("'webkitHidden' in document")) {
			hiddenProp = "webkitHidden"
		} else if (js("'msHidden' in document")) {
			hiddenProp = "msHidden"
		}
		if (hiddenProp != null) {
			document.addEventListener(hiddenPropEventMap[hiddenProp!!]!!, visibilityChangeHandler)
			visibilityChangeHandler(null)
		}
	}

	private fun unwatchVisibilityChanges() {
		if (hiddenProp != null) {
			document.removeEventListener(hiddenPropEventMap[hiddenProp!!]!!, visibilityChangeHandler)
			hiddenProp = null
		}
	}

	override fun isVisible(): Boolean {
		return _isVisible
	}

	private fun isVisible(value: Boolean) {
		if (_isVisible == value) return
		_isVisible = value
		isVisibleChanged.dispatch(value)
	}

	private var _isActive: Boolean = true

	override val isActive: Boolean
		get() {
			return _isActive
		}

	private fun isActive(value: Boolean) {
		if (_isActive == value) return
		_isActive = value
		isActiveChanged.dispatch(value)
	}

	override val width: Float
		get() {
			return _width
		}

	override val height: Float
		get() {
			return _height
		}

	override val scaleX: Float
		get() = 1f
	override val scaleY: Float
		get() = 1f

	override fun setSize(width: Float, height: Float, isUserInteraction: Boolean) {
		if (_width == width && _height == height) return // no-op
		_width = width
		_height = height
		if (!isUserInteraction) {
			canvas.style.width = "${_width.toInt()}px"
			canvas.style.height = "${_height.toInt()}px"
		}

		sizeIsDirty = true
		sizeChanged.dispatch(_width, _height, isUserInteraction)
	}

	private var _clearColor = Color.CLEAR.copy()

	override var clearColor: ColorRo
		get() = _clearColor
		set(value) {
			_clearColor.set(value)
			gl.clearColor(value)
			requestRender()
		}

	override var continuousRendering: Boolean = false
	private var _renderRequested: Boolean = true

	override fun shouldRender(clearRenderRequest: Boolean): Boolean {
		val bool = continuousRendering || _renderRequested
		if (clearRenderRequest && _renderRequested) _renderRequested = false
		return bool
	}

	override fun requestRender() {
		if (_renderRequested) return
		_renderRequested = true
	}

	override fun renderBegin() {
		if (sizeIsDirty) {
			sizeIsDirty = false
			canvas.width = _width.toInt()
			canvas.height = _height.toInt()
		}
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
	}

	override fun renderEnd() {
	}

	private var _closeRequested = false

	override fun isCloseRequested(): Boolean {
		return _closeRequested
	}

	// TODO: Implement. Pop-ups can be closed
	override fun requestClose() {
		_closeRequested = true
	}

	private var _fullScreen = false
	override var fullScreen: Boolean
		get() = _fullScreen
		set(value) {
			if (value == _fullScreen) return
			_fullScreen = value
//			if (value) {
//				canvas.requestFullscreen()
//			} else {
//				document.exitFullscreen()
//			}
		}

	private val _location by lazy { JsLocation(window.location) }
	override val location: Location
		get() = _location

	override fun dispose() {
		sizeChanged.dispose()

		window.removeEventListener("resize", resizeHandler)
		canvas.removeEventListener("webglcontextlost", webGlContextRestoredHandler)
		window.removeEventListener("blur", blurHandler)
		window.removeEventListener("focus", focusHandler)
		unwatchVisibilityChanges()
	}
}