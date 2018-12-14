package com.acornui.util

import com.acornui.build.ArgumentMap
import java.io.File


fun main(args: Array<String>) {
	val argMap = ArgumentMap(args)

	if (!argMap.exists("from")) {
		println("Usage: -to=[to] -from=[from]")
		System.exit(-1)
	}

	val to = argMap.get("to")!!
	val from = argMap.get("from")!!


	val replacements = arrayListOf(Pair(from, to))

	val txtExtensions = arrayOf("name", "txt", "xml", "kt", "java", "json", "iml", "remove", "js", "html", "css")
	val excludes = arrayOf("dist", "www", "acornlibs", "workspace.xml")

	for (i in File(".").walkBottomUp()) {
		if (excludes.contains(i.name)) {
			i.deleteRecursively()
			continue
		}
		if (txtExtensions.contains(i.extension)) {
			val text = i.readText()
			val newText = text.replaceList(replacements)
			if (text != newText) i.writeText(newText)
		}
		val newName = i.name.replaceList(replacements)
		i.renameTo(File(i.parentFile, newName))
	}
}


private fun String.replaceList(replacements: ArrayList<Pair<String, String>>): String {
	var str = this
	for (i in replacements) {
		str = str.replace(i.first, i.second)
	}
	return str
}

object RenameHelper {}