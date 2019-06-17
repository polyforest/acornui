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

package com.acornui.jvm.graphic

import com.acornui.core.GlConfig
import com.acornui.core.WindowConfig
import com.acornui.core.browser.Location
import com.acornui.core.graphic.Window
import com.acornui.gl.core.Gl20
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.jvm.browser.JvmLocation
import com.acornui.logging.Log
import com.acornui.signal.*
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.properties.Delegates


/**
 * @author nbilyk
 */
class GlfwWindowImpl(
		windowConfig: WindowConfig,
		private val glConfig: GlConfig,
		private val gl: Gl20,
		debug: Boolean
) : Window {

	private val cancel = Cancel()
	private val _closeRequested = Signal1<Cancel>()
	override val closeRequested = _closeRequested.asRo()
	private val _isActiveChanged = Signal1<Boolean>()
	override val isActiveChanged = _isActiveChanged.asRo()
	private val _isVisibleChanged = Signal1<Boolean>()
	override val isVisibleChanged = _isVisibleChanged.asRo()
	private val _sizeChanged = Signal3<Float, Float, Boolean>()
	override val sizeChanged = _sizeChanged.asRo()

	private var _width: Float = 0f
	private var _height: Float = 0f

	private var _scaleX: Float = 1f
	private var _scaleY: Float = 1f

	private val _scaleChanged = Signal2<Float, Float>()
	override val scaleChanged = _scaleChanged.asRo()

	val windowId: Long

	private var _clearColor = Color.CLEAR.copy()

	override var clearColor: ColorRo
		get() = _clearColor
		set(value) {
			_clearColor.set(value)
			gl.clearColor(value)
			requestRender()
		}

	init {
		if (debug)
			GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE)

		GLFWErrorCallback.createPrint(System.err).set()

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!GLFW.glfwInit())
			throw IllegalStateException("Unable to initialize GLFW")

		// Configure our window
		GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default

		if (glConfig.antialias) GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4)

		//GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL11.GL_FALSE) // the window will stay hidden after creation
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11.GL_TRUE) // the window will be resizable

		// Create the window
		windowId = GLFW.glfwCreateWindow(windowConfig.initialWidth.toInt(), windowConfig.initialHeight.toInt(), windowConfig.title, MemoryUtil.NULL, MemoryUtil.NULL)
		if (windowId == MemoryUtil.NULL)
			throw Exception("Failed to create the GLFW window")

		// Redraw when the window has been minimized / restored / etc.

		GLFW.glfwSetWindowIconifyCallback(windowId) {
			_, iconified ->
			isVisible = !iconified
			requestRender()
		}

		GLFW.glfwSetFramebufferSizeCallback(windowId) {
			_, width, height ->
			updateSize(width.toFloat() / _scaleX, height.toFloat() / _scaleY, true)
		}

		GLFW.glfwSetWindowContentScaleCallback(windowId) {
			_, scaleX, scaleY ->
			updateScale(scaleX, scaleY)
		}

		GLFW.glfwSetWindowFocusCallback(windowId) {
			_, focused ->
			isActive = focused
		}

		GLFW.glfwSetWindowCloseCallback(windowId) {
			_closeRequested.dispatch(cancel.reset())
			if (cancel.canceled) {
				GLFW.glfwSetWindowShouldClose(windowId, false)
			}
		}

		// Get the thread stack and push a new frame
		val stack = stackPush()
		@Suppress("ConvertTryFinallyToUseCall") // Don't convert to use call, causes problem with JVM Build
		try {
			val pWidth = stack.mallocInt(1) // int*
			val pHeight = stack.mallocInt(1) // int*
			// Get the window size passed to glfwCreateWindow
			GLFW.glfwGetWindowSize(windowId, pWidth, pHeight)
		} finally {
			stack.close()
		}

		// Get the window high definition scale
		val monitor = GLFW.glfwGetPrimaryMonitor()
		val scaleXArr = FloatArray(1)
		val scaleYArr = FloatArray(1)
		GLFW.glfwGetMonitorContentScale(monitor, scaleXArr, scaleYArr)
		_scaleX = scaleXArr[0]
		_scaleY = scaleYArr[0]

		// Get the resolution of the primary monitor
		val vidMode = GLFW.glfwGetVideoMode(monitor)
		// Center our window
		if (vidMode != null)
			GLFW.glfwSetWindowPos(windowId, (vidMode.width() - windowConfig.initialWidth.toInt()) / 2, (vidMode.height() - windowConfig.initialHeight.toInt()) / 2)

		// Make the OpenGL context current
		GLFW.glfwMakeContextCurrent(windowId)

		// Enable v-sync
		if (glConfig.vSync)
			GLFW.glfwSwapInterval(1)

		// Make the window visible
		GLFW.glfwShowWindow(windowId)

		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities()

