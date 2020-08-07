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
import com.acornui.string.toRadix
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class CssClass(val className: String) {

	override fun toString(): String = ".$className"
}

class CssProp(val propName: String) {

	override fun toString(): String = "--$propName"

	val v: String = "var(--$propName)"
}

private var cssUid = 0

fun cssClass(): ReadOnlyProperty<Any?, CssClass> {

	return object : ReadOnlyProperty<Any?, CssClass> {

		private var className: CssClass? = null

		override fun getValue(thisRef: Any?, property: KProperty<*>): CssClass {
			if (className == null)
				className = CssClass(property.name + "_" + (++cssUid).toRadix(36))
			return className!!
		}
	}
}

fun cssProp(): ReadOnlyProperty<Any?, CssProp> {

	return object : ReadOnlyProperty<Any?, CssProp> {

		private var className: CssProp? = null

		override fun getValue(thisRef: Any?, property: KProperty<*>): CssProp {
			if (className == null)
				className = CssProp(property.name + "_" + (++cssUid).toRadix(36))
			return className!!
		}
	}
}

class StyleTagToggle(private val styleTag: CssClass) : ReadWriteProperty<UiComponent, Boolean> {

	override fun getValue(thisRef: UiComponent, property: KProperty<*>): Boolean =
		thisRef.containsClass(styleTag)

	override fun setValue(thisRef: UiComponent, property: KProperty<*>, value: Boolean) {
		if (thisRef.containsClass(styleTag) != value) {
			thisRef.toggleClass(styleTag)
		}
	}
}

fun styleTagToggle(styleTag: CssClass) = StyleTagToggle(styleTag)