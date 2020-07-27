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

import com.acornui.component.layout.FlowGroup
import com.acornui.component.style.StyleTag
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.skins.Theme
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object Panel {

	val styleTag = StyleTag("Panel")

	init {

		addCssToHead("""
$styleTag {
	background: ${cssVar(Theme::panelBackground)};
	box-shadow: ${cssVar(Theme::panelShadow)};
	border-radius: ${cssVar(Theme::borderRadius)};
	padding: ${cssVar(Theme::padding)};
}			
		""")
	}
}

/**
 * Creates a FlowGroup with Panel styling.
 */
inline fun Context.panel(init: ComponentInit<FlowGroup> = {}): FlowGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FlowGroup(this).apply {
		addClass(Panel.styleTag)
		init()
	}
}
