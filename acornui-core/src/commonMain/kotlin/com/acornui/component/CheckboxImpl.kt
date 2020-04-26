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

import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.signal.Signal
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class CheckboxImpl(
		owner: Context
) : ButtonImpl(owner), InputComponent<Boolean> {

	init {
		styleTags.add(CheckboxImpl)
		toggleOnClick = true
	}

	@Suppress("UNCHECKED_CAST")
	final override val changed: Signal<(CheckboxImpl) -> Unit> = toggledChanged as Signal<(CheckboxImpl) -> Unit>

	override var inputValue: Boolean
		get() = toggled
		set(value) {
			toggled = value
		}

	companion object : StyleTag
}

inline fun Context.checkbox(init: ComponentInit<CheckboxImpl> = {}): CheckboxImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = CheckboxImpl(this)
	c.init()
	return c
}

inline fun Context.checkbox(label: String, init: ComponentInit<CheckboxImpl> = {}): CheckboxImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = CheckboxImpl(this)
	b.label = label
	b.init()
	return b
}
