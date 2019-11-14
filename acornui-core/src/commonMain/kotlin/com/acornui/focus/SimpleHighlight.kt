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

package com.acornui.focus

import com.acornui.Disposable
import com.acornui.component.*
import com.acornui.di.Injector
import com.acornui.di.Owned
import com.acornui.di.Scoped
import com.acornui.focus.FocusHighlighter.Companion.HIGHLIGHT_PRIORITY
import com.acornui.math.Bounds
import com.acornui.popup.PopUpInfo
import com.acornui.popup.PopUpManager
import com.acornui.skins.Theme
import com.acornui.time.callLater

interface HighlightView : UiComponent {
	var highlighted: UiComponentRo?
}

open class SimpleHighlight(
		owner: Owned,
		atlasPath: String,
		regionName: String
) : ContainerImpl(owner), HighlightView {

	private val highlight = addChild(atlas(atlasPath, regionName))

	/**
	 * Overridden so that the parent render context doesn't get set to the parent.
	 */
	override var parent: ContainerRo? = null

	/**
	 * The target being highlighted.
	 */
	override var highlighted: UiComponentRo? = null
		set(value) {
			if (field != value) {
				field?.invalidated?.remove(::highlightedInvalidatedHandler)
				field = value
				field?.invalidated?.add(::highlightedInvalidatedHandler)
				_renderContext.parentContext = value?.renderContext ?: defaultRenderContext
				invalidate(ValidationFlags.LAYOUT or ValidationFlags.RENDER_CONTEXT)
			}
		}

	private fun highlightedInvalidatedHandler(c: UiComponentRo, flags: Int) {
		if (flags.containsFlag(ValidationFlags.LAYOUT))
			invalidateLayout()
		else if (flags.containsFlag(ValidationFlags.RENDER_CONTEXT))
			invalidate(ValidationFlags.RENDER_CONTEXT)
	}

	init {
		interactivityMode = InteractivityMode.NONE
		includeInLayout = false
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val highlighted = highlighted ?: return
		val w = explicitWidth ?: highlighted.width
		val h = explicitHeight ?: highlighted.height
		val splits = highlight.region?.splits
		if (splits != null) {
			// left, top, right, bottom
			// If the highlight is a nine patch, offset the highlight by the padding. This allows for the ability to
			// curve around the highlighted target without cutting into it.
			highlight.setSize(w + splits[0] + splits[2], h + splits[1] + splits[3])
			highlight.moveTo(-splits[0], -splits[1])
		} else {
			highlight.setSize(w, h)
			highlight.moveTo(0f, 0f)
		}
		out.set(highlight.bounds)
	}

	override fun updateRenderContext() {
		super.updateRenderContext()
	}

	override fun draw() {
		super.draw()
	}

	override fun dispose() {
		highlighted = null
		super.dispose()
	}
}

class SimpleFocusHighlighter(
		override val injector: Injector,
		private val highlight: HighlightView
) : Scoped, FocusHighlighter, Disposable {

	private val popUpManager by PopUpManager

	constructor(owner: Owned, theme: Theme) : this(owner.injector, SimpleHighlight(owner, theme.atlasPath, "FocusRect").apply {
		colorTint = theme.focusHighlightColor
	})

	private val popUpInfo = PopUpInfo(
			highlight,
			isModal = false,
			priority = HIGHLIGHT_PRIORITY,
			dispose = false,
			focus = false,
			highlightFocused = false
	)

	override fun unhighlight(target: UiComponentRo) {
		highlight.highlighted = null
		callLater {
			popUpManager.removePopUp(popUpInfo)
		}
	}

	override fun highlight(target: UiComponentRo) {
		highlight.highlighted = target
		callLater {
			popUpManager.addPopUp(popUpInfo)
		}
	}

	override fun dispose() {
		popUpManager.removePopUp(popUpInfo)
		if (!highlight.isDisposed)
			highlight.dispose()
	}
}