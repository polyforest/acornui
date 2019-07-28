@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.tasks

import com.acornui.build.plugins.util.packAssets
import org.gradle.api.DefaultTask
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


    /**
     * Folders ending in this suffix will be packed.
     * Set to null for no packing.
     */
    @get:Input
    var unpackedSuffix = "_unpacked"

    @get:OutputDirectory
    val outputDir = objects.directoryProperty()

    private val packedExtensions = arrayOf("json", "png")

    @TaskAction
    fun execute(inputChanges: InputChanges) {

        val unpackedSuffix = unpackedSuffix
        val directoriesToPack = mutableSetOf<Pair<File, File>>() // List of source folder to pack destination.

        inputChanges.getFileChanges(sources).forEach { change ->
            val relPath = change.normalizedPath
            if (relPath.isEmpty()) return@forEach

            val sourceFile = change.file
            val targetFile = outputDir.file(relPath).get().asFile

            if (change.changeType == ChangeType.REMOVED || !sourceFile.exists()) {
                if (targetFile.exists())
                    if (targetFile.isDirectory) targetFile.deleteRecursively() else targetFile.delete()

                if (sourceFile.isDirectory && sourceFile.name.endsWith(unpackedSuffix)) {
                    val name = sourceFile.name.removeSuffix(unpackedSuffix)
                    targetFile.parentFile.listFiles()?.forEach {
                        if (it.name.startsWith(name) && packedExtensions.contains(it.extension.toLowerCase()))
                            it.delete()
                    }
                }
            } else {
                if (sourceFile.parentFile.name.endsWith(unpackedSuffix)) {
                    directoriesToPack.add(sourceFile.parentFile to targetFile.parentFile.parentFile)
                } else {
                    if (change.fileType != FileType.DIRECTORY) {
                        sourceFile.parentFile.mkdirs()
                        sourceFile.copyTo(targetFile, overwrite = true)
                    }
                }
            }
        }

        directoriesToPack.forEach { (srcDir, destDir) ->
            logger.lifecycle("Packing assets: " + srcDir.path)
            packAssets(srcDir, destDir, unpackedSuffix)
        }
    }
}