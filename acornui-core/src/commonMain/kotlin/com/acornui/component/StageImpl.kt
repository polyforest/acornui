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

package com.acornui.component

import com.acornui.Disposable
import com.acornui.RedrawRegions
import com.acornui.collection.forEach2
import com.acornui.component.style.StyleableRo
import com.acornui.di.Injector
import com.acornui.di.OwnedImpl
import com.acornui.di.createScope
import com.acornui.di.inject
import com.acornui.focus.Focusable
import com.acornui.function.as1
import com.acornui.function.as2
import com.acornui.gl.core.Gl20
import com.acornui.graphic.Color
import com.acornui.input.SoftKeyboardManager
import com.acornui.logging.Log
import com.acornui.math.Bounds
import com.acornui.math.IntRectangle
import com.acornui.popup.PopUpManager
import com.acornui.reflect.observable
import com.acornui.time.timer

/**
 * @author nbilyk
 */
open class StageImpl(injector: Injector) : Stage, ElementContainerImpl<UiComponent>(OwnedImpl(injector)), Focusable {

	private val defaultBackgroundColor = gl.getParameterfv(Gl20.COLOR_CLEAR_VALUE, Color())

	final override val style = bind(StageStyle())
	override var showWaitingForSkinMessage = true

	private val popUpManager = inject(PopUpManager)
	private val popUpManagerView: UiComponent

	private val softKeyboardManager by SoftKeyboardManager
	private var softKeyboardView: UiComponent? = null

	private var skinCheckTimer: Disposable? = null

	init {
		skinCheckTimer = timer(5f, 10, callback = ::skinCheck)
		focusEnabled = true
		interactivityMode = InteractivityMode.ALWAYS
		interactivity.init(this)
		focusManager.init(this)
		popUpManagerView = addChild(popUpManager.init(createScope(Stage to this)))
		popUpManagerView.layoutInvalidatingFlags = 0

		softKeyboardManager.changed.add(::invalidateLayout.as1)
		gl.stencilFunc(Gl20.EQUAL, 1, -1)
		gl.stencilOp(Gl20.KEEP, Gl20.KEEP, Gl20.KEEP)
		gl.enable(Gl20.STENCIL_TEST)
		
		watch(style) {
			useRedrawRegions = it.useRedrawRegions
			showRedrawRegions = it.showRedrawRegions && it.useRedrawRegions
		}
	}
	
	private var showRedrawRegions = false
	
	private var useRedrawRegions: Boolean by observable(true) {
		_renderContext.redrawOverride = if (it) null else RedrawRegions.ALWAYS 
		invalidate(ValidationFlags.RENDER_CONTEXT)
	}

	private fun skinCheck() {
		if (showWaitingForSkinMessage && styleRules.isEmpty())
			Log.debug("Awaiting skin...")
		else
			skinCheckTimer?.dispose()

	}

	override val styleParent: StyleableRo? = null

	/**
	 * Invoked when the window's size or scaling has changed.
	 * This will update the viewport and framebuffer information.
	 */
	protected open fun windowChangedHandler() {
		invalidate(ValidationFlags.LAYOUT or ValidationFlags.RENDER_CONTEXT)
	}

	override fun onActivated() {
		window.sizeChanged.add(::windowChangedHandler.as2)
		window.scaleChanged.add(::windowChangedHandler.as2)
		window.refresh.add(::windowChangedHandler)
		windowChangedHandler()
		super.onActivated()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.sizeChanged.remove(::windowChangedHandler.as2)
		window.scaleChanged.remove(::windowChangedHandler.as2)
		window.refresh.remove(::windowChangedHandler)
	}

	override fun updateRenderContext() {
		val w = window.framebufferWidth
		val h = window.framebufferHeight
		glState.setViewport(0, 0, w, h)
		glState.setFramebuffer(null, w, h, window.scaleX, window.scaleY)
		renderContext.redraw.invalidate(0, 0, w, h)
		super.updateRenderContext()
	}

	override fun updateDrawRegionScreen(out: IntRectangle) {
		out.set(0, 0, window.framebufferWidth, window.framebufferHeight)
	}

	//-------------------------------------------------------------
	// External elements
	// Pop up manager view
	// Soft keyboard view
	//-------------------------------------------------------------

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		addChild(newIndex, element)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val w = window.width
		val h = window.height
		val softKeyboardH: Float = if (softKeyboardManager.isShowing) {
			if (softKeyboardView == null)
				softKeyboardView = addChild(softKeyboardManager.createView(this))
			val keyboardView = softKeyboardView!!
			keyboardView.setSize(w, null)
			keyboardView.setPosition(0f, h - keyboardView.height)
			keyboardView.height
		} else 0f

		elementsToLayout.forEach2 {
			// Elements of the stage all are explicitly sized to the dimensions of the stage.
			it.setSize(w, h - softKeyboardH)
		}
		popUpManagerView.setSize(w, h - softKeyboardH)
		out.set(w, h)
	}

	override fun render() {
		// The stage shouldn't check if it needs redrawing.
		draw()
	}

	override fun draw() {
		val redraw = renderContext.redraw
		if (redraw.enabled) {
			gl.clearStencil(0)
			gl.clear(Gl20.STENCIL_BUFFER_BIT)
			gl.clearStencil(1)
			gl.clearColor(style.backgroundColor ?: defaultBackgroundColor)
			gl.enable(Gl20.SCISSOR_TEST)
			redraw.regions.forEach2 {
				gl.scissor(it.x, it.y, it.width, it.height)
				gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
			}
			gl.disable(Gl20.SCISSOR_TEST)
		} else {
			gl.clearStencil(1)
			gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
		}
		glState.batch.resetRenderCount()
		glState.uniforms.setCamera(renderContext, useModel = false)
		super.draw()
		glState.batch.flush()

		// Draw redraw regions

		if (showRedrawRegions) {
			gl.clearColor(Color.RED)
			gl.enable(Gl20.SCISSOR_TEST)
			redraw.regions.forEach2 {
				gl.scissor(it.x, it.y, it.width, 1)
				gl.clear(Gl20.COLOR_BUFFER_BIT)

				gl.scissor(it.x, it.y, 1, it.height)
				gl.clear(Gl20.COLOR_BUFFER_BIT)

				gl.scissor(it.right - 1, it.y, 1, it.height)
				gl.clear(Gl20.COLOR_BUFFER_BIT)

				gl.scissor(it.x, it.bottom - 1, it.width, 1)
				gl.clear(Gl20.COLOR_BUFFER_BIT)
			}
			gl.disable(Gl20.SCISSOR_TEST)
			gl.clearColor(style.backgroundColor ?: defaultBackgroundColor)
		}
		redraw.clear()
	}

	override fun dispose() {
		super.dispose()
		softKeyboardManager.changed.remove(::invalidateLayout.as1)
	}
}
