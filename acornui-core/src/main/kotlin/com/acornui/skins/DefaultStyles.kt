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
import com.acornui.css.cssVar
import com.acornui.dom.addCssToHead

object DefaultStyles {

	init {
		InputStyles
		val s = StageImpl.styleTag

		addCssToHead("""
$s * {
	box-sizing: border-box;
	margin: 0;
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
	scrollbar-face-color: #646464;
	scrollbar-base-color: #646464;
	scrollbar-3dlight-color: #646464;
	scrollbar-highlight-color: #646464;
	scrollbar-track-color: #000;
	scrollbar-arrow-color: #000;
	scrollbar-shadow-color: #646464;
}

::-webkit-scrollbar {
	width: 8px;
	height: 8px;
}

::-webkit-scrollbar-button {
	background-color: #666;
}

::-webkit-scrollbar-track {
	background-color: #646464;
}

::-webkit-scrollbar-track-piece {
	background-color: #000;
}

::-webkit-scrollbar-thumb {
	height: 50px;
	background-color: #666;
}

::-webkit-scrollbar-corner {
	background-color: #646464;
}

/*::-webkit-resizer {
	background-color: #666;
}*/

		""")
	}
}