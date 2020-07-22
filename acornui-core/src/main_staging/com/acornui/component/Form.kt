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

import com.acornui.component.layout.algorithm.GridLayoutContainer
import com.acornui.component.style.styleTag
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.di.Context
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val formLabelStyleTag = styleTag()

inline fun Context.formLabel(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return text(text) {
		addClass(formLabelStyleTag)
	}.apply(init)
}

val formStyleTag = styleTag()

inline fun <E : UiComponent> Context.gridForm(init: ComponentInit<GridLayoutContainer<E>> = {}): GridLayoutContainer<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return GridLayoutContainer<E>(this).apply {
		addClass(formStyleTag)
		init()
	}
}

inline fun Context.gridForm(init: ComponentInit<GridLayoutContainer<UiComponent>> = {}): GridLayoutContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return gridForm<UiComponent>(init)
}