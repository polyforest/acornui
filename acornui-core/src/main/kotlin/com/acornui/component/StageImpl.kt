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

import com.acornui.component.style.cssClass
import com.acornui.di.Context
import com.acornui.dom.addStyleToHead
import com.acornui.skins.CssProps
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * The Stage is the root element of an application. By default it is 100% width, 100% height.
 * @author nbilyk
 */
open class StageImpl(owner: Context) : Stage, Div(owner) {

	init {
		addClass(StageStyle.stage)
	}

}

object StageStyle {

	val stage by cssClass()

	init {
		addStyleToHead("""
			
*:focus:not(.focus-visible) {
	/* Don't show an outline for elements focused via mouse */
	outline: none;
}			
			
$stage .focus-visible {
	box-shadow: 0 0 0 ${CssProps.focusThickness.v} ${CssProps.focus.v};
	outline: none;
	transition: box-shadow 0.2s ease-in-out;
}

$stage {
	width: 100%;
	height: 100%;
	background: #444;
    color: #ddd;
}

$stage > * {
	width: 100%;
	height: 100%;
}

			""")
	}
}

/**
 * Creates a [Stage] component.
 */
inline fun Context.stage(init: ComponentInit<StageImpl> = {}): StageImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return StageImpl(this).apply(init)
}
