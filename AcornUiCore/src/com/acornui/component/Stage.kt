/*
 * Copyright 2015 Nicholas Bilyk
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
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.core.di.DKey
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.focus.Focusable
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo

interface StageRo : ContainerRo, Focusable

interface Stage : ElementContainer<UiComponent>, StageRo {

	val style: StageStyle

	companion object : DKey<Stage>, StyleTag
}

class StageStyle : StyleBase() {

	override val type: StyleType<StageStyle> = StageStyle

	var backgroundColor: ColorRo by prop(Color.WHITE)

	companion object : StyleType<StageStyle>
}

val Scoped.stage: Stage
	get() = inject(Stage)