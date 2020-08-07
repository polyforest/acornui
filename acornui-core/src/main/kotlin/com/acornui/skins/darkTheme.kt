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

package com.acornui.skins

import com.acornui.component.PanelStyle
import com.acornui.component.style.CssClass
import com.acornui.component.style.cssClass
import com.acornui.dom.addStyleToHead

private object DarkTheme {

	val darkTheme by cssClass()

	init {
		DefaultStyles
		addStyleToHead("""
			
$darkTheme {
	background: #222;
    color: #ccc;
	${CssProps.toggledInner}: #fff;
	${CssProps.borderColor}: #29395d;
	${CssProps.componentBackground}: rgba(33, 33, 33, 0.9);
	${CssProps.buttonBackground}: #1b2235;
	${CssProps.buttonBackgroundHover}: #224;
	${CssProps.buttonBackgroundActive}: #112;
	${CssProps.buttonTextColor}: #ccc;
	${CssProps.buttonTextHoverColor}: #ddd;
	${CssProps.buttonTextActiveColor}: #bbb;
	${CssProps.inputBackground}: rgba(33, 33, 33, 0.9);
	${CssProps.inputTextColor}: #ccc;
	${CssProps.componentShadow}: 0;
	${CssProps.dataRowEvenBackground}: #333;
	${CssProps.dataRowOddBackground}: #222;
}

$darkTheme footer {
	color: #bbb;
    background-color: #262728;
}

$darkTheme ${PanelStyle.colors} {
	background: rgba(31, 35, 37, 0.75);
	color: #ccc;
	${CssProps.scrollbarButtonColor}: #666;
	box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.9);
}
	""", priority = 1.0)
	}
}

val darkTheme: CssClass
	get() = DarkTheme.darkTheme