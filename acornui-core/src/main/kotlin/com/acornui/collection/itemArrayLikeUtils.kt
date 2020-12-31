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

package com.acornui.collection

import org.w3c.dom.ItemArrayLike

fun <T> ItemArrayLike<T>.find(predicate: Filter<T>): T? {
	for (i in 0 until length) {
		val item = item(i).unsafeCast<T>()
		if (predicate(item))
			return item
	}
	return null
}

fun <T> ItemArrayLike<T>.forEach(action: (T) -> Unit) {
	for (i in 0 until length) {
		action(item(i).unsafeCast<T>())
	}
}

fun <T> ItemArrayLike<T>.first(): T {
	if (length == 0)
		throw NoSuchElementException("ItemArrayLike is empty.")
	return item(0).unsafeCast<T>()
}

fun <T> ItemArrayLike<T>.firstOrNull(): T? {
	if (length == 0) return null
	return item(0)
}