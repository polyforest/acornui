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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.js.dom

import com.acornui.component.*
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.Focusable
import com.acornui.js.dom.component.DomContainer
import com.acornui.math.Bounds
import org.w3c.dom.HTMLElement

/**
 * @author nbilyk
 */
@Suppress("LeakingThis")
open class DomStageImpl(owner: Owned, root: HTMLElement) : Stage, ElementContainerImpl<UiComponent>(owner, StageContainer(root)), Focusable {

	override final val style = bind(StageStyle())

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
	override var highlight: UiComponent? = null // Do nothing to highlight when the stage has focus.

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
		super.updateLayout(w, h, out)
		for (i in 0.._elements.lastIndex) {
			// Children of the stage all are explicitly sized to the dimensions of the stage.
			_elements[i].setSize(w, h)
		}
		out.set(w, h)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.sizeChanged.remove(windowResizedHandler)
	}

}

private class StageContainer(root: HTMLElement) : DomContainer(root) {
	override fun setSize(width: Float?, height: Float?) {
	}
}