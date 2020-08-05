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

package com.acornui.component

 import com.acornui.component.style.cssClass
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.skins.Theme
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class Panel(owner: Context) : DivComponent(owner) {

	init {
		addClass(styleTag)
		addClass(panelColorsStyle)
	}

	companion object {

		val styleTag by cssClass()
		val panelColorsStyle by cssClass()

		init {
			addCssToHead("""
$styleTag {
	padding: ${cssVar(Theme::padding)};
	overflow: hidden;
	min-width: min-content;
	min-height: min-content;
}

$panelColorsStyle {
	color: ${cssVar(Theme::panelTextColor)};
	background: ${cssVar(Theme::panelBackground)};
	box-shadow: ${cssVar(Theme::panelShadow)};
	border: ${cssVar(Theme::borderThickness)} solid ${cssVar(Theme::border)};
	border-radius: ${cssVar(Theme::borderRadius)};
	--scrollbarButtonColor: ${cssVar(Theme::panelScrollbarButtonColor)};
	--scrollbarTrackColor: ${cssVar(Theme::panelScrollbarTrackColor)};
	--scrollbarCornerColor: ${cssVar(Theme::panelScrollbarCornerColor)};
	--scrollbarBorderRadius: ${cssVar(Theme::borderRadius)};
	--loadingSpinnerColor: ${cssVar(Theme::panelLoadingSpinnerColor)};
}


			""")
		}
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
