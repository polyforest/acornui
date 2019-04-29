package com.acornui.collection

actual fun <V> stringMapOf(vararg pairs: Pair<String, V>): MutableMap<String, V> = kotlin.collections.stringMapOf(*pairs)