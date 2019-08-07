@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.tasks

import com.acornui.build.plugins.util.packAssets
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

open class AcornUiResourceProcessorTask @javax.inject.Inject constructor(private val objects: ObjectFactory) : DefaultTask() {

	private val fileTrees = mutableListOf<FileTree>()

	@get:SkipWhenEmpty
	@get:Incremental
	@get:PathSensitive(PathSensitivity.RELATIVE)
	@get:InputFiles
	val sources: FileCollection
		get() = fileTrees.reduce(FileTree::plus)

	/**
	 * Adds the given file tree to the list of sources.
	 */
	fun from(tree: FileTree): AcornUiResourceProcessorTask {
		fileTrees.add(tree)
		return this
	}

	fun from(source: File): AcornUiResourceProcessorTask {
		from(objects.fileCollection().from(source).asFileTree)
		return this
	}

	fun from(sources: Iterable<File>): AcornUiResourceProcessorTask {
		sources.map { from(it) }
		return this
	}

	fun into(directory: File?) {
		outputDir.set(directory)
	}

	fun into(directory: Directory?) {
		outputDir.set(directory)
	}

	@get:OutputDirectory
	val outputDir = objects.directoryProperty()

	@TaskAction
	fun execute(inputChanges: InputChanges) {

        val directoriesToProcess = mutableMapOf<String, MutableSet<DirectoryToProcessEntry>>()
        directoriesToProcess += processors.map { it.key to mutableSetOf() }

		inputChanges.getFileChanges(sources).forEach { change ->
			val relPath = change.normalizedPath
			if (relPath.isEmpty()) return@forEach
			val sourceFile = change.file
			val targetFile = outputDir.file(relPath).get().asFile

            val foundSpecialFolder = change.file.findSpecialFolder()
			if (foundSpecialFolder != null) {
				val specialFolderToFilePath = change.file.relativeTo(foundSpecialFolder.second).path
				val specialFolderDest = outputDir.file(relPath.removeSuffix(specialFolderToFilePath)).get().asFile

				directoriesToProcess[foundSpecialFolder.first]!!.add(DirectoryToProcessEntry(
						foundSpecialFolder.second,
						specialFolderDest,
						removed = specialFolderToFilePath.isEmpty() && change.changeType == ChangeType.REMOVED
				))
			} else {
				if (change.changeType == ChangeType.REMOVED || !sourceFile.exists()) {
					if (targetFile.exists())
						if (targetFile.isDirectory) targetFile.deleteRecursively() else targetFile.delete()
				} else {
					if (change.fileType != FileType.DIRECTORY) {
						sourceFile.parentFile.mkdirs()
						sourceFile.copyTo(targetFile, overwrite = true)
					}
				}
			}
		}


        processors.forEach {
            (suffix, processor) ->
            val directoryToProcess = directoriesToProcess[suffix]!!
            if (directoryToProcess.isNotEmpty()) {
                processor.invoke(this, suffix, directoryToProcess)
            }
        }
	}

    private fun File.findSpecialFolder(): Pair<String, File>? {
        val keys = processors.keys
        var p: File? = this
        while (p != null) {
            if (p.isDirectory) {
                val found = keys.find { p!!.name.endsWith(it) }
                if (found != null)
                    return found to p
            }
            p = p.parentFile
        }
        return null
    }

	companion object {

		var processors: Map<String, DirectoryProcessor> = mapOf("_unpacked" to ::packAssets)

		private val packedExtensions = arrayOf("json", "png")

		fun packAssets(task: Task, suffix: String, entries: Iterable<DirectoryToProcessEntry>) {
			entries.forEach {
				if (it.removed) {
					task.logger.lifecycle("Removing assets: " + it.sourceDir.path)
					val name = it.sourceDir.name.removeSuffix(suffix)
					it.destinationDir.parentFile.listFiles()?.forEach { child ->
						if (child.name.startsWith(name) && packedExtensions.contains(child.extension.toLowerCase()))
							child.delete()
					}
				} else {
					task.logger.lifecycle("Packing assets: " + it.sourceDir.path)
					packAssets(it.sourceDir, it.destinationDir.parentFile, suffix)
				}
			}
		}
	}
}

data class DirectoryToProcessEntry(
        val sourceDir: File,
        val destinationDir: File,

        /**
         * True if the directory has been removed, instead of created or modified.
         */
        val removed: Boolean
)

typealias DirectoryProcessor = (task: Task, suffix: String, entries: Iterable<DirectoryToProcessEntry>) -> Unit