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

package com.acornui.component

import com.acornui.component.*
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.focus.FocusContainer
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.Focusable
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.math.Bounds
import com.acornui.math.MinMaxRo

/**
 * @author nbilyk
 */
open class GlStageImpl(owner: Owned) : Stage, ElementContainerImpl<UiComponent>(owner), FocusContainer, Focusable {

	final override val style = bind(StageStyle())

	private val gl = inject(Gl20)
	private val glState = inject(GlState)
	private val focus = inject(FocusManager)

	/**
	 * The stage may not be focusEnabled false; it is the default focus state.
	 */
	override var focusEnabled: Boolean
		get(): Boolean = true
		set(value) {
			throw Exception("The stage must be focusable")
		}

	override var focusOrder: Float = 0f
	override var highlight: UiComponent? = null // The stage is never highlighted, do nothing.

	init {
		interactivityMode = InteractivityMode.ALWAYS
		styleTags.add(Stage)
		interactivity.init(this)
		focus.init(this)
		watch(style) {
			window.clearColor = it.backgroundColor
		}
	}

	protected open val windowResizedHandler: (Float, Float, Boolean) -> Unit = {
		newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		gl.viewport(0, 0, (newWidth * window.scaleX).toInt(), (newHeight * window.scaleY).toInt())
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun onActivated() {
		window.sizeChanged.add(windowResizedHandler)
		windowResizedHandler(window.width, window.height, false)
		super.onActivated()
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val w = window.width
		val h = window.height
		for (i in 0.._elements.lastIndex) {
			// Elements of the stage all are explicitly sized to the dimensions of the stage.
			_elements[i].setSize(w, h)
		}
		out.set(w, h)
	}

	override fun render(viewport: MinMaxRo) {
		glState.batch.resetRenderCount()
		super.render(viewport)
		glState.batch.flush(true)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.sizeChanged.remove(windowResizedHandler)
	}

}