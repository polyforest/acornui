/*
 * Copyright 2018 Poly Forest
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

package com.acornui.reflect

import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Similar to Delegates.observable, except vetoes when oldValue == newValue and onChange only takes newValue as a
 * parameter.
 */
inline fun <T> observable(initialValue: T, crossinline onChange: (newValue: T) -> Unit):
		ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {

	override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean {
		return oldValue != newValue
	}

	override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange(newValue)
}

/**
 * [observable] Except also invokes the [onChange] callback with [initialValue]
 */
inline fun <T> observableAndCall(initialValue: T, crossinline onChange: (newValue: T) -> Unit):
		ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {

	override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean {
		return oldValue != newValue
	}

	override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange(newValue)

	init {
		onChange(initialValue)
	}
}