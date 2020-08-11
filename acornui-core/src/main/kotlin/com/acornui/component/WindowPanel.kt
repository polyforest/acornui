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

@file:Suppress("CssNoGenericFontName")

package com.acornui.component

import com.acornui.component.style.CommonStyleTags
import com.acornui.component.style.cssClass
import com.acornui.di.Context
import com.acornui.dom.addStyleToHead
import com.acornui.dom.div
import com.acornui.google.Icons
import com.acornui.google.icon
import com.acornui.skins.CssProps
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class WindowPanel(owner: Context) : Div(owner) {

	val titleBar = addChild(div {
		addClass(WindowPanelStyle.titleBar)
		addClass(CommonStyleTags.controlBar)
	})

	val labelComponent = titleBar.addElement(div {
		addClass(WindowPanelStyle.label)
	})

	val closeButton = titleBar.addElement(div {
		addClass(WindowPanelStyle.closeButton)
		+icon(Icons.CLOSE)
	})

	val contents = addChild(div {
		addClass(WindowPanelStyle.contents)
	})

	init {
		addClass(WindowPanelStyle.windowPanel)
		addClass(PanelStyle.colors)

	}

	override var label: String
		get() = labelComponent.label
		set(value) {
			labelComponent.label = value
		}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		contents.removeElement(element)
	}
}

object WindowPanelStyle {

	val windowPanel by cssClass()
	val titleBar by cssClass()
	val label by cssClass()
	val contents by cssClass()
	val closeButton by cssClass()

	init {
		addStyleToHead(
			"""
$windowPanel {
	
}

$titleBar {
	display: flex;
	flex-direction: row;
	align-items: center;
	border-bottom-style: solid;
	border-bottom-width: inherit;
	border-bottom-color: inherit;
	border-radius: ${CssProps.borderRadius.v} ${CssProps.borderRadius.v} 0 0;
	padding: ${CssProps.padding.v};
}

$closeButton {
	display: flex;
	flex-grow: 0;
	margin-left: 10px;
	user-select: none;
	-moz-user-select: none;
	-webkit-user-select: none;
	cursor: pointer;
}

$closeButton i {
	font-size: inherit;
}

$closeButton:hover {
	color: #900;
}

$closeButton:active {
	color: #B44;
}

$contents {
	padding: ${CssProps.padding.v};
}

		"""
		)
	}
}

inline fun Context.windowPanel(init: ComponentInit<WindowPanel> = {}): WindowPanel {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return WindowPanel(this).apply(init)
}