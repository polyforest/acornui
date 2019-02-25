/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.recycle.Clearable

/**
 * A dual-key map.
 * @author nbilyk
 */
class DualHashMap<J, K, V>(
		var removeEmptyInnerMaps: Boolean = false
) : Clearable {

	val map: MutableMap<J, HashMap<K, V>> = HashMap()

	fun put(key1: J, key2: K, value: V) {
		var inner = map[key1]
		if (inner == null) {
			inner = HashMap<K, V>()
			map.put(key1, inner)
		}
		inner.put(key2, value)
	}

	operator fun get(key1: J, key2: K): V? {
		return map[key1]?.get(key2)
	}

	operator fun get(key1: J): MutableMap<K, V>? {
		return map[key1]
	}

	fun remove(key: J): MutableMap<K, V>? {
		return map.remove(key)
	}

	fun remove(key1: J, key2: K): V? {
		val inner: HashMap<K, V> = map[key1] ?: return null
		val value = inner.remove(key2)
		if (removeEmptyInnerMaps && inner.isEmpty()) {
			map.remove(key1)
		}
		return value
	}

	override fun clear() {
		map.clear()
	}
}