@file:Suppress("UnstableApiUsage", "UNUSED_PARAMETER", "unused")

package com.acornui.build.plugins.tasks

import com.acornui.build.plugins.tasks.fileprocessor.BitmapFontsFileProcessor
import com.acornui.build.plugins.tasks.fileprocessor.CopyFileProcessor
import com.acornui.build.plugins.tasks.fileprocessor.PackTexturesFileProcessor
import com.acornui.build.plugins.tasks.fileprocessor.TextFileProcessor
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

open class AcornUiResourceProcessorTask @javax.inject.Inject constructor(objects: ObjectFactory) : DefaultTask() {

	@Incremental
	@PathSensitive(PathSensitivity.RELATIVE)
	@InputFiles
	val sources: ConfigurableFileCollection = objects.fileCollection()

	/**
	 * Adds the given file tree to the list of sources.
	 */
	fun from(tree: FileTree): AcornUiResourceProcessorTask {
		sources.from(tree)
		return this
	}

	fun from(source: File): AcornUiResourceProcessorTask {
		sources.from(source)
		return this
	}

	fun from(sources: Iterable<File>): AcornUiResourceProcessorTask {
		this.sources.from(sources)
		return this
	}

	fun into(directory: File?) {
		outputDir.set(directory)
	}

	fun into(directory: Directory?) {
		outputDir.set(directory)
	}

	@get:OutputDirectory
	val outputDir: DirectoryProperty = objects.directoryProperty()

	@Nested
	var fileProcessors: List<FileChangeProcessor> = arrayListOf(PackTexturesFileProcessor(), BitmapFontsFileProcessor(), TextFileProcessor(), CopyFileProcessor())

//	private val directoryProcessors: Map<String, DirectoryProcessor> = mapOf("_unpacked" to ::packAcornAssets, "_unprocessedFonts" to ::processBitmapFonts)
	// [\w-_]+[-_]([a-z]{2}(?:-[A-Z]{2})?)\.properties

	@TaskAction
	fun execute(inputChanges: InputChanges) {
		val outputDir = outputDir.asFile.get()
		val processorChanges = fileProcessors.map { ArrayList<FileChange>() }

		inputChanges.getFileChanges(sources).forEach { change ->
			for ((index, fileProcessor) in fileProcessors.withIndex()) {
				if (fileProcessor.accepts(change, outputDir, this)) {
					processorChanges[index].add(change)
					break
				}
			}
		}
		for ((index, fileProcessor) in fileProcessors.withIndex()) {
			fileProcessor.process(processorChanges[index], outputDir, this)
		}
	}
}

