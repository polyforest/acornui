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

import com.acornui.component.style.StyleTag
import com.acornui.component.style.styleTag
import com.acornui.core.di.Owned

open class Checkbox(
		owner: Owned
) : Button(owner) {

	init {
		styleTags.add(Checkbox)
		toggleOnClick = true
	}

	companion object : StyleTag {
		val NO_LABEL = styleTag()
	}
}

fun Owned.checkbox(init: ComponentInit<Checkbox> = {}): Checkbox {
	val c = Checkbox(this)
	c.init()
	return c
}

fun Owned.checkbox(label: String, init: ComponentInit<Checkbox> = {}): Checkbox {
	val b = Checkbox(this)
	b.label = label
	b.init()
	return b
}