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

import com.acornui.recycle.ObjectPool

fun <K, V> Map<K, V>.containsAllKeys(keys: Array<K>): Boolean {
	for (i in 0..keys.lastIndex) {
		if (!containsKey(keys[i])) {
			return false
		}
	}
	return true
}

fun <K, V> Map<K, V>.copy(): MutableMap<K, V> {
	val m = HashMap<K, V>()
	m.putAll(this)
	return m
}

val mapPool = object : ObjectPool<MutableMap<*, *>>({ HashMap<Any?, Any?>() }) {
	override fun free(obj: MutableMap<*, *>) {
		obj.clear()
		super.free(obj)
	}
}

/**
 * Uses a transform method to create entries to put inside the [other] map.
 *
 * @param other The map to put the transformed keys and values into.
 * @param transform A transform method to convert keys and values from the receiver to new keys and values.
 * @return Returns the [other] map.
 */
inline fun <K, V, K2, V2> Map<K, V>.mapTo(other: MutableMap<K2, V2> = HashMap(), transform: (key: K, value: V) -> Pair<K2, V2>): MutableMap<K2, V2> {
	for ((key, value) in this) {
		val (newKey, newValue) = transform(key, value)
		other[newKey] = newValue
	}
	return other
}

fun <K, V> Map<K, V?>.toNotNull(): MutableMap<K, V> {
	val newMap = HashMap<K, V>()
	for ((k, v) in entries) {
		if (v != null)
			newMap[k] = v
	}
	return newMap
}


expect fun <V> stringMapOf(vararg pairs: Pair<String, V>): MutableMap<String, V>

class HashMapWithDefault<K, V>(private val wrapped: MutableMap<K, V> = HashMap(), private val defaultProvider: (K) -> V) : MutableMap<K, V> by wrapped, MutableMapWithDefault<K, V> {

	override fun get(key: K): V {
		if (!wrapped.containsKey(key)) {
			put(key, defaultProvider(key))
		}
		return wrapped[key]!!
	}
}

interface MapWithDefault<K, out V> : Map<K, V> {
	override fun get(key: K): V
}

interface MutableMapWithDefault<K, V> : MutableMap<K, V>, MapWithDefault<K, V>