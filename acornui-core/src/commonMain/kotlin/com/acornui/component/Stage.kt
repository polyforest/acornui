/*
 * Copyright 2019 Poly Forest, LLC
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

import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.DKey
import com.acornui.di.Injector
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.focus.Focusable
import com.acornui.graphic.ColorRo

interface StageRo : ContainerRo, Focusable

interface Stage : ElementContainer<UiComponent>, StageRo {

	val style: StageStyle
	var showWaitingForSkinMessage: Boolean

	companion object : DKey<Stage> {
		override fun factory(injector: Injector): Stage? {
			return StageImpl(injector)
		}
	}
}

val Scoped.stage: Stage
	get() = inject(Stage)

class StageStyle : StyleBase() {

	override val type = Companion

	/**
	 * If null, this will default to the [com.acornui.WindowConfig.backgroundColor] property.
	 */
	var backgroundColor: ColorRo? by prop(null)

	/**
	 * If true, only invalidated sections of the window will be cleared every frmae.
	 * 
	 * This should be set to false if the entire screen is subject to frequent changes.
	 */
	var useRedrawRegions: Boolean by prop(true)

	/**
	 * True if the redraw regions should be shown for debugging purposes.
	 */
	var showRedrawRegions: Boolean by prop(false)

	companion object : StyleType<StageStyle>
}
