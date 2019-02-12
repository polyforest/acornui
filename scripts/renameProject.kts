/*
 * Copyright 2017 Nicholas Bilyk
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

/**
 * newProject.kts Will copy a folder, and will do a series of filename and token replacements to create a renamed
 * project.
 */


import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

import kotlin.text.Regex

val txtExtensions = arrayOf("name", "txt", "xml", "kt", "kts", "java", "json", "iml", "js", "html", "php", "css")

println("Rename project ${args.joinToString(", ")}")
if (args.size != 2) error("Usage: path/sourceFolder path/destFolder")

val source = args[0]
val destination = args[1]
val sourceDir = File(source)
val destinationDir = File(destination)

if (!sourceDir.exists()) error("source '$source' does not exist.")
if (destinationDir.exists()) error("Destination already exists.")
println("Copying to destination: ${destinationDir.absolutePath}")

val templateName: String = sourceDir.name.toCamelCase()
val newProjectName: String = destinationDir.name.toCamelCase()
val replacements = ArrayList<Pair<String, String>>()
replacements.add(Pair(templateName, newProjectName))
replacements.add(Pair(templateName.toLowerCase(), newProjectName.toLowerCase()))
replacements.add(Pair(templateName.toUpperCase(), newProjectName.toUpperCase()))
replacements.add(Pair(templateName.toUnderscoreCase(), newProjectName.toUnderscoreCase()))
replacements.add(Pair(templateName.toUnderscoreCase().toUpperCase(), newProjectName.toUnderscoreCase().toUpperCase()))
replacements.add(Pair(templateName.toFirstLowerCase(), newProjectName.toFirstLowerCase()))

val ignoredFiles = HashMap<File, Boolean>()
ignoredFiles[File(sourceDir, ".git")] = true
for (line in "git status --ignored -s".runCommand(sourceDir)!!.lines()) {
	if (line.startsWith("!! "))
		ignoredFiles[File(sourceDir, line.substring(3))] = true // Cut off the preceding "!! "
}

val openList = ArrayList<String>()
openList.add("")
while (openList.isNotEmpty()) {
	val next = openList.removeAt(0)
	val src = File(sourceDir, next)
	if (ignoredFiles.contains(src))
		continue

	if (src.isDirectory) {
		for (child in src.listFiles()) {
			openList.add(child.toRelativeString(sourceDir))
		}
	} else {
		val newName = next.replaceList(replacements)
		val newFile = File(destination, newName)
		newFile.parentFile.mkdirs()

		if (txtExtensions.contains(src.extension.toLowerCase())) {
			val text = src.readText()
			val newText = text.replaceList(replacements)
			newFile.writeText(newText)
		} else {
			src.copyTo(newFile)
		}
	}
}

fun String.toUnderscoreCase(): String {
	return replace(Regex("([a-z])([A-Z]+)"), "$1_$2").toLowerCase()
}

fun String.toCamelCase(): String {
	return split("-").map { it[0].toUpperCase() + it.substring(1) }.joinToString("")
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

fun error(msg: String) {
	println(msg)
	System.exit(-1)
}

fun String.runCommand(workingDir: File = File(".")): String? {
	return try {
		val parts = this.split("\\s".toRegex())
		val proc = ProcessBuilder(*parts.toTypedArray())
				.directory(workingDir)
				.redirectOutput(ProcessBuilder.Redirect.PIPE)
				.redirectError(ProcessBuilder.Redirect.PIPE)
				.start()

		proc.waitFor(60, TimeUnit.MINUTES)
		proc.inputStream.bufferedReader().readText()
	} catch(e: IOException) {
		e.printStackTrace()
		null
	}
}