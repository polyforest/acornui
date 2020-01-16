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

@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.tasks

import org.gradle.api.Task
import org.gradle.api.file.FileType
import org.gradle.work.ChangeType
import org.gradle.work.FileChange
import java.io.File

interface FileChangeProcessor {

	/**
	 * @return Returns true if this processor should handle processing the given file.
	 */
	fun accepts(change: FileChange, outputDir: File, task: Task): Boolean

	/**
	 * Processes the given changed files.
	 */
	fun process(changes: List<FileChange>, outputDir: File, task: Task)
}

/**
 * A base class for a file change processor meant to work on files, not directories.
 */
abstract class FileChangeProcessorBase : FileChangeProcessor {

	override fun process(changes: List<FileChange>, outputDir: File, task: Task) {
		for (change in changes) {
			val relPath = change.normalizedPath
			if (relPath.isEmpty()) continue
			val sourceFile = change.file
			val targetFile = outputDir.resolve(relPath)

			if (change.changeType == ChangeType.REMOVED || !sourceFile.exists()) {
				if (targetFile.exists())
					targetFile.deleteRecursively()
			} else {
				if (change.fileType != FileType.DIRECTORY) {
					targetFile.parentFile.mkdirs()
					process(sourceFile, targetFile, task)
				}
			}
		}
	}

	abstract fun process(sourceFile: File, targetFile: File, task: Task)
}
