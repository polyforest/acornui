/*
 * Copyright 2018 Nicholas Bilyk
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

import java.io.File

import kotlin.text.Regex
println("ARGS: " + args.joinToString())
val argMap = ArgumentMap(args)

if (!argMap.exists("name")) {
	println("Usage: -name=[name] -destination=[destination] -template=(optional) -templatesDir=(optional)")
	System.exit(-1)
}

val name = argMap.get("name")!!
val template = argMap.get("template", "HelloWorld")
val destination = File(argMap.get("destination", "d", ""), name)

if (destination.exists()) {
	System.err.println("Destination already exists.")
	System.exit(-1)
}
println("Destination ${destination.absolutePath}")

// Find the template:
val templatesDirStr = argMap.get("templatesDir", "../AcornUiDemos")
var templatesDir = File(templatesDirStr)
if (!templatesDir.exists()) templatesDir = File(findAcornDir(), argMap.get("templatesDir", "../AcornUiDemos"))
println("templatesDir: ${templatesDir.absolutePath}")
if (!templatesDir.exists()) throw Exception("Could not find templates directory: $templatesDirStr")
val templateDir: File = File(templatesDir, template)
println("Template used: ${templateDir.absolutePath}")
if (!templateDir.exists()) throw Exception("Could not find template: $template")

templateDir.copyRecursively(destination)
val replacements = ArrayList<Pair<String, String>>()
replacements.add(Pair(template, name))
replacements.add(Pair(template.toLowerCase(), name.toLowerCase()))
replacements.add(Pair(template.toUpperCase(), name.toUpperCase()))
replacements.add(Pair(template.toUnderscoreCase(), name.toUnderscoreCase()))
replacements.add(Pair(template.toUnderscoreCase().toUpperCase(), name.toUnderscoreCase().toUpperCase()))
replacements.add(Pair(template.toFirstLowerCase(), name.toFirstLowerCase()))

val txtExtensions = arrayOf("name", "txt", "xml", "kt", "kts", "java", "json", "iml", "js", "html", "php", "css")
val excludes = arrayOf("dist", "out", "www", "acornlibs", "workspace.xml", ".git", ".svn")

for (i in destination.walkBottomUp()) {
	if (excludes.contains(i.name)) {
		i.deleteRecursively()
		continue
	}
	if (txtExtensions.contains(i.extension.toLowerCase())) {
		val text = i.readText()
		val newText = text.replaceList(replacements)
		i.writeText(newText)
	}
	val newName = i.name.replaceList(replacements)
	i.renameTo(File(i.parentFile, newName))
}

fun findAcornDir(): File {
	return File("..")
}

fun String.toUnderscoreCase(): String {
	return replace(Regex("([a-z])([A-Z]+)"), "$1_$2").toLowerCase()
}

fun String.toFirstLowerCase(): String {
	return this[0].toLowerCase() + substring(1)
}

fun String.replaceList(replacements: ArrayList<Pair<String, String>>): String {
	var str = this
	for (i in replacements) {
		str = str.replace(i.first, i.second)
	}
	return str
}

object NewProject

/**
 * Represents the argument array (such as it is used in the command line) in a way that makes it easy to retrieve
 * specific properties.
 */
class ArgumentMap(private val args: Array<String>) {

	fun exists(name: String): Boolean {
		val str = name.toLowerCase()
		val str2 = "$str="
		val found = args.firstOrNull { it.toLowerCase() == str || it.toLowerCase().startsWith(str2) }
		return found != null
	}

	fun get(name: String): String? {
		val str = name.toLowerCase() + "="
		val index = args.indexOfFirst { it.toLowerCase().startsWith(str) }
		if (index == -1) return null
		return args[index].substring(name.length + 1)
	}

	fun get(name: String, alias: String, default: String): String {
		if (exists(name)) return get(name, default)
		return get(alias, default)
	}

	fun get(name: String, default: String): String {
		val str = name.toLowerCase() + "="
		val index = args.indexOfFirst { it.toLowerCase().startsWith(str) }
		if (index == -1) return default
		return args[index].substring(name.length + 1)
	}

}