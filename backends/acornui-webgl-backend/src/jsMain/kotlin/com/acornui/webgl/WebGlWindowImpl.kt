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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "UnsafeCastFromDynamic")

package com.acornui.webgl

import com.acornui.WindowConfig
import com.acornui.function.as1
import com.acornui.graphic.Window
import com.acornui.logging.Log
import com.acornui.signal.*
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.floor

/**
 * @author nbilyk
 */
class WebGlWindowImpl(
		private val canvas: HTMLCanvasElement,
		config: WindowConfig) : Window {

	private val cancel = Cancel()
	private val _closeRequested = Signal1<Cancel>()
	override val closeRequested: Signal<(Cancel) -> Unit> = _closeRequested.asRo()

	private val _isActiveChanged = Signal1<Boolean>()
	override val isActiveChanged = _isActiveChanged.asRo()

	private val _isVisibleChanged = Signal1<Boolean>()
	override val isVisibleChanged = _isVisibleChanged.asRo()

	private val _sizeChanged = Signal2<Float, Float>()
	override val sizeChanged = _sizeChanged.asRo()

	private val _scaleChanged = Signal2<Float, Float>()
	override val scaleChanged = _scaleChanged.asRo()

	private val _refresh = Signal0()
	override val refresh = _refresh.asRo()

	override var width: Float = 0f
		private set

	override var height: Float = 0f
		private set

	private var sizeIsDirty: Boolean = true

	// Visibility properties
	private var hiddenProp: String? = null
	private val hiddenPropEventMap = hashMapOf(
			"hidden" to "visibilitychange",
			"mozHidden" to "mozvisibilitychange",
			"webkitHidden" to "webkitvisibilitychange",
			"msHidden" to "msvisibilitychange")
	
	private var scaleQuery = window.matchMedia("(resolution: ${window.devicePixelRatio}dppx")

	init {
		resizeHandler()
		window.addEventListener("resize", ::resizeHandler.as1)
		canvas.addEventListener("webglcontextrestored", ::webGlContextRestoredHandler.as1)
		window.addEventListener("blur", ::blurHandler.as1)
		window.addEventListener("focus", ::focusHandler.as1)

		// Watch for devicePixelRatio changes.
		scaleQuery.addListener(::scaleChangedHandler.as1)

		canvas.addEventListener("selectstart", { it.preventDefault() })
		if (config.title.isNotEmpty())
			document.title = config.title

		watchForVisibilityChanges()

		document.addEventListener("fullscreenchange", ::fullScreenChangedHandler.as1)
		val oBU = window.onbeforeunload
		window.onbeforeunload = { event ->
			oBU?.invoke(event)
			_closeRequested.dispatch(cancel.reset())
			if (cancel.canceled) {
				event.preventDefault()
				event.returnValue = ""
			}
			undefined // Necessary for ie11 not to alert user.
		}
	}

	private fun scaleChangedHandler() {
		val oldScaleX = scaleX
		val oldScaleY = scaleY
		val newScale = window.devicePixelRatio.toFloat()
		if (newScale == oldScaleX && newScale == oldScaleY) return
		Log.debug("Window scale changed to: $newScale")
		scaleX = newScale
		scaleY = newScale
		sizeIsDirty = true
		scaleQuery.removeListener(::scaleChangedHandler.as1)
		scaleQuery = window.matchMedia("(resolution: ${window.devicePixelRatio}dppx")
		scaleQuery.addListener(::scaleChangedHandler.as1)
		_scaleChanged.dispatch(newScale, newScale)
		requestRender()
	}

	// TODO: Study context loss
	// getExtension( 'WEBGL_lose_context' ).loseContext();
	private fun webGlContextRestoredHandler() {
		Log.info("WebGL context lost")
		_refresh.dispatch()
	}

	private fun blurHandler() {
		isActive = false
	}

	private fun focusHandler() {
		isActive = true
	}

	private fun fullScreenChangedHandler() {
		_fullScreenChanged.dispatch()
	}

	private fun resizeHandler() {
		val newWidth = canvas.offsetWidth.toFloat()
		val newHeight = canvas.offsetHeight.toFloat()
		if (newWidth == this.width && newHeight == this.height) return
		Log.debug("Window size changed to: $newWidth, $newHeight")
		this.width = newWidth
		this.height = newHeight
		sizeIsDirty = true
		_sizeChanged.dispatch(width, height)
		requestRender()
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
			document.addEventListener(hiddenPropEventMap[hiddenProp!!]!!, ::visibilityChangeHandler.as1)
			visibilityChangeHandler()
		}
	}

	private fun unwatchVisibilityChanges() {
		if (hiddenProp != null) {
			document.removeEventListener(hiddenPropEventMap[hiddenProp!!]!!, ::visibilityChangeHandler.as1)
			hiddenProp = null
		}
	}

	private fun visibilityChangeHandler() {
		isVisible = document[hiddenProp!!] != true
	}

	override var isVisible: Boolean = true
		private set(value) {
			field = value
			_isVisibleChanged.dispatch(value)
		}

	override var isActive: Boolean = true
		private set(value) {
			field = value
			_isActiveChanged.dispatch(value)
		}

	override val framebufferWidth: Int
		get() = (width * scaleX + 0.5).toInt()

	override val framebufferHeight: Int
		get() = (height * scaleY + 0.5).toInt()

	override var scaleX: Float = window.devicePixelRatio.toFloat()
		private set

	override var scaleY: Float = window.devicePixelRatio.toFloat()
		private set

	override fun setSize(width: Float, height: Float) {
		if (this.width == width && this.height == height) return
		Log.debug("Setting size: $width, $height")
		this.width = width
		this.height = height
		canvas.style.width = "${width}px"
		canvas.style.height = "${height}px"
		sizeIsDirty = true
		requestRender()
	}

	override var continuousRendering: Boolean = false
	private var _renderRequested: Boolean = true

	override fun shouldRender(clearRenderRequest: Boolean): Boolean {
//		resizeHandler()
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
			canvas.width = framebufferWidth
			canvas.height = framebufferHeight
			Log.debug("Canvas size: ${canvas.width} ${canvas.height}")
		}
	}

	override fun renderEnd() {
	}

	override fun isCloseRequested(): Boolean = false

	// TODO: Implement. Pop-ups can be closed
	override fun requestClose(force: Boolean) {
	}

	private val _fullScreenChanged = Signal0()
	override val fullScreenChanged = _fullScreenChanged.asRo()

	override val fullScreenEnabled: Boolean
		get() = document.fullscreenEnabled

	override var fullScreen: Boolean
		get() = document.fullscreen
		set(value) {
			if (value && !fullScreen && document.fullscreenEnabled) {
				canvas.requestFullscreen().then { resizeHandler() }
			} else if (!value && fullScreen) {
				document.exitFullscreen().then { resizeHandler() }
			}
			requestRender()
		}

	override fun alert(message: String) {
		window.alert(message)
	}

	override fun dispose() {
		_scaleChanged.dispose()
		_sizeChanged.dispose()
		_isVisibleChanged.dispose()
		scaleQuery.removeListener(::scaleChangedHandler.as1)

		window.removeEventListener("resize", ::resizeHandler.as1)
		canvas.removeEventListener("webglcontextlost", ::webGlContextRestoredHandler.as1)
		window.removeEventListener("blur", ::blurHandler.as1)
		window.removeEventListener("focus", ::focusHandler.as1)
		document.removeEventListener("fullscreenchange", ::fullScreenChangedHandler.as1)
		unwatchVisibilityChanges()
	}
}
