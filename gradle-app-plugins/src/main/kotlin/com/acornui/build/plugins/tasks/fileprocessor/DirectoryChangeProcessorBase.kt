/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.build.plugins.tasks.fileprocessor

import com.acornui.build.plugins.tasks.FileChangeProcessor
import org.gradle.api.Task
import org.gradle.work.FileChange
import java.io.File


/**
 * A base class for a file change processor meant to work on whole directories, not files.
 */
abstract class DirectoryChangeProcessorBase : FileChangeProcessor {

	/**
	 * If this regex matches a parent directory name of a changed file, the processor will be accepted.
	 * @see accepts
	 */
	protected abstract val directoryRegex: Regex

	override fun accepts(change: FileChange, outputDir: File, task: Task): Boolean {
		return change.file.directoryToProcess != null
	}

	override fun process(changes: List<FileChange>, outputDir: File, task: Task) {
		val directoriesToProcess = HashMap<File, File>()
		for (change in changes) {
			val sourceDirectory = change.file.directoryToProcess!!
			val directoryToFilePath = change.file.relativeTo(sourceDirectory).invariantSeparatorsPath
			val dest = outputDir.resolve(change.normalizedPath.removeSuffix(directoryToFilePath))
			directoriesToProcess[sourceDirectory] = dest
		}
		for (entry in directoriesToProcess) {
			process(entry.key, entry.value, task)
		}
	}

	abstract fun process(sourceDir: File, destinationDir: File, task: Task)

	private val File.directoryToProcess: File?
		get() {
			val directoryRegex =  directoryRegex
			var p: File? = this
			while (p != null) {
				if (p.isDirectory && directoryRegex.matches(p.name))
					return p
				p = p.parentFile
			}
			return null
		}
}

fun File.removeSuffix(suffix: String): File = parentFile.resolve(name.removeSuffix(suffix))