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

package com.acornui.skins

import com.acornui.component.StageImpl
import com.acornui.component.input.InputStyles
import com.acornui.component.style.CommonStyleTags
import com.acornui.css.cssVar
import com.acornui.dom.addCssToHead

object DefaultStyles {

	init {
		InputStyles
		val s = StageImpl.styleTag

		addCssToHead("""
$s * {
	box-sizing: border-box;
}

$s ul {
	padding: 0 0 calc(-1 * ${cssVar(Theme::gap)}) 0;
	list-style: none;
}

$s ul li {
	margin-bottom: ${cssVar(Theme::gap)} 
}


/* ScrollBar Style */

* {
	scrollbar-face-color: ${cssVar(Theme::scrollbarButtonColor)};
	scrollbar-base-color: ${cssVar(Theme::scrollbarCornerColor)};
	scrollbar-3dlight-color: ${cssVar(Theme::scrollbarButtonColor)};
	scrollbar-highlight-color: ${cssVar(Theme::scrollbarButtonColor)};
	scrollbar-track-color: ${cssVar(Theme::scrollbarTrackColor)};
	scrollbar-arrow-color: ${cssVar(Theme::scrollbarButtonColor)};
	scrollbar-shadow-color: ${cssVar(Theme::scrollbarButtonColor)};
	
	/* Firefox */
	scrollbar-color: ${cssVar(Theme::scrollbarButtonColor)} ${cssVar(Theme::scrollbarTrackColor)} ${cssVar(Theme::scrollbarButtonColor)} ;
	scrollbar-width: thin;
}

::-webkit-scrollbar {
	width: ${cssVar(Theme::scrollbarThickness)};
	height: ${cssVar(Theme::scrollbarThickness)};
}

::-webkit-scrollbar-button {
	background-color: ${cssVar(Theme::scrollbarButtonColor)};
	height: 0;
	width: 0;
}

::-webkit-scrollbar-track {
	background-color: ${cssVar(Theme::scrollbarTrackColor)};
}

::-webkit-scrollbar-thumb {
	background-color: ${cssVar(Theme::scrollbarButtonColor)};
	border-radius: ${cssVar(Theme::scrollbarBorderRadius)};
}

::-webkit-scrollbar-corner {
	background-color: ${cssVar(Theme::scrollbarCornerColor)};
}

footer {
	color: ${cssVar(Theme::footerTextColor)};
	background: ${cssVar(Theme::footerBackgroundColor)};
}

/* 	A way to hide an element without using visibility: hidden. Typically used with Safari workarounds. */
${CommonStyleTags.hidden} {
	position: absolute; 
	left: -9999px; 
	width: 1px; 
	height: 1px;
}

		""", priority = -1.0)
	}
}