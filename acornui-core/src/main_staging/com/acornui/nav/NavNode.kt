package com.acornui.nav

import com.acornui.browser.decodeURIComponent
import com.acornui.browser.encodeURIComponent


data class NavNode(
		val name: String,
		val params: Map<String, String> = HashMap()
) {

	override fun toString(): String {
		if (params.isEmpty()) return encodeURIComponent(name)
		var str = "${encodeURIComponent(name)}?"
		var isFirst = true
		val orderedParams = params.entries.sortedBy { it.key }
		for (entry in orderedParams) {
			if (!isFirst) str += "&"
			else isFirst = false
			str += encodeURIComponent(entry.key) + "=" + encodeURIComponent(entry.value)
		}
		return str
	}

	companion object {
		fun fromStr(str: String): NavNode {
			val split = str.split("?")
			val params = stringMapOf<String>()
			val name = decodeURIComponent(split[0])
			if (split.size > 1) {
				val paramsSplit = split[1].split("&")
				for (i in 0..paramsSplit.lastIndex) {
					val keyValue = paramsSplit[i].split("=")
					val key = decodeURIComponent(keyValue[0])
					val value = decodeURIComponent(keyValue[1])
					params[key] = value
				}
			}
			return NavNode(name, params)
		}
	}
}