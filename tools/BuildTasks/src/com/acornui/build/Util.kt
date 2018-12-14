package com.acornui.build

import java.io.File

@Deprecated("Use apply", ReplaceWith("apply(f)"))
inline fun <T> T.with(f: T.() -> Unit): T { this.f(); return this }

fun ArrayList<File>.toStringList(): MutableList<String> {
	val arr = ArrayList<String>()
	for (i in this) {
		arr.add(i.absolutePath)
	}
	return arr
}

val PATH_SEPARATOR = System.getProperty("path.separator")