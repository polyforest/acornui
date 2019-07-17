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

import com.acornui.collection.forEach2
import com.acornui.component.style.StyleableRo
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.OwnedImpl
import com.acornui.core.di.inject
import com.acornui.core.focus.Focusable
import com.acornui.core.input.SoftKeyboardManager
import com.acornui.core.popup.PopUpManager
import com.acornui.core.time.timer
import com.acornui.function.as2
import com.acornui.logging.Log
import com.acornui.math.Bounds

/**
 * @author nbilyk
 */
open class StageImpl(injector: Injector) : Stage, ElementContainerImpl<UiComponent>(OwnedImpl(injector)), Focusable {

	final override val style = bind(StageStyle())
	override var showWaitingForSkinMessage = true

	private val popUpManagerView = inject(PopUpManager).view
	private val softKeyboardManagerView = inject(SoftKeyboardManager).view

	private var skinCheckTimer: Disposable? = null

	init {
		skinCheckTimer = timer(5f, 10, callback = ::skinCheck)
		focusEnabled = true
		interactivityMode = InteractivityMode.ALWAYS
		interactivity.init(this)
		focusManager.init(this)

		addChild(popUpManagerView)
		addChild(softKeyboardManagerView)

		watch(style) {
			window.clearColor = it.bgColor
		}
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
		windowChangedHandler()
		super.onActivated()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.sizeChanged.remove(::windowChangedHandler.as2)
		window.scaleChanged.remove(::windowChangedHandler.as2)
	}

	override fun updateRenderContext() {
		val w = window.framebufferWidth
		val h = window.framebufferHeight
		glState.setViewport(0, 0, w, h)
		glState.setFramebuffer(null, w, h, window.scaleX, window.scaleY)
		defaultRenderContext.validate()
		super.updateRenderContext()
	}

	override fun invalidate(flags: Int): Int {
		val flagsInvalidated = super.invalidate(flags)
		if (flagsInvalidated != 0)
			window.requestRender()
		return flagsInvalidated
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
		val softKeyboardH: Float = if (softKeyboardManagerView.shouldLayout) {
			softKeyboardManagerView.setSize(w, null)
			softKeyboardManagerView.height
		} else 0f

		elementsToLayout.forEach2 {
			// Elements of the stage all are explicitly sized to the dimensions of the stage.
			it.setSize(w, h - softKeyboardH)
		}
		popUpManagerView.setSize(w, h - softKeyboardH)
		softKeyboardManagerView.setPosition(0f, h - softKeyboardH)
		out.set(w, h)
	}

	override fun render() {
		glState.batch.resetRenderCount()
		super.render()
		glState.batch.flush()
	}



}
