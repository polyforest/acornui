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

package com.acornui.factory

import com.acornui.core.Disposable

class LazyInstance<out R, out T>(
		private val receiver: R,
		private val factory: R.() -> T
) {

	private var _created = false
	private var _instance: T? = null

	val created: Boolean
		get() = _created

	val instance: T
		get() {
			if (!_created) {
				_instance = receiver.factory()
				_created = true
			}
			@Suppress("UNCHECKED_CAST")
			return _instance as T
		}

	fun clear() {
		_instance = null
		_created = false
	}
}

/**
 * Creates a LazyInstance object that isn't lazy. Can be used to coerce an instance in places that expect laziness.
 */
fun <R, T> R.lazyInstance(instance: T): LazyInstance<R, T> {
	val notReallyLazyInstance= LazyInstance(this) { instance }
	notReallyLazyInstance.instance // Mark as created
	return notReallyLazyInstance
}

fun <R, T> R.lazyInstance(factory: R.() -> T): LazyInstance<R, T> {
	val notReallyLazyInstance= LazyInstance(this, factory)
	notReallyLazyInstance.instance // Mark as created
	return notReallyLazyInstance
}

fun <R, T : Disposable?> LazyInstance<R, T>.disposeInstance() {
	if (created) {
		instance?.dispose()
		clear()
	}
}
