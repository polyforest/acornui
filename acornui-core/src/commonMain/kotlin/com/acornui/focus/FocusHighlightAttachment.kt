/*
 * Copyright 2020 Poly Forest, LLC
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
import com.acornui.ManagedDisposable
import com.acornui.component.HighlightView
import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.function.as1
import com.acornui.popup.PopUpInfo
import com.acornui.popup.PopUpManager
import com.acornui.properties.afterChange

/**
 * A component attachment that handles focus highlight.
 */
private class FocusHighlightAttachment(private val host: UiComponentRo) : Disposable {

	var highlightDelegate: UiComponentRo? by afterChange(null, ::refreshFocusHighlight.as1)

	var showHighlight by afterChange(false, ::refreshFocusHighlight.as1)
	
	private var highlightView: HighlightView? = null

	private val popUpManager = host.inject(PopUpManager)
	private var popUpInfo: PopUpInfo<HighlightView>? = null
	private val watched: ManagedDisposable
	
	val style = host.bind(FocusableStyle()).apply {
		watched = host.watch(this) {
			clearHighlight()
			refreshFocusHighlight()
		}
	}

	private fun clearHighlight() {
		highlightTarget = null
		highlightView?.dispose()
		highlightView = null
		popUpInfo = null
	}

	private var highlightTarget: UiComponentRo? = null
		set(value) {
			if (field == value) return
			field = value
			if (value != null) {
				if (popUpInfo == null) {
					highlightView = style.highlight(host)
					if (highlightView != null) {
						popUpInfo = PopUpInfo(
								highlightView!!,
								isModal = false,
								priority = style.highlightPriority,
								dispose = false,
								focus = false,
								highlightFocused = false
						)
					}
				}
				if (popUpInfo != null) {
					popUpManager.addPopUp(popUpInfo!!)
				}
			} else {
				if (popUpInfo != null) {
					popUpManager.removePopUp(popUpInfo!!)
				}
			}
			highlightView?.highlighted = value
		}

	private fun refreshFocusHighlight() {
		highlightTarget = if (showHighlight) {
			highlightDelegate ?: host
		} else {
			null
		}
	}

	override fun dispose() {
		showHighlight = false
		host.unbind(style)
		watched.dispose()
	}

	companion object
}

private val UiComponentRo.focusHighlightAttachment: FocusHighlightAttachment
	get() = createOrReuseAttachment(FocusHighlightAttachment) {
		FocusHighlightAttachment(this)
	}

/**
 * If set, the provided delegate will be highlighted instead of this component when it's focused.
 * The highlighter will still be obtained from this component's [focusableStyle].
 */
var UiComponentRo.focusHighlightDelegate: UiComponentRo?
	get() = focusHighlightAttachment.highlightDelegate
	set(value) {
		focusHighlightAttachment.highlightDelegate = value
	}

/**
 * If true, this component will render its focus highlight as provided by the [focusableStyle].
 */
var UiComponentRo.showFocusHighlight: Boolean
	get() {
		// If the highlight has never been set to true, don't create the highlight attachment.
		return getAttachment<FocusHighlightAttachment>(FocusHighlightAttachment)?.showHighlight == true
	}
	set(value) {
		if (value != showFocusHighlight)
			focusHighlightAttachment.showHighlight = value
	}

/**
 * A style object for decorating the component with a focus highlight.
 */
val UiComponentRo.focusableStyle: FocusableStyle
	get() = focusHighlightAttachment.style
