package com.acornui.collection

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

val mapPool = object : ObjectPool<MutableMap<*, *>>(8, { HashMap<Any?, Any?>() }) {
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
		other.put(newKey, newValue)
	}
	return other
}