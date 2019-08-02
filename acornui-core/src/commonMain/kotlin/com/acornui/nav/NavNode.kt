/*
 * Copyright (c) 2019. Matrix Precise
 */

package com.acornui.nav

import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import com.acornui.collection.stringMapOf

data class NavNode(
		val name: String,
		val params: Map<String, String> = HashMap()
) {

	override fun toString(): String {
		if (params.isEmpty()) return encodeUriComponent2(name)
		var str = "${encodeUriComponent2(name)}?"
		var isFirst = true
		val orderedParams = params.entries.sortedBy { it.key }
		for (entry in orderedParams) {
			if (!isFirst) str += "&"
			else isFirst = false
			str += encodeUriComponent2(entry.key) + "=" + encodeUriComponent2(entry.value)
		}
		return str
	}

	companion object {
		fun fromStr(str: String): NavNode {
			val split = str.split("?")
			val params = stringMapOf<String>()
			val name = decodeUriComponent2(split[0])
			if (split.size > 1) {
				val paramsSplit = split[1].split("&")
				for (i in 0..paramsSplit.lastIndex) {
					val keyValue = paramsSplit[i].split("=")
					val key = decodeUriComponent2(keyValue[0])
					val value = decodeUriComponent2(keyValue[1])
					params[key] = value
				}
			}
			return NavNode(name, params)
		}
	}
}