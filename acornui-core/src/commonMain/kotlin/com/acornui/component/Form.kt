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

import com.acornui.component.layout.algorithm.GridLayout
import com.acornui.component.layout.algorithm.GridLayoutData
import com.acornui.component.layout.algorithm.GridLayoutStyle
import com.acornui.component.style.styleTag
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.validation.ValidationContainerImpl
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

val formStyleTag = styleTag()

typealias FormContainer<T> = ValidationContainerImpl<T, GridLayoutStyle, GridLayoutData, UiComponent>

/**
 * Creates a grid layout container with a style tag for forms.
 *
 * The basic skin will set this as two columns, where the first column is right aligned and the second column left
 * aligned.
 */
inline fun <T> Context.form(init: ComponentInit<FormContainer<T>> = {}): FormContainer<T> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationContainerImpl<T, GridLayoutStyle, GridLayoutData, UiComponent>(this, GridLayout()).apply {
		styleTags.add(formStyleTag)
		init()
	}
}

/**
 * Creates a grid layout container with a style tag for forms.
 *
 * The basic skin will set this as two columns, where the first column is right aligned and the second column left
 * aligned.
 */
@JvmName("unvalidatedForm")
inline fun Context.form(init: ComponentInit<FormContainer<Nothing>> = {}): FormContainer<Nothing> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationContainerImpl<Nothing, GridLayoutStyle, GridLayoutData, UiComponent>(this, GridLayout()).apply {
		styleTags.add(formStyleTag)
		init()
	}
}

val formLabelStyleTag = styleTag()

inline fun Context.formLabel(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return text(text) {
		styleTags.add(formLabelStyleTag)
	}.apply(init)
}