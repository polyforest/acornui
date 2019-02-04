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

package com.acornui.build.util

import java.io.*
import java.util.jar.*

/**
 * Utilities for easily working with jar files.
 * Created by nbilyk on 9/3/2015.
 */
object JarUtil {

	private val defaultManifest = Manifest()

	init {
		defaultManifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
	}

	val defaultInsertionFilter: (file: File, root: File) -> String? = {
		file, root ->
		file.toRelativeString(root).replace("\\", "/")
	}

	fun createJar(file: File, out: File, filter: (file: File, root: File) -> String? = defaultInsertionFilter, manifest: Manifest? = defaultManifest) {
		createJar(arrayOf(file), out, filter, manifest)
	}

	fun createJar(files: Array<File>, out: File, filter: (file: File, root: File) -> String? = defaultInsertionFilter, manifest: Manifest? = defaultManifest) {
		out.parentFile!!.mkdirs()
		val target = if (manifest == null) {
			JarOutputStream(FileOutputStream(out))
		} else {
			JarOutputStream(FileOutputStream(out), manifest)
		}
		for (i in files) {
			addToJar(i, i, target, filter)
		}
		target.close()
	}

	private fun addToJar(source: File, root: File, target: JarOutputStream, filter: (file: File, root: File) -> String? = defaultInsertionFilter) {
		if (!source.exists()) return
		var name: String = filter(source, root) ?: return
		if (source.isDirectory) {
			if (!name.isEmpty()) {
				if (!name.endsWith("/")) name += "/"
				val entry = JarEntry(name)
				entry.time = source.lastModified()
				target.putNextEntry(entry)
				target.closeEntry()
			}
			for (nestedFile in source.listFiles()!!) {
				addToJar(nestedFile, root, target)
			}
		} else {
			val entry = JarEntry(name)
			entry.time = source.lastModified()
			target.putNextEntry(entry)
			val inStream = BufferedInputStream(FileInputStream(source))

			val buffer = ByteArray(1024)
			while (true) {
				val count = inStream.read(buffer)
				if (count == -1) break
				target.write(buffer, 0, count)
			}
			target.closeEntry()
			inStream.close()
		}

	}

	private val defaultExtractionFilter = {
		entry: JarEntry ->
		val name = entry.name
		if (name.startsWith("MANIFEST")) null
		else name
	}

	/**
	 * Extracts files passing a filter from a jar file to the given destination.
	 * @param jar The jar to explode
	 * @param destination The destination folder to copy the jar files to.
	 * @param filter A filter returning the name of the new file, or null if the jar entry shouldn't be copied.
	 */
	fun extractFromJar(jar: JarFile, destination: File, filter: (entry: JarEntry) -> String? = defaultExtractionFilter, force: Boolean = false) {
		if (destination.exists() && !destination.isDirectory) throw IllegalArgumentException("$destination is not a directory.")

		val enumEntries = jar.entries()
		while (enumEntries.hasMoreElements()) {
			val next = enumEntries.nextElement()
			val name = filter(next) ?: continue

			val newFile = File("" + destination.absolutePath + File.separator + name)
			if (next.isDirectory) {
				newFile.mkdirs()
			} else {
				newFile.parentFile!!.mkdirs()
				copyJarEntry(jar, next, newFile, force)
			}
		}
	}

	/**
	 * Copies a single jar entry from the given [jar] file to the destination [newFile].
	 */
	fun copyJarEntry(jar: JarFile, entry: JarEntry, newFile: File, force: Boolean) {
		if (!force && (newFile.exists() && entry.time <= newFile.lastModified())) return
		val inputStream = jar.getInputStream(entry) // get the input stream
		val fileOutputStream = FileOutputStream(newFile)
		while (true) {
			val byte = inputStream.read()
			if (byte < 0) break
			fileOutputStream.write(byte)
		}
		fileOutputStream.close()
		inputStream.close()
	}

	/**
	 * Returns the jar for the currently executing code.
	 */
	fun getCurrentJar(): JarFile {
		return JarFile(javaClass.protectionDomain.codeSource.location.toURI().path)
	}

}

class LogStreamReader(inputStream: InputStream, private val printStream: PrintStream = System.out) : Runnable {

	private val reader = BufferedReader(InputStreamReader(inputStream))

	override fun run() {
		try {
			var line = reader.readLine()
			while (line != null) {
				printStream.println(line)
				line = reader.readLine()
			}
			reader.close()
		} catch (e: IOException) {
			e.printStackTrace()
		}

	}
}