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

@file:Suppress("CssOverwrittenProperties")

package com.acornui.component

import com.acornui.component.style.cssClass
import com.acornui.di.Context
import com.acornui.dom.addStyleToHead
import com.acornui.skins.CssProps
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class Panel(owner: Context) : Div(owner) {

	init {
		addClass(PanelStyle.panel)
		addClass(PanelStyle.colors)
	}

}

object PanelStyle {

	val panel by cssClass()
	val colors by cssClass()

	init {
		addStyleToHead(
			"""
$panel {
	padding: ${CssProps.padding.v};
	overflow: hidden;
	min-width: min-content;
	min-height: min-content;
}

$colors {
	background: #ddd;
    color: #222;
	box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.4);
	border: ${CssProps.borderThickness.v} solid ${CssProps.borderColor.v};
	border-radius: ${CssProps.borderRadius.v};
	
	${CssProps.scrollbarTrackColor}: transparent;
    ${CssProps.scrollbarButtonColor}: #666;
    ${CssProps.scrollbarCornerColor}: transparent;
    ${CssProps.loadingSpinnerColor}: #222;
}


			"""
		)
	}
}

/**
 * Creates a Panel.
 */
inline fun Context.panel(init: ComponentInit<Panel> = {}): Panel {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Panel(this).apply {
		init()
	}
}
