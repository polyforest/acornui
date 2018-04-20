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

package com.acornui.jvm.graphics

import com.acornui.core.GlConfig
import com.acornui.core.WindowConfig
import com.acornui.core.browser.Location
import com.acornui.core.graphics.Window
import com.acornui.gl.core.Gl20
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.jvm.browser.LwjglLocation
import com.acornui.logging.Log
import com.acornui.signal.Signal1
import com.acornui.signal.Signal3
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor
import org.lwjgl.glfw.GLFW.glfwGetVideoMode
import org.lwjgl.glfw.GLFW.glfwSetWindowPos
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil


/**
 * @author nbilyk
 */
class GlfwWindowImpl(
		windowConfig: WindowConfig,
		private val glConfig: GlConfig,
		private val gl: Gl20,
		debug: Boolean
) : Window {

	override val isActiveChanged: Signal1<Boolean> = Signal1()
	override val isVisibleChanged: Signal1<Boolean> = Signal1()
	override val sizeChanged: Signal3<Float, Float, Boolean> = Signal3()

	private var _width: Float = 0f
	private var _height: Float = 0f

	val windowId: Long

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
			isVisible(!iconified)
			requestRender()
		}

		GLFW.glfwSetFramebufferSizeCallback(windowId) {
			_, width, height ->
			updateSize(width.toFloat(), height.toFloat(), true)
		}

		GLFW.glfwSetWindowFocusCallback(windowId) {
			_, focused ->
			isActive(focused)
		}

		// Get the thread stack and push a new frame
		val stack = stackPush()
		try {
			val pWidth = stack.mallocInt(1) // int*
			val pHeight = stack.mallocInt(1) // int*

			// Get the window size passed to glfwCreateWindow
			GLFW.glfwGetWindowSize(windowId, pWidth, pHeight)

			// Get the resolution of the primary monitor
			val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())

			// Center the window
			glfwSetWindowPos(
					windowId,
					(vidMode!!.width() - pWidth.get(0)) / 2,
					(vidMode.height() - pHeight.get(0)) / 2
			)
		} finally {
			stack.close()
		}

		// Get the resolution of the primary monitor
		val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
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

		setSize(windowConfig.initialWidth, windowConfig.initialHeight, false)

		Log.info("Vendor: ${GL11.glGetString(GL11.GL_VENDOR)}")
		Log.info("Supported GLSL language version: ${GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION)}")
	}

	private var _clearColor = Color.CLEAR.copy()

	override var clearColor: ColorRo
		get() = _clearColor
		set(value) {
			_clearColor.set(value)
			gl.clearColor(value)
			requestRender()
		}

	private var _isVisible: Boolean = true

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
		sizeChanged.dispatch(_width, _height, false)
	}

	override val width: Float
		get() {
			return _width
		}

	override val height: Float
		get() {
			return _height
		}

	override fun setSize(width: Float, height: Float, isUserInteraction: Boolean) {
		if (_width == width && _height == height) return // no-op
		GLFW.glfwSetWindowSize(windowId, width.toInt(), height.toInt())
		updateSize(width, height, isUserInteraction)
	}

	private fun updateSize(width: Float, height: Float, userInteraction: Boolean) {
		if (_width == width && _height == height) return // no-op
		requestRender()
		_width = width
		_height = height
		sizeChanged.dispatch(_width, _height, userInteraction)
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
					setSize(w.toFloat(), h.toFloat(), true)
					GLFW.glfwSetWindowMonitor(windowId, GLFW.glfwGetPrimaryMonitor(), 0, 0, w, h, r)
				} else {
					GLFW.glfwSetWindowMonitor(windowId, 0, ((w - lastWidth) * 0.5f).toInt(), ((h - lastHeight) * 0.5f).toInt(), lastWidth.toInt(), lastHeight.toInt(), 60)
				}
				if (glConfig.vSync)
					GLFW.glfwSwapInterval(1)
			}
		}

	override val location: Location = LwjglLocation()

	override fun dispose() {
		sizeChanged.dispose()
		Callbacks.glfwFreeCallbacks(windowId)
		GLFW.glfwTerminate()
	}
}