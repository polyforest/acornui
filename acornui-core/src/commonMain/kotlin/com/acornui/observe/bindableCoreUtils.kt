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

package com.acornui.observe

import com.acornui.Disposable
import com.acornui.ManagedDisposable
import com.acornui.di.Context
import com.acornui.di.onDisposed
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

/**
 * Adds a callback and invokes it immediately.
 *
 * This binding is owned and will be automatically removed when this context is disposed.
 *
 * @param bindable The bindable object to watch for changes.
 * @return Returns a [ManagedDisposable] object that will remove the callback on [Disposable.dispose]
 */
fun Context.bind(bindable: Bindable, callback: () -> Unit): ManagedDisposable {
	contract { callsInPlace(callback, InvocationKind.AT_LEAST_ONCE) }
	bindable.addBinding(callback)
	callback()
	return onDisposed {
		bindable.removeBinding(callback)
	}
}

/**
 *
 * @return If [bindable] is null, returns null, otherwise returns a [Disposable] object that will remove the callback
 * on [Disposable.dispose]
 */
@JvmName("bindNullable")
fun Context.bind(bindable: Bindable?, callback: () -> Unit): ManagedDisposable? {
	if (bindable == null) return null
	return bind(bindable, callback)
}