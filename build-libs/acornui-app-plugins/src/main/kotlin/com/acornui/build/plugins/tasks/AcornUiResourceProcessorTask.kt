@file:Suppress("UnstableApiUsage", "UNUSED_PARAMETER", "unused")

package com.acornui.build.plugins.tasks

import com.acornui.build.plugins.util.createRuntimeKotlinClasspath
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.process.internal.ExecActionFactory
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import javax.inject.Inject

lateinit var packTexturesClasspath: FileCollection

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
	val outputDir: DirectoryProperty = objects.directoryProperty()

	private val processors: Map<String, DirectoryProcessor> = mapOf("_unpacked" to ::packAssets, "_unprocessedFonts" to ::processFonts)

	@TaskAction
	fun execute(inputChanges: InputChanges) {

		val directoriesToProcess = mutableMapOf<String, MutableSet<DirectoryToProcessEntry>>()
		directoriesToProcess += processors.map { it.key to mutableSetOf() }

		inputChanges.getFileChanges(sources).forEach { change ->
			val relPath = change.normalizedPath
			if (relPath.isEmpty()) return@forEach
			val sourceFile = change.file
			val targetFile = outputDir.file(relPath).get().asFile

			val found = change.file.findSpecialFolder()
			if (found != null) {
				val (suffix, foundSpecialFolder) = found
				val specialFolderToFilePath = sourceFile.relativeTo(foundSpecialFolder).invariantSeparatorsPath
				val specialFolderDest = outputDir.file(relPath.removeSuffix(specialFolderToFilePath)).get().asFile
				val dest = specialFolderDest.parentFile.resolve(specialFolderDest.name.removeSuffix(suffix))

				directoriesToProcess[suffix]!!.add(DirectoryToProcessEntry(
						foundSpecialFolder,
						dest
				))
			} else {
				if (change.changeType == ChangeType.REMOVED || !sourceFile.exists()) {
					if (targetFile.exists())
						targetFile.deleteRecursively()
				} else {
					if (change.fileType != FileType.DIRECTORY) {
						sourceFile.parentFile.mkdirs()
						sourceFile.copyTo(targetFile, overwrite = true)
					}
				}
			}
		}


		processors.forEach { (suffix, processor) ->
			val directoryToProcess = directoriesToProcess[suffix]!!
			if (directoryToProcess.isNotEmpty()) {
				processor.invoke(suffix, directoryToProcess)
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

	@Inject
	protected open fun getExecActionFactory(): ExecActionFactory {
		throw UnsupportedOperationException()
	}

	private val packedExtensions = arrayOf("json", "png")

	private fun packAssets(suffix: String, entries: Iterable<DirectoryToProcessEntry>) {
		entries.forEach {
			if (it.sourceDir.exists()) {
				logger.lifecycle("Packing assets: " + it.sourceDir.path)

				getExecActionFactory().newJavaExecAction().apply {
					main = "com.acornui.texturepacker.PackAssetsKt"
					args = listOf(it.sourceDir.absolutePath, it.destinationDir.parentFile.absolutePath, suffix)
					classpath = packTexturesClasspath
					maxHeapSize = "3g"
					execute()
				}

			} else {
				logger.lifecycle("Removing assets: " + it.sourceDir.path)
				val name = it.sourceDir.name.removeSuffix(suffix)
				it.destinationDir.parentFile.listFiles()?.forEach { child ->
					if (child.name.startsWith(name) && packedExtensions.contains(child.extension.toLowerCase()))
						child.delete()
				}
			}
		}
	}

	private fun processFonts(suffix: String, entries: Iterable<DirectoryToProcessEntry>) {
		entries.forEach {
			it.destinationDir.deleteRecursively()
			if (it.sourceDir.exists()) {
				logger.lifecycle("Processing fonts: " + it.sourceDir.path)
				getExecActionFactory().newJavaExecAction().apply {
					main = "com.acornui.font.ProcessFontsKt"
					args = listOf(it.sourceDir.absolutePath, it.destinationDir.absolutePath)
					classpath = project.configurations.getByName("bitmapFontGenerator")
					maxHeapSize = "3g"
					execute()
				}
			} else {
				logger.lifecycle("Removing fonts: " + it.destinationDir.path)
			}
		}
	}
}

fun Project.createBitmapFontGeneratorConfig() {
	val acornVersion: String by extra
	val gdxVersion: String by extra
	configurations.create("bitmapFontGenerator") {
		dependencies.apply {
			add(project.dependencies.create("com.acornui:gdx-font-processor:$acornVersion"))
			add(project.dependencies.create("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop"))
			add(project.dependencies.create("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"))
		}
	}
}

fun Project.createPackTexturesConfig() {
	val acornVersion: String by extra
	packTexturesClasspath = createRuntimeKotlinClasspath("packTextures") {
		runtimeOnly("com.acornui:acornui-texture-packer:$acornVersion")
	}
}

data class DirectoryToProcessEntry(
		val sourceDir: File,
		val destinationDir: File
)

typealias DirectoryProcessor = (suffix: String, entries: Iterable<DirectoryToProcessEntry>) -> Unit