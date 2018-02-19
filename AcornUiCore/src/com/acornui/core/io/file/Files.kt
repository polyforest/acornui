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

package com.acornui.core.io.file

import com.acornui.collection.pop
import com.acornui.core.di.DKey
import com.acornui.io.file.FilesManifest
import com.acornui.core.replace2
import com.acornui.core.split2


interface Files {

	fun getFile(path: String): FileEntry?

	fun getDir(path: String): Directory?

	companion object : DKey<Files>
}

/**
 * Files allows you to check if a file or directory exists without making a request.
 * Files depends on manifest data provided in the application bootstrap. This data should be auto-generated
 * by the AcornAssets task.
 */
class FilesImpl(manifest: FilesManifest) : Files {

	private val map = HashMap<String, FileEntry>()

	private val rootDir = Directory("", null, HashMap(), HashMap())

	init {
		for (file in manifest.files) {
			// Create all directories leading up to the new file entry.
			val path = file.path
			val pathSplit = path.split("/")
			var p = rootDir
			for (i in 0..pathSplit.lastIndex - 1) {
				val part = pathSplit[i]
				var dir = p.directories[part]
				if (dir == null) {
					val newPath = if (p == rootDir) part else p.path + "/" + part
					dir = Directory(newPath, p, HashMap(), HashMap())
					(p.directories as MutableMap).put(part, dir)
				}
				p = dir
			}
			val fileEntry = FileEntry(file.path, file.modified, file.size, p)
			map.put(fileEntry.path, fileEntry)

			// Add the file entry to the directory.
			(p.files as MutableMap).put(pathSplit.last(), fileEntry)
		}
	}

	override fun getFile(path: String): FileEntry? {
		val entry = map[path.replace2('\\', '/')]
		return entry
	}

	override fun getDir(path: String): Directory? {
		if (path == "") return rootDir
		var p: Directory? = rootDir
		val pathSplit = path.replace2('\\', '/').split2('/')
		for (part in pathSplit) {
			if (p == null) return null
			p = p.getDir(part)
		}
		return p
	}
}

class FileEntry(
		val path: String,
		val modified: Long,
		val size: Long,
		val parent: Directory
) : Comparable<FileEntry> {

	fun siblingFile(name: String): FileEntry? {
		return parent.getFile(name)
	}

	fun siblingDir(name: String): Directory? {
		return parent.getDir(name)
	}

	val name: String
		get() {
			return path.substringAfterLast('/')
		}

	val nameNoExtension: String
		get() {
			return path.substringAfterLast('/').substringBeforeLast('.')
		}

	val extension: String
		get() {
			return path.substringAfterLast('.')
		}

	fun hasExtension(extension: String): Boolean {
		return this.extension.equals(extension, ignoreCase = true)
	}

	/**
	 * Calculates the number of directories deep this file entry is.
	 */
	val depth: Int by lazy {
		var count = -1
		var index = -1
		do {
			count++
			index = path.indexOf('/', index + 1)
		} while (index != -1)
		count
	}

	override fun compareTo(other: FileEntry): Int {
		if (depth == other.depth) {
			return path.compareTo(other.path)
		} else {
			return depth.compareTo(other.depth)
		}
	}
}

