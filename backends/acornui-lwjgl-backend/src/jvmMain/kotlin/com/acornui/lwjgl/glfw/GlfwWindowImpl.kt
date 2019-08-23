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

package com.acornui.lwjgl.glfw

import com.acornui.GlConfig
import com.acornui.WindowConfig
import com.acornui.browser.Location
import com.acornui.graphic.Window
import com.acornui.gl.core.Gl20
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.lwjgl.browser.JvmLocation
import com.acornui.logging.Log
import com.acornui.signal.*
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyfd.TinyFileDialogs
import kotlin.math.ceil
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
	private val _sizeChanged = Signal2<Float, Float>()
	override val sizeChanged = _sizeChanged.asRo()
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

	override var width: Float = 0f
		private set

	override var height: Float = 0f
		private set

	override var framebufferWidth: Int = 0
		private set

	override var framebufferHeight: Int = 0
		private set

	override var scaleX: Float = 1f
		private set

	override var scaleY: Float = 1f
		private set

	private var forceClose = false

	init {
		if (debug)
			glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)

		GLFWErrorCallback.createPrint(System.err).set()

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw IllegalStateException("Unable to initialize GLFW")

		// Configure our window
		glfwDefaultWindowHints() // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
		glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE)

		if (glConfig.antialias)
			glfwWindowHint(GLFW_SAMPLES, 4)

		//glfwWindowHint(GLFW_VISIBLE, GL11.GL_FALSE) // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GL11.GL_TRUE) // the window will be resizable

		val primaryMonitor = glfwGetPrimaryMonitor()

		// Get the content scaling for the primary monitor
		val windowScaleXArr = FloatArray(1)
		val windowScaleYArr = FloatArray(1)
		glfwGetMonitorContentScale(primaryMonitor, windowScaleXArr, windowScaleYArr)
		val wSX = windowScaleXArr[0]
		val wSY = windowScaleYArr[0]

		// Create the window
		windowId = glfwCreateWindow((windowConfig.initialWidth * wSX).toInt(), (windowConfig.initialHeight * wSY).toInt(), windowConfig.title, MemoryUtil.NULL, MemoryUtil.NULL)
		if (windowId == MemoryUtil.NULL)
			throw Exception("Failed to create the GLFW window")

		// Get the frame buffer size
		val framebufferW = IntArray(1)
		val framebufferH = IntArray(1)
		glfwGetFramebufferSize(windowId, framebufferW, framebufferH)
		framebufferWidth = framebufferW[0]
		framebufferHeight = framebufferH[0]

		// Make the OpenGL context current
		glfwMakeContextCurrent(windowId)

		// Enable v-sync
		if (glConfig.vSync)
			glfwSwapInterval(1)


		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities()

