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

package com.acornui.component.style

import com.acornui.component.UiComponent
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline class StyleTag(val className: String)  {

	override fun toString(): String = ".$className"
}


class StyleTagToggle(private val styleTag: StyleTag) : ReadWriteProperty<UiComponent, Boolean> {

	override fun getValue(thisRef: UiComponent, property: KProperty<*>): Boolean {
		return thisRef.dom.classList.contains(styleTag.className)
	}

	override fun setValue(thisRef: UiComponent, property: KProperty<*>, value: Boolean) {
		if (getValue(thisRef, property) != value) {
			thisRef.toggleClass(styleTag)
		}
	}
}

fun styleTagToggle(styleTag: StyleTag) = StyleTagToggle(styleTag)