class Directory(
		val path: String,
		val parent: Directory?,
		val directories: Map<String, Directory>,
		val files: Map<String, FileEntry>
) : Comparable<Directory> {

	val name: String
		get() {
			return path.substringAfterLast('/')
		}

	val depth: Int by lazy {
		if (parent == null) 0 else parent.depth + 1
	}

	/**
	 * Returns the total number of files in this directory (recursive).
	 */
	val totalFiles: Int by lazy {
		var c = files.size
		for (directory in directories.values) {
			c += directory.totalFiles
		}
		c
	}

	fun getFile(name: String): FileEntry? {
		return files[name]
	}

	fun getDir(name: String): Directory? {
		return directories[name]
	}

	fun walkFilesTopDown(maxDepth: Int = 100): Sequence<FileEntry> {
		return FilesTopDownSequence(this, maxDepth)
	}

	fun walkDirectoriesTopDown(maxDepth: Int = 100): Sequence<Directory> {
		return DirectoriesTopDownSequence(this, maxDepth)
	}

	/**
	 * Recursively invokes a callback on all descendant files in this directory.
	 * @param callback Invoked once for each file. If the callback returns false, the iteration will immediately stop.
	 * @param maxDepth The maximum depth to traverse. A maxDepth of 0 will not follow any subdirectories of this directory.
	 */
	@Deprecated("use walkTopDown")
	fun mapFiles(callback: (FileEntry) -> Boolean, maxDepth: Int = 100) {
		val openList = ArrayList<Directory>()
		val depths = ArrayList<Int>()
		openList.add(this)
		depths.add(0)

		while (openList.isNotEmpty()) {
			val next = openList.pop()
			val depth = depths.pop()
			for (file in next.files.values.sorted()) {
				val shouldContinue = callback(file)
				if (!shouldContinue) return
			}
			if (depth < maxDepth) {
				for (i in next.directories.values.sorted()) {
					openList.add(i)
					depths.add(depth + 1)
				}
			}
		}
	}

	/**
	 * Recursively invokes a callback on all descendant directories in this directory.
	 * @param callback Invoked once for each directory (Not including this directory). If the callback returns false, the iteration will immediately stop.
	 * @param maxDepth The maximum depth to traverse. A maxDepth of 0 will not follow any subdirectories of this directory.
	 */
	@Deprecated("use walkTopDown")
	fun mapDirectories(callback: (Directory) -> Boolean, maxDepth: Int = 100) {
		val openList = ArrayList<Directory>()
		val depths = ArrayList<Int>()
		openList.add(this)
		depths.add(0)

		while (openList.isNotEmpty()) {
			val next = openList.pop()
			val depth = depths.pop()
			for (dir in next.directories.values.sorted()) {
				val shouldContinue = callback(dir)
				if (!shouldContinue) return

				if (depth < maxDepth) {
					openList.add(dir)
					depths.add(depth + 1)
				}
			}
		}
	}

	override fun compareTo(other: Directory): Int {
		return if (depth == other.depth) {
			path.compareTo(other.path)
		} else {
			depth.compareTo(other.depth)
		}
	}

	fun relativePath(file: FileEntry): String {
		return file.path.substringAfter(path + "/", path)
	}
}

private class FilesTopDownSequence(private val root: Directory, private val maxDepth: Int) : Sequence<FileEntry> {

	override fun iterator(): Iterator<FileEntry> = object : Iterator<FileEntry> {

		private val openList = ArrayList<Directory>()
		private val files = ArrayList<FileEntry>()
		private var fileIndex = 0

		init {
			if (root.totalFiles > 0) {
				openList.add(root)
				step()
			}
		}

		override fun next(): FileEntry {
			val ret = files[fileIndex]
			step()
			return ret
		}

		private fun step() {
			if (fileIndex < files.size) {
				fileIndex++
			} else {
				fileIndex = 0
				files.clear()
				while (true) {
					val newParent = openList.pop()
					if (newParent.depth <= maxDepth && newParent.totalFiles > 0) {
						openList.addAll(newParent.directories.values.sorted())
					}
					if (newParent.files.isNotEmpty()) {
						files.addAll(newParent.files.values.sorted())
						break
					}
				}
			}
		}

		override fun hasNext(): Boolean {
			return fileIndex < files.size || openList.isNotEmpty()
		}
	}
}

private class DirectoriesTopDownSequence(private val root: Directory, private val maxDepth: Int) : Sequence<Directory> {

	override fun iterator(): Iterator<Directory> = object : Iterator<Directory> {

		private val openList = arrayListOf(root)

		override fun next(): Directory {
			val next = openList.pop()
			if (next.depth <= maxDepth) {
				openList.addAll(next.directories.values.sorted())
			}
			return next
		}

		override fun hasNext(): Boolean {
			return openList.isNotEmpty()
		}
	}
}