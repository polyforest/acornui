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

package com.acornui.collection

inline fun <K, V> MutableMap<K, V>.cached(key: K, value: V, update: () -> Unit) {
	if (this[key] == value) return
	update()
	this[key] = value
}

// Boolean

inline fun <K> MutableMap<K, BooleanArray>.cached1(key: K, value1: Boolean, update: () -> Unit) {
	val arr = this.getOrPut(key) { BooleanArray(1) }
	if (arr[0] == value1) return
	update()
	arr[0] = value1
}

inline fun <K> MutableMap<K, BooleanArray>.cached2(key: K, value1: Boolean, value2: Boolean, update: () -> Unit) {
	val arr = this.getOrPut(key) { BooleanArray(2) }
	if (arr[0] == value1 && arr[1] == value2) return
	update()
	arr[0] = value1
	arr[1] = value2
}

inline fun <K> MutableMap<K, BooleanArray>.cached3(key: K, value1: Boolean, value2: Boolean, value3: Boolean, update: () -> Unit) {
	val arr = this.getOrPut(key) { BooleanArray(3) }
	if (arr[0] == value1 && arr[1] == value2 && arr[2] == value3) return
	update()
	arr[0] = value1
	arr[1] = value2
	arr[2] = value3
}

inline fun <K> MutableMap<K, BooleanArray>.cached4(key: K, value1: Boolean, value2: Boolean, value3: Boolean, value4: Boolean, update: () -> Unit) {
	val arr = this.getOrPut(key) { BooleanArray(4) }
	if (arr[0] == value1 && arr[1] == value2 && arr[2] == value3 && arr[3] == value4) return
	update()
	arr[0] = value1
	arr[1] = value2
	arr[2] = value3
	arr[3] = value4
}

inline fun <K> MutableMap<K, BooleanArray>.cached(key: K, value: BooleanArray, update: () -> Unit) {
	val arr = this.getOrPut(key) { BooleanArray(value.size) }
	if (arr.contentEquals(value)) return
	update()
	value.copyInto(arr)
}

// Int

inline fun <K> MutableMap<K, IntArray>.cached1(key: K, value1: Int, update: () -> Unit) {
	val arr = this.getOrPut(key) { IntArray(1) }
	if (arr[0] == value1) return
	update()
	arr[0] = value1
}

inline fun <K> MutableMap<K, IntArray>.cached2(key: K, value1: Int, value2: Int, update: () -> Unit) {
	val arr = this.getOrPut(key) { IntArray(2) }
	if (arr[0] == value1 && arr[1] == value2) return
	update()
	arr[0] = value1
	arr[1] = value2
}

inline fun <K> MutableMap<K, IntArray>.cached3(key: K, value1: Int, value2: Int, value3: Int, update: () -> Unit) {
	val arr = this.getOrPut(key) { IntArray(3) }
	if (arr[0] == value1 && arr[1] == value2 && arr[2] == value3) return
	update()
	arr[0] = value1
	arr[1] = value2
	arr[2] = value3
}

inline fun <K> MutableMap<K, IntArray>.cached4(key: K, value1: Int, value2: Int, value3: Int, value4: Int, update: () -> Unit) {
	val arr = this.getOrPut(key) { IntArray(4) }
	if (arr[0] == value1 && arr[1] == value2 && arr[2] == value3 && arr[3] == value4) return
	update()
	arr[0] = value1
	arr[1] = value2
	arr[2] = value3
	arr[3] = value4
}

inline fun <K> MutableMap<K, IntArray>.cached(key: K, value: IntArray, update: () -> Unit) {
	val arr = this.getOrPut(key) { IntArray(value.size) }
	if (arr.contentEquals(value)) return
	update()
	value.copyInto(arr)
}

// Float

inline fun <K> MutableMap<K, FloatArray>.cached1(key: K, value1: Float, update: () -> Unit) {
	val arr = this.getOrPut(key) { FloatArray(1) }
	if (arr[0] == value1) return
	update()
	arr[0] = value1
}

inline fun <K> MutableMap<K, FloatArray>.cached2(key: K, value1: Float, value2: Float, update: () -> Unit) {
	val arr = this.getOrPut(key) { FloatArray(2) }
	if (arr[0] == value1 && arr[1] == value2) return
	update()
	arr[0] = value1
	arr[1] = value2
}

inline fun <K> MutableMap<K, FloatArray>.cached3(key: K, value1: Float, value2: Float, value3: Float, update: () -> Unit) {
	val arr = this.getOrPut(key) { FloatArray(3) }
	if (arr[0] == value1 && arr[1] == value2 && arr[2] == value3) return
	update()
	arr[0] = value1
	arr[1] = value2
	arr[2] = value3
}

inline fun <K> MutableMap<K, FloatArray>.cached4(key: K, value1: Float, value2: Float, value3: Float, value4: Float, update: () -> Unit) {
	val arr = this.getOrPut(key) { FloatArray(4) }
	if (arr[0] == value1 && arr[1] == value2 && arr[2] == value3 && arr[3] == value4) return
	update()
	arr[0] = value1
	arr[1] = value2
	arr[2] = value3
	arr[3] = value4
}

inline fun <K> MutableMap<K, FloatArray>.cached(key: K, value: FloatArray, update: () -> Unit) {
	val arr = this.getOrPut(key) { FloatArray(value.size) }
	if (arr.contentEquals(value)) return
	update()
	value.copyInto(arr)
}