//		if (debug) {
//			GLUtil.setupDebugMessageCallback()
//		}

		setSize(windowConfig.initialWidth, windowConfig.initialHeight)

		clearColor = windowConfig.backgroundColor

		Log.info("Vendor: ${GL11.glGetString(GL11.GL_VENDOR)}")
		Log.info("Supported GLSL language version: ${GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION)}")
	}

	private fun updateScale(scaleX: Float, scaleY: Float) {
		_scaleX = scaleX
		_scaleY = scaleY
		_scaleChanged.dispatch(scaleX, scaleY)
	}

	override var isVisible: Boolean by Delegates.observable(true) {
		prop, old, new ->
		_isVisibleChanged.dispatch(new)
	}

	override var isActive: Boolean by Delegates.observable(true) {
		prop, old, new ->
		_isActiveChanged.dispatch(new)
		_sizeChanged.dispatch(_width, _height, false)
	}

	override val width: Float
		get() = _width

	override val height: Float
		get() = _height

	override val scaleX: Float
		get() = _scaleX

	override val scaleY: Float
		get() = _scaleY

	override fun setSize(width: Float, height: Float) = setSize(width, height, false)

	private fun setSize(width: Float, height: Float, isUserInteraction: Boolean) {
		if (_width == width && _height == height) return // no-op
		GLFW.glfwSetWindowSize(windowId, width.toInt(), height.toInt())
		updateSize(width, height, isUserInteraction)
	}

	private fun updateSize(width: Float, height: Float, userInteraction: Boolean) {
		if (_width == width && _height == height) return // no-op
		requestRender()
		_width = width
		_height = height
		_sizeChanged.dispatch(_width, _height, userInteraction)
	}

	override var continuousRendering: Boolean = false
	private var _renderRequested: Boolean = true

	override fun shouldRender(clearRenderRequest: Boolean): Boolean {
		val shouldRender = continuousRendering || _renderRequested
		if (clearRenderRequest && _renderRequested) _renderRequested = false
		return shouldRender
	}

	override fun requestRender() {
		if (_renderRequested) return
		_renderRequested = true
	}

	override fun renderBegin() {
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
	}

	override fun renderEnd() {
		GLFW.glfwSwapBuffers(windowId) // swap the color buffers
	}

	override fun isCloseRequested(): Boolean {
		return GLFW.glfwWindowShouldClose(windowId)
	}

	override fun requestClose() {
		GLFW.glfwSetWindowShouldClose(windowId, true)
	}

	private var lastWidth = windowConfig.initialWidth
	private var lastHeight = windowConfig.initialHeight

	private val _fullScreenChanged = Signal0()
	override val fullScreenChanged = _fullScreenChanged.asRo()

	override val fullScreenEnabled: Boolean = true

	private var _fullScreen = false
	override var fullScreen: Boolean
		get() = _fullScreen
		set(value) {
			if (_fullScreen != value) {
				Log.info("Fullscreen $value")
				val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()) ?: return
				_fullScreen = value
				val w = videoMode.width()
				val h = videoMode.height()
				val r = videoMode.refreshRate()
				if (value) {
					lastWidth = _width
					lastHeight = _height
					setSize(w.toFloat(), h.toFloat(), isUserInteraction = false)
					GLFW.glfwSetWindowMonitor(windowId, GLFW.glfwGetPrimaryMonitor(), 0, 0, w, h, r)
				} else {
					GLFW.glfwSetWindowMonitor(windowId, 0, ((w - lastWidth) * 0.5f).toInt(), ((h - lastHeight) * 0.5f).toInt(), lastWidth.toInt(), lastHeight.toInt(), 60)
				}
				if (glConfig.vSync)
					GLFW.glfwSwapInterval(1)
				requestRender()
				_fullScreenChanged.dispatch()
			}
		}

	override val location: Location = JvmLocation()

	override fun alert(message: String) {
		TinyFileDialogs.tinyfd_notifyPopup(null, message, "error")
	}

	override fun dispose() {
		_closeRequested.dispose()
		_isActiveChanged.dispose()
		_sizeChanged.dispose()
		_isVisibleChanged.dispose()
		_scaleChanged.dispose()
		Callbacks.glfwFreeCallbacks(windowId)
		GLFW.glfwTerminate()
	}
}
