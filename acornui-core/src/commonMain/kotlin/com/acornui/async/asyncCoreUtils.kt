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

package com.acornui.async

import com.acornui.collection.Tuple4
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * Invokes a callback when the deferred value has been computed successfully. This callback will not be invoked on
 * failure.
 */
infix fun <T> Deferred<T>.then(callback: (result: T) -> Unit): Deferred<T> {
	globalLaunch {
		var successful = false
		var result: T? = null
		try {
			result = await()
			successful = true
		} catch (t: Throwable) {
		}
		@Suppress("UNCHECKED_CAST")
		if (successful) {
			launch(Dispatchers.UI) {
				callback(result as T)
			}
		}
	}
	return this
}

infix fun Deferred<Unit>.then(callback: () -> Unit): Deferred<Unit> {
	return then<Unit> { callback() }
}

infix fun <A, B> Deferred<Pair<A, B>>.then(callback: (result: A, B) -> Unit): Deferred<Pair<A, B>> {
	return then<Pair<A, B>> { callback(it.first, it.second) }
}

infix fun <A, B, C> Deferred<Triple<A, B, C>>.then(callback: (result: A, B, C) -> Unit): Deferred<Triple<A, B, C>> {
	return then<Triple<A, B, C>> { callback(it.first, it.second, it.third) }
}

infix fun <A, B, C, D> Deferred<Tuple4<A, B, C, D>>.then(callback: (result: A, B, C, D) -> Unit): Deferred<Tuple4<A, B, C, D>> {
	return then<Tuple4<A, B, C, D>> { callback(it.first, it.second, it.third, it.fourth) }
}

/**
 * Invokes a callback when this deferred object has failed to produce a result.
 */
infix fun <T> Deferred<T>.catch(callback: (Throwable) -> Unit): Deferred<T> {
	globalLaunch {
		try {
			await()
		} catch (t: Throwable) {
			launch(Dispatchers.UI) {
				callback(t.cause ?: t)
			}
		}
	}
	return this
}

/**
 * Invokes a callback when the deferred value has been either been computed successfully or failed.
 */
infix fun <T> Deferred<T>.finally(callback: (result: T?) -> Unit): Deferred<T> {
	globalLaunch {
		var result: T? = null
		try {
			result = await()
		} catch (t: Throwable) {}
		launch(Dispatchers.UI) {
			callback(result)
		}
	}
	return this
}