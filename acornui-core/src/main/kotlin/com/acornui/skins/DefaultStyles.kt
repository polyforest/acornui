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

import com.acornui.component.StageStyle
import com.acornui.component.input.InputStyles
import com.acornui.component.style.CommonStyleTags
import com.acornui.component.text.TextStyleTags
import com.acornui.dom.addStyleToHead
import com.acornui.google.MaterialIconsCss

object DefaultStyles {

	init {

		js("""
require("focus-visible");			
		""")

		MaterialIconsCss
		InputStyles
		val s = StageStyle.stage

		addStyleToHead(
			"""
$s * {
	box-sizing: border-box;
}

$s ul {
	padding: 0 0 calc(-1 * ${CssProps.gap.v}) 0;
	list-style: none;
}

$s ul li {
	margin-bottom: ${CssProps.gap.v} 
}

$s a {
	color: ${CssProps.toggled.v};
	text-decoration: none;
	border-radius: ${CssProps.borderRadius.v};
}

$s a:hover {
	color: ${CssProps.borderHover.v};
	text-decoration: none;
}

$s a:active {
	color: ${CssProps.borderActive.v};
}


/* ScrollBar Style */

* {
	scrollbar-face-color: ${CssProps.scrollbarButtonColor.v};
	scrollbar-base-color: ${CssProps.scrollbarCornerColor.v};
	scrollbar-3dlight-color: ${CssProps.scrollbarButtonColor.v};
	scrollbar-highlight-color: ${CssProps.scrollbarButtonColor.v};
	scrollbar-track-color: ${CssProps.scrollbarTrackColor.v};
	scrollbar-arrow-color: ${CssProps.scrollbarButtonColor.v};
	scrollbar-shadow-color: ${CssProps.scrollbarButtonColor.v};
	
	/* Firefox */
	scrollbar-color: ${CssProps.scrollbarButtonColor.v} ${CssProps.scrollbarTrackColor.v} ${CssProps.scrollbarButtonColor.v} ;
	scrollbar-width: thin;
}

::-webkit-scrollbar {
	width: ${CssProps.scrollbarThickness.v};
	height: ${CssProps.scrollbarThickness.v};
}

::-webkit-scrollbar-button {
	background-color: ${CssProps.scrollbarButtonColor.v};
	height: 0;
	width: 0;
}

::-webkit-scrollbar-track {
	background-color: ${CssProps.scrollbarTrackColor.v};
}

::-webkit-scrollbar-thumb {
	background-color: ${CssProps.scrollbarButtonColor.v};
	border-radius: ${CssProps.scrollbarBorderRadius.v};
}

::-webkit-scrollbar-corner {
	background-color: ${CssProps.scrollbarCornerColor.v};
}

/* 	A way to hide an element without using visibility: hidden. Typically used with Safari workarounds. */
${CommonStyleTags.hidden} {
	position: absolute; 
	left: -9999px; 
	width: 1px; 
	height: 1px;
}

:root {
    ${CssProps.scrollbarTrackColor}: #111;
    ${CssProps.scrollbarButtonColor}: #666;
    ${CssProps.scrollbarCornerColor}: #333;
    ${CssProps.scrollbarThickness}: 8px;
    ${CssProps.focus}: rgba(49, 104, 254, .8);
    ${CssProps.focusThickness}: 2px;
    ${CssProps.toggled}: #275efe;
    ${CssProps.toggledInner}: #fff;
    ${CssProps.toggledInnerDisabled}: #888;
    ${CssProps.borderColor}: #bbc1e1;
    ${CssProps.borderHover}: #0f3ef8;
    ${CssProps.borderActive}: #445ed9;
    ${CssProps.borderDisabled}: #bbc1e1;
    ${CssProps.componentBackground}: #fff;
    ${CssProps.disabled}: #7a7b82;
    ${CssProps.disabledOpacity}: 0.8;
    ${CssProps.disabledInner}: #bfbfbf;
    ${CssProps.buttonBackground}: #f3f4f7;
    ${CssProps.buttonBackgroundHover}: #fff;
    ${CssProps.buttonBackgroundActive}: #e3e4e7;
    ${CssProps.buttonTextColor}: #333;
    ${CssProps.buttonTextHoverColor}: #444;
    ${CssProps.buttonTextActiveColor}: #222;
    ${CssProps.borderThickness}: 1px;
    ${CssProps.componentPadding}: 4px;
    ${CssProps.borderRadius}: 8px;
    ${CssProps.inputBackground}: #fffe;
    ${CssProps.inputTextColor}: #333;
    ${CssProps.inputBorderRadius}: 2px;
    ${CssProps.inputPadding}: 4px;
    ${CssProps.gap}: 6px;
    ${CssProps.padding}: 6px;
    ${CssProps.componentShadow}: 1px 1px 3px rgba(0, 0, 0, 0.2);
    ${CssProps.dataRowEvenBackground}: #eeef;
    ${CssProps.dataRowOddBackground}: #ddde;
    ${CssProps.loadingSpinnerColor}: #ddd;		
    ${CssProps.strongWeight}: bolder;		
}

footer {
	color: #222;
    background-color: #bbb;
}

${CommonStyleTags.controlBar} {
	background: #393b48;
	color: #dbdce2;
}

${TextStyleTags.error} {
	color: #b22;
}

${TextStyleTags.warning} {
	color: #bb2;
}

${TextStyleTags.info} {
	color: #262;
}

		""", priority = -1.0)
	}
}