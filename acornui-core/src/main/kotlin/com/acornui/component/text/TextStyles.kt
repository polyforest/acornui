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

@file:Suppress("unused")

package com.acornui.component.text

import com.acornui.component.ComponentInit
import com.acornui.component.style.cssClass
import com.acornui.di.Context
import com.acornui.dom.createElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Common text style tags.
 */
object TextStyleTags {

	val error by cssClass()
	val warning by cssClass()
	val info by cssClass()
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.error] tag.
 */
inline fun Context.errorText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextField(this)
	t.addClass(TextStyleTags.error)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.warning] tag.
 */
inline fun Context.warningText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextField(this)
	t.addClass(TextStyleTags.warning)
	t.text = text
	t.init()
	return t
}

/**
 * A shortcut to creating a text field with the [TextStyleTags.info] tag.
 */
inline fun Context.infoText(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextField(this)
	t.addClass(TextStyleTags.info)
	t.text = text
	t.init()
	return t
}

object FontStyle {
	const val NORMAL = "normal"
	const val ITALIC = "italic"
}

/**
 * Needs to be an inline element without a baseline (so its bottom (not baseline) is used for alignment)
 */
private val baselineLocator by lazy {
	createElement<HTMLImageElement>("img") {
		style.verticalAlign = "baseline"
	}
}

/**
 * Calculates the baseline height of this element by temporarily adding an empty component with no baseline and a
 * baseline alignment to this element, measuring its bottom bounds and comparing to this component's bottom bounds.
 */
fun HTMLElement.calculateBaselineHeight(): Double {
	val bottomY = getBoundingClientRect().bottom
	appendChild(baselineLocator)
	val baselineY = baselineLocator.getBoundingClientRect().bottom
	removeChild(baselineLocator)
	return bottomY - baselineY
}