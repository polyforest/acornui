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

package com.acornui.skins

import com.acornui.build.plugins.tasks.AcornUiResourceProcessorTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileType
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.work.ChangeType
import org.gradle.work.FileChange
import org.gradle.work.InputChanges
import java.io.File

/**
 * Delegates the inputs to the AcornUiResourceProcessorTask.
 * Expects:
 *   arg0 - output directory
 *   arg1 - changes in the form of: "absolutePath0,changeType0,fileType0,normalizedPathN;absolutePathN,changeTypeN,fileTypeN,normalizedPathN"
 */
fun main(args: Array<String>) {
	val project = ProjectBuilder.builder().build()
	val task = project.tasks.create("processResources", AcornUiResourceProcessorTask::class.java) {
		it.into(File(args[0]))
	}
	val changes: MutableList<FileChange> = args[1].split(";").map {
		val attrs = it.split(",")
		FileChangeImpl(File(attrs[0]), ChangeType.valueOf(attrs[1]), FileType.valueOf(attrs[2]), attrs[3])
	}.toMutableList()
	task.execute(FixedInputChanges(changes))
}

private class FixedInputChanges(private val fileChanges: MutableList<FileChange>) : InputChanges {
	override fun getFileChanges(parameter: FileCollection): MutableIterable<FileChange> = fileChanges
	override fun getFileChanges(parameter: Provider<out FileSystemLocation>): MutableIterable<FileChange> = fileChanges
	override fun isIncremental() = true
}

private class FileChangeImpl(private val file: File, private val changeType: ChangeType, private val fileType: FileType, private val normalizedPath: String): FileChange {
	override fun getFile(): File = file
	override fun getChangeType(): ChangeType = changeType
	override fun getFileType(): FileType = fileType
	override fun getNormalizedPath(): String = normalizedPath
}