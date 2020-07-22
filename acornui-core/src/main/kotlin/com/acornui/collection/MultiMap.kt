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

typealias MultiMap2<K1, K2, V> = MapWithDefault<K1, Map<K2, V>>
typealias MutableMultiMap2<K1, K2, V> = MutableMapWithDefault<K1, MutableMap<K2, V>>

operator fun <K1, K2, V> MultiMap2<K1, K2, V>.get(key1: K1, key2: K2): V? {
	if (!containsKey(key1)) return null
	return this[key1][key2]
}

fun <K1, K2, V> MutableMultiMap2<K1, K2, V>.remove(key1: K1, key2: K2, removeEmptyMaps: Boolean = true): V? {
	val m1 = this[key1]
	val old = m1.remove(key2)
	if (removeEmptyMaps && m1.isEmpty()) remove(key1)
	return old
}

typealias MultiMap3<K1, K2, K3, V> = MapWithDefault<K1, MultiMap2<K2, K3, V>>
typealias MutableMultiMap3<K1, K2, K3, V> = MutableMapWithDefault<K1, MutableMultiMap2<K2, K3, V>>

operator fun <K1, K2, K3, V> MultiMap3<K1, K2, K3, V>.get(key1: K1, key2: K2, key3: K3): V? {
	if (!containsKey(key1)) return null
	return this[key1][key2, key3]
}

fun <K1, K2, K3, V> MutableMultiMap3<K1, K2, K3, V>.remove(key1: K1, key2: K2, key3: K3, removeEmptyMaps: Boolean = true): V? {
	val m1 = this[key1]
	val old = m1.remove(key2, key3, removeEmptyMaps)
	if (removeEmptyMaps && m1.isEmpty()) remove(key1)
	return old
}

typealias MultiMap4<K1, K2, K3, K4, V> = MapWithDefault<K1, MultiMap3<K2, K3, K4, V>>
typealias MutableMultiMap4<K1, K2, K3, K4, V> = MutableMapWithDefault<K1, MutableMultiMap3<K2, K3, K4, V>>

operator fun <K1, K2, K3, K4, V> MultiMap4<K1, K2, K3, K4, V>.get(key1: K1, key2: K2, key3: K3, key4: K4): V? {
	if (!containsKey(key1)) return null
	return this[key1][key2, key3, key4]
}

fun <K1, K2, K3, K4, V> MutableMultiMap4<K1, K2, K3, K4, V>.remove(key1: K1, key2: K2, key3: K3, key4: K4, removeEmptyMaps: Boolean = true): V? {
	val m1 = this[key1]
	val old = m1.remove(key2, key3, key4, removeEmptyMaps)
	if (removeEmptyMaps && m1.isEmpty()) remove(key1)
	return old
}

inline fun <reified K1, reified K2, V> multiMap2(): MutableMultiMap2<K1, K2, V> =
		HashMapWithDefault(stringOrHashMapOf()) { stringOrHashMapOf() }

inline fun <reified K1, reified K2, reified K3, V> multiMap3(): MutableMultiMap3<K1, K2, K3, V> =
		HashMapWithDefault(stringOrHashMapOf()) { multiMap2() }

inline fun <reified K1, reified K2, reified K3, reified K4, V> multiMap4(): MutableMultiMap4<K1, K2, K3, K4, V> =
		HashMapWithDefault(stringOrHashMapOf()) { multiMap3() }

fun <K1, K2, V> MultiMap2<K1, K2, V>.iterateValues2(inner: (V) -> Unit) {
	for (value in values) {
		for (v2 in value.values) {
			inner(v2)
		}
	}
}

fun <K1, K2, K3, V> MultiMap3<K1, K2, K3, V>.iterateValues3(inner: (V) -> Unit) {
	for (value in values) {
		value.iterateValues2(inner)
	}
}

fun <K1, K2, K3, K4, V> MultiMap4<K1, K2, K3, K4, V>.iterateValues4(inner: (V) -> Unit) {
	for (value in values) {
		value.iterateValues3(inner)
	}
}

/**
 * Checks the type of K and creates a string map or a hashmap accordingly.
 */
inline fun <reified K, V> stringOrHashMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V> {
	@Suppress("UNCHECKED_CAST")
	return if (K::class == String::class) stringMapOf(*pairs as Array<out Pair<String, V>>) as MutableMap<K, V> else hashMapOf(*pairs)
}
