/*
 * Copyright 2018 Poly Forest
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

import com.acornui.core.di.Injector
import com.acornui.core.di.OwnedImpl
import com.acornui.core.focus.Focusable
import com.acornui.math.Bounds
import com.acornui.skins.theme
import kotlin.math.ceil

/**
 * @author nbilyk
 */
open class StageImpl(injector: Injector) : Stage, ElementContainerImpl<UiComponent>(OwnedImpl(injector)), Focusable {

	init {
		focusEnabled = true
		interactivityMode = InteractivityMode.ALWAYS
		interactivity.init(this)
		focusManager.init(this)
	}

	/**
	 * It is not normal to set gl state within an event handler, but because this is the Stage, we can safely set
	 * certain properties and expect them to act as the first values in the stack.
	 *
	 * Also note that event handlers can never be called within a render.
	 */
	protected open val windowResizedHandler: (Float, Float, Boolean) -> Unit = {
		newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		val w = ceil(newWidth * window.scaleX).toInt()
		val h = ceil(newHeight * window.scaleY).toInt()
		val viewport = glState.viewport
		if (w != viewport.width || h != viewport.height) {
			glState.setViewport(0, 0, w, h)
			glState.setFramebuffer(null, w, h, window.scaleX, window.scaleY)
			invalidate(ValidationFlags.LAYOUT or ValidationFlags.RENDER_CONTEXT)
		}
	}

	override fun onActivated() {
		window.sizeChanged.add(windowResizedHandler)
		windowResizedHandler(window.width, window.height, false)
		super.onActivated()
	}

	override fun invalidate(flags: Int): Int {
		val flagsInvalidated = super.invalidate(flags)
		if (flagsInvalidated != 0)
			window.requestRender()
		return flagsInvalidated
	}

	override fun updateStyles() {
		super.updateStyles()
		window.clearColor = theme().bgColor
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val w = window.width
		val h = window.height
		_elements.iterate {
			// Elements of the stage all are explicitly sized to the dimensions of the stage.
			if (it.shouldLayout)
				it.setSize(w, h)
			true
		}
		out.set(w, h)
	}

	override fun render() {
		glState.batch.resetRenderCount()
		super.render()
		glState.batch.flush()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.sizeChanged.remove(windowResizedHandler)
	}

}