//		if (debug) {
//			GLUtil.setupDebugMessageCallback()
//		}

		// Center our window
		val vidMode = glfwGetVideoMode(primaryMonitor)
		if (vidMode != null) {
			val pWidth = IntArray(1)
			val pHeight = IntArray(1)
			glfwGetWindowSize(windowId, pWidth, pHeight)

			val pX = IntArray(1)
			val pY = IntArray(1)
			glfwGetMonitorPos(primaryMonitor, pX, pY)
			glfwSetWindowPos(windowId, pX[0] + (vidMode.width() - (pWidth[0])) / 2, pY[0] + (vidMode.height() - (pHeight[0])) / 2)
		}

		// Get the window dpi scale
		val scaleXArr = FloatArray(1)
		val scaleYArr = FloatArray(1)
		glfwGetWindowContentScale(windowId, scaleXArr, scaleYArr)
		scaleX = scaleXArr[0]
		scaleY = scaleYArr[0]

		// Get the window size
		val windowW = IntArray(1)
		val windowH = IntArray(1)
		glfwGetWindowSize(windowId, windowW, windowH)
		width = windowW[0] / scaleX
		height = windowH[0] / scaleY

		// Make the window visible
		glfwShowWindow(windowId)

		clearColor = windowConfig.backgroundColor

		Log.info("Vendor: ${GL11.glGetString(GL11.GL_VENDOR)}")
		Log.info("Supported GLSL language version: ${GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION)}")


		// Redraw when the window has been minimized / restored / etc.

		glfwSetWindowIconifyCallback(windowId) { _, iconified ->
			isVisible = !iconified
			requestRender()
		}

		glfwSetFramebufferSizeCallback(windowId) { _, width, height ->
			updateFramebuffer(width, height)
		}

		glfwSetWindowSizeCallback(windowId) { _, width, height ->
			updateSize(width / scaleX, height / scaleY)
		}

		glfwSetWindowContentScaleCallback(windowId) { _, scaleX, scaleY ->
			updateScale(scaleX, scaleY)
		}

		glfwSetWindowFocusCallback(windowId) { _, focused ->
			isActive = focused
		}

		glfwSetWindowCloseCallback(windowId) {
			if (!forceClose) {
				_closeRequested.dispatch(cancel.reset())
				if (cancel.canceled) {
					glfwSetWindowShouldClose(windowId, false)
				}
			}
		}
	}

	private fun updateScale(scaleX: Float, scaleY: Float) {
		this.scaleX = scaleX
		this.scaleY = scaleY
		_scaleChanged.dispatch(scaleX, scaleY)
	}

	override var isVisible: Boolean by Delegates.observable(true) { prop, old, new ->
		_isVisibleChanged.dispatch(new)
	}

	override var isActive: Boolean by Delegates.observable(true) { prop, old, new ->
		_isActiveChanged.dispatch(new)
	}

	override fun setSize(width: Float, height: Float) {
		if (this.width == width && this.height == height) return // no-op
		glfwSetWindowSize(windowId, ceil(width * scaleX).toInt(), ceil(height * scaleY).toInt())
		// TODO: Test if this kicks off callbacks for new sizes
	}

	private fun updateSize(width: Float, height: Float) {
		this.width = width
		this.height = height
		requestRender()
		_sizeChanged.dispatch(width, height)
	}

	private fun updateFramebuffer(width: Int, height: Int) {
		this.framebufferWidth = width
		this.framebufferHeight = height
		requestRender()
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
		glfwSwapBuffers(windowId) // swap the color buffers
	}

	override fun isCloseRequested(): Boolean {
		return glfwWindowShouldClose(windowId)
	}

	override fun requestClose(force: Boolean) {
		forceClose = force
		glfwSetWindowShouldClose(windowId, true)
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
				Log.info("Fullscreen: $value")
				val primaryMonitor = glfwGetPrimaryMonitor()
				val videoMode = glfwGetVideoMode(primaryMonitor) ?: return
				_fullScreen = value
				val w = videoMode.width()
				val h = videoMode.height()
				val r = videoMode.refreshRate()
				// Get the content scaling for the primary monitor
				val windowScaleXArr = FloatArray(1)
				val windowScaleYArr = FloatArray(1)
				glfwGetMonitorContentScale(primaryMonitor, windowScaleXArr, windowScaleYArr)
				val wSX = windowScaleXArr[0]
				val wSY = windowScaleYArr[0]
				if (value) {
					lastWidth = width
					lastHeight = height
					setSize(w.toFloat() / wSX, wSY)
					glfwSetWindowMonitor(windowId, primaryMonitor, 0, 0, w, h, r)
				} else {
					setSize(lastWidth, lastHeight)
					glfwSetWindowMonitor(windowId, 0, ((w - lastWidth * wSX) * 0.5f).toInt(), ((h - lastHeight * wSY) * 0.5f).toInt(), (lastWidth * wSX).toInt(), (lastHeight * wSY).toInt(), 60)
				}
				if (glConfig.vSync)
					glfwSwapInterval(1)
				requestRender()
				_fullScreenChanged.dispatch()
				_sizeChanged.dispatch(width, height)
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
		_scaleChanged.dispose()
		_isVisibleChanged.dispose()
		Callbacks.glfwFreeCallbacks(windowId)
		glfwTerminate()
	}
}
