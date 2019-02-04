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

import com.acornui.core.di.Owned
import com.acornui.core.focus.Focusable
import com.acornui.math.Bounds
import com.acornui.math.MinMaxRo
import com.acornui.math.Rectangle
import kotlin.math.roundToInt

/**
 * @author nbilyk
 */
open class GlStageImpl(owner: Owned) : Stage, ElementContainerImpl<UiComponent>(owner), Focusable {

	private val stageViewport = Rectangle()

	init {
		focusEnabled = true
		interactivityMode = InteractivityMode.ALWAYS
		interactivity.init(this)
		focusManager.init(this)
		_viewport = stageViewport
	}

	/**
	 * It is not normal to set gl state within an event handler, but because this is the Stage, we can safely set
	 * certain properties and expect them to act as the first values in the stack.
	 *
	 * Also note that event handlers can never be called within a render.
	 */
	protected open val windowResizedHandler: (Float, Float, Boolean) -> Unit = {
		newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		val w = (newWidth * window.scaleX).roundToInt()
		val h = (newHeight * window.scaleY).roundToInt()
		glState.setViewport(0, 0, w, h)
		glState.setFramebuffer(null, w, h, window.scaleX, window.scaleY)
		invalidate(ValidationFlags.LAYOUT or ValidationFlags.VIEWPORT)
	}

	override fun updateViewport() {
		stageViewport.set(0f, 0f, window.width, window.height)
	}

	override fun onActivated() {
		window.sizeChanged.add(windowResizedHandler)
		windowResizedHandler(window.width, window.height, false)
		super.onActivated()
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

	override fun render(clip: MinMaxRo) {
		glState.batch.resetRenderCount()
		super.render(clip)
		glState.batch.flush()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.sizeChanged.remove(windowResizedHandler)
	}

}