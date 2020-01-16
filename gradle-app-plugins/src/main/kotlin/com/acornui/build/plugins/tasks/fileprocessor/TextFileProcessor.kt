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

import com.acornui.build.plugins.tasks.FileChangeProcessorBase
import org.gradle.api.Task
import org.gradle.kotlin.dsl.extra
import org.gradle.work.FileChange
import java.io.File

class TextFileProcessor : FileChangeProcessorBase() {

	/**
	 * The file extensions to be considered text files and transformed with [textFileProcessors]. (lowercase)
	 */
	var textFilePatterns = listOf("asp", "aspx", "cfm", "cshtml", "css", "go", "htm", "html", "json", "jsp", "jspx",
			"php", "php3", "php4", "phtml", "rhtml", "txt", "properties")

	/**
	 * A list of processors to mutates text files.
	 */
	var textFileProcessors: List<TextFileContentsProcessor> = listOf(TokenReplacementFileProcessor())

	override fun accepts(change: FileChange, outputDir: File, task: Task): Boolean {
		return (textFileProcessors.isNotEmpty() && textFilePatterns.contains(change.file.extension.toLowerCase()))
	}

	override fun process(sourceFile: File, targetFile: File, task: Task) {
		val properties = (task.project.extra.properties + task.extra.properties).mapValues { it.value.toString() }
		var str = sourceFile.readText()
		for (fileProcessor in textFileProcessors) {
			str = fileProcessor.process(targetFile.path, str, properties)
		}
		targetFile.writeText(str)
	}
}