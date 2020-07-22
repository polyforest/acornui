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

@file:Suppress("LeakingThis")

package com.acornui.component

import com.acornui.component.style.StyleTag
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.skins.Theme

/**
 * @author nbilyk
 */
open class StageImpl(owner: Context) : Stage, DivComponent(owner) {

	init {
		dependencies += listOf(Stage to this)
	}

//	private val popUpManager = inject(PopUpManager)
//	private val popUpManagerView: UiComponent

	init {
//		dependencies += TooltipManager to TooltipManagerImpl(this)

		addClass(styleTag)
//		popUpManagerView = addChild(popUpManager.init(this))
//		popUpManagerView.layoutInvalidatingFlags = 0
	}

	//-------------------------------------------------------------
	// External elements
	// Pop up manager view
	//-------------------------------------------------------------

	companion object {

		val styleTag = StyleTag("StageImpl")

		init {
			addCssToHead("""
$styleTag *:focus {
	box-shadow: 0 0 0 ${cssVar(Theme::focus)};
	outline: none;
	transition: box-shadow 0.2s ease-in-out;
}

$styleTag {
	position: relative;
	width: 100%;
	height: 100%;
	background: ${cssVar(Theme::stageBackground)};
	color: ${cssVar(Theme::textColor)};
}

$styleTag > * {
	width: 100%;
	height: 100%;
}

			""")
		}
	}

}
