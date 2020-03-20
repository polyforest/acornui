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

@file:Suppress("LeakingThis")

package com.acornui.component

import com.acornui.collection.forEach2
import com.acornui.component.style.StylableRo
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.focus.Focusable
import com.acornui.function.as1
import com.acornui.function.as2
import com.acornui.gl.core.DefaultShaderProgram
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.ShaderBatch
import com.acornui.graphic.Color
import com.acornui.graphic.centerCamera
import com.acornui.graphic.orthographicCamera
import com.acornui.input.SoftKeyboardManager
import com.acornui.logging.Log
import com.acornui.math.*
import com.acornui.popup.PopUpManager
import com.acornui.time.Timer
import com.acornui.time.timer
import kotlin.time.seconds

/**
 * @author nbilyk
 */
open class StageImpl(owner: Context) : Stage, ElementContainerImpl<UiComponent>(owner), Focusable {

	init {
		childDependencies += listOf(Stage to this, TooltipManager to TooltipManagerImpl(inject(PopUpManager), this))
	}
	
	private val defaultBackgroundColor = gl.getParameterfv(Gl20.COLOR_CLEAR_VALUE, Color())

	final override val style = bind(StageStyle())
	override var showWaitingForSkinMessage = true

	private val popUpManager = inject(PopUpManager)
	private val popUpManagerView: UiComponent

	private val softKeyboardManager by SoftKeyboardManager
	private var softKeyboardView: UiComponent? = null

	private val cam = orthographicCamera(false)

	private val _windowRegion = MinMax()

	override val canvasClipRegion: MinMaxRo by validationProp(ValidationFlags.VIEW_PROJECTION) {
		canvasClipRegionOverride ?: _windowRegion
	}

	override val viewport: RectangleRo by validationProp(ValidationFlags.VIEW_PROJECTION) {
		viewportOverride ?: _windowRegion
	}

	override val viewProjectionTransform: Matrix4Ro by validationProp(ValidationFlags.VIEW_PROJECTION) {
		cameraOverride?.viewProjectionTransform ?: parent?.viewProjectionTransform ?: Matrix4.IDENTITY
	}

	init {
		cameraOverride = cam
		own(timer(5.seconds, 10, callback = ::skinCheck))
		focusEnabled = true
		interactivityMode = InteractivityMode.ALWAYS
		interactivity.init(this)
		focusManager.init(this)
		popUpManagerView = addChild(popUpManager.init(this))
		popUpManagerView.layoutInvalidatingFlags = 0

		softKeyboardManager.changed.add(::invalidateLayout.as1)
		gl.colorMask(true, true, true, true)
		gl.stencilFunc(Gl20.EQUAL, 0, -1)
		gl.stencilOp(Gl20.KEEP, Gl20.KEEP, Gl20.KEEP)
		gl.enable(Gl20.STENCIL_TEST)

		gl.enable(Gl20.BLEND)
		try {
			gl.useProgram(DefaultShaderProgram(gl).program)
		} catch (e: Throwable) {}

		watch(style) {
			gl.clearColor(style.backgroundColor ?: defaultBackgroundColor)
		}
	}
	
	private fun skinCheck(timer: Timer) {
		if (showWaitingForSkinMessage && styleRules.isEmpty())
			Log.debug("Awaiting skin...")
		else
			timer.dispose()

	}

	override val styleParent: StylableRo? = null

	/**
	 * Invoked when the window's size or scaling has changed.
	 * This will update the viewport and framebuffer information.
	 */
	protected open fun windowChangedHandler() {
		invalidate(ValidationFlags.LAYOUT or ValidationFlags.VIEW_PROJECTION)
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

	override fun updateViewProjection() {
		super.updateViewProjection()
		val w = window.framebufferWidth
		val h = window.framebufferHeight
		if (w <= 0f || h <= 0f) return
		gl.viewport(0, 0, w, h)
		gl.bindFramebuffer(null)
		_windowRegion.set(0f, 0f, window.width, window.height)
		window.centerCamera(cam)
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
		draw()
	}

	override fun draw() {
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
		ShaderBatch.totalDrawCalls = 0
		gl.uniforms.setCamera(this)
		super.draw()
		gl.batch.flush()
	}

	override fun dispose() {
		super.dispose()
		softKeyboardManager.changed.remove(::invalidateLayout.as1)
	}
}
