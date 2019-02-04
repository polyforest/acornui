/*
 * Copyright 2019 Nicholas Bilyk
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

package com.acornui.signal

/**
 * A Signal placeholder that contains no handlers and cannot be dispatched.
 */
internal object EmptySignal : Signal<Any> {

	override val isDispatching: Boolean = false

	override fun isNotEmpty(): Boolean = false
	override fun isEmpty(): Boolean = true
	override fun add(handler: Any, isOnce: Boolean) {}

	override fun remove(handler: Any) {}

	override fun contains(handler: Any): Boolean = false

	override fun addBinding(callback: () -> Unit) {}

	override fun removeBinding(callback: () -> Unit) {}
}

fun <T : Any> emptySignal(): Signal<T> = EmptySignal