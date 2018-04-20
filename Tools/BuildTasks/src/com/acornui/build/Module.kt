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

package com.acornui.build

import com.acornui.build.util.*
import com.acornui.logging.Log
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

import java.util.jar.Attributes
import java.util.jar.Manifest

abstract class Module(

		/**
		 * The base directory of the module. (Must exist)
		 */
		protected val baseDir: File,

		/**
		 * The name of the module, will be used when matching command line parameters.
		 */
		val name: String = baseDir.name,

		/**
		 * The directory for compilation output.
		 */
		val out: File = File("out"),

		/**
		 * The distribution directory for jars and compiled assets.
		 */
		val dist: File = File("dist")
) {

	var jvmJar = File(dist, "${name}_jvm.jar")
	var jsJar = File(dist, "${name}_js.jar")
	var sourcesJar = File(dist, "${name}_sources.jar")

	var outJs = File(out, "js/production/$name/")
	var outJvm = File(out, "jvm/production/$name/")
	var outAssets = File(out, "assets/production/$name/")

	var hasJs = true
	var hasJvm = true

	/**
	 * A list of resource directories.
	 */
	var resources = listOf(rel("resources"))

	var moduleDependencies = listOf<Module>()
	var jvmLibraryDependencies = listOf(rel("lib"))
	var jsLibraryDependencies = listOf<File>()
	var sources = listOf(rel("src"))

	var includeKotlinJvmRuntime: Boolean = false

	/**
	 * For deployJvm, if there is no manifest file, a manifest will be created using this as the main class.
	 */
	var mainClass: String? = null

	/**
	 * The module type for JS output.
	 */
	var moduleKind = ModuleKind.AMD

	init {
		if (!baseDir.exists()) throw Exception("${baseDir.absolutePath} does not exist.")
	}

	/**
	 * A shortcut to getting a file as a child of the [baseDir].
	 */
	fun rel(s: String): File {
		return File(baseDir, s)
	}

	/**
	 * Returns the resources directory for the given skin.
	 *
	 * @param name The name of the skin folder. E.g. "basic"
	 */
	fun skin(name: String): File {
		return File(BuildUtil.ACORNUI_HOME_PATH, "Skins/$name/resources")
	}

	open fun buildAssets() {
		if (sourcesAreNewer(resources, outAssets)) {
			outAssets.clean()
			for (resDir in resources) {
				if (!resDir.exists()) continue
				println("Copying assets for $name")
				resDir.copyRecursively(outAssets)
			}
		}
	}

	open fun buildJs() {
		if (!hasJs) return
		val sourceFolders = ArrayList<File>(sources)
		val jsSrc = rel("jsSrc")
		if (jsSrc.exists())
			sourceFolders.add(jsSrc)
		Log.info("sourceFolders: ${sourceFolders.joinToString(", ")}")
		val libraryFiles = ArrayList<String>()
		for (i in moduleDependencies) {
			val metaFile = File(i.outJs, "${i.name}.meta.js")
			if (!metaFile.exists()) throw Exception("Halted, dependency not built: ${i.name}")
			libraryFiles.add(metaFile.absolutePath)
		}
		if (sourcesAreNewer(sourceFolders + libraryFiles.map(::File), outJs)) {
			outJs.clean()
			expandLibraryDependencies(jsLibraryDependencies, libraryFiles)
			val compilerArgs = K2JSCompilerArguments().apply {
				//coroutinesWarn = false
				moduleKind = this@Module.moduleKind.name.toLowerCase()
				outputFile = File(outJs, "$name.js").absolutePath
				sourceMap = true
				metaInfo = true
				if (libraryFiles.isNotEmpty())
					libraries = libraryFiles.joinToString(PATH_SEPARATOR)
				freeArgs = sourceFolders.toStringList()
			}
			println("$name Compiling JS")
			val exitCode = K2JSCompiler().exec(BasicMessageCollector(verbose = verbose), Services.EMPTY, compilerArgs)
			if (exitCode != ExitCode.OK) System.exit(exitCode.code)
		}
	}

	open fun buildJvm() {
		if (!hasJvm) return
		val sourceFolders = ArrayList<File>(sources)
		val jvmSrc = rel("jvmSrc")
		if (jvmSrc.exists())
			sourceFolders.add(jvmSrc)
		Log.info("sourceFolders: ${sourceFolders.joinToString(", ")}")

		val libraryFiles = ArrayList<String>()
		for (i in moduleDependencies) {
			if (!i.outJvm.exists()) throw Exception("Halted, dependency not built: ${i.name}")
			libraryFiles.add(i.outJvm.absolutePath)
		}
		if (sourcesAreNewer(sourceFolders + libraryFiles.map(::File), outJvm)) {
			outJvm.clean()
			expandLibraryDependencies(jvmLibraryDependencies, libraryFiles)
			val compilerArgs = K2JVMCompilerArguments().apply {
				destination = outJvm.absolutePath
				if (libraryFiles.isNotEmpty())
					classpath = libraryFiles.joinToString(PATH_SEPARATOR)
				includeRuntime = includeKotlinJvmRuntime
				freeArgs = sourceFolders.toStringList()
			}
			println("$name Compiling JVM")
			val exitCode = K2JVMCompiler().exec(BasicMessageCollector(verbose = verbose), Services.EMPTY, compilerArgs)
			if (exitCode != ExitCode.OK) System.exit(exitCode.code)
		}
	}

	/**
	 * Pull built assets from dependent modules, merging them into one folder.
	 */
	open fun mergeAssets() {
	}

	open fun deployJs() {
		if (!hasJs) return
		if (sourcesAreNewer(outJs, jsJar)) {
			println("$name Creating JS Jar")
			jsJar.delete()

			JarUtil.createJar(outJs, jsJar)
		}
	}

	open fun deployJvm() {
		if (!hasJvm) return
		if (sourcesAreNewer(outJvm, jvmJar)) {
			println("$name Creating JVM Jar ${jvmJar.absolutePath}")
			jvmJar.delete()

			var hasManifest = false
			for (sourceFolder in sources) {
				if (File(sourceFolder, "./META-INF/MANIFEST.MF").exists()) {
					hasManifest = true
					break
				}
			}

			val manifest: Manifest?
			if (hasManifest) {
				manifest = null
				println("Using existing manifest.")
			} else {
				println("Creating manifest with mainClass $mainClass.")
				manifest = Manifest()
				manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
				if (mainClass != null) manifest.mainAttributes[Attributes.Name.MAIN_CLASS] = mainClass
			}
			JarUtil.createJar(arrayOf(outJvm), jvmJar, manifest = manifest)
		}
	}

	open fun deploySources() {
		if (sourcesAreNewer(sources, sourcesJar)) {
			println("$name Creating Sources Jar")
			sourcesJar.delete()
			JarUtil.createJar(sources.toTypedArray(), sourcesJar, manifest = null)
		}
	}

	open fun clean() {
		jvmJar.delete()
		jsJar.delete()
		sourcesJar.delete()
		outJs.deleteRecursively()
		outJvm.deleteRecursively()
		outAssets.deleteRecursively()
		dist.deleteRecursively()
	}

	override fun toString(): String {
		return "[Module name=$name]"
	}

	fun executeJar(args: Array<String>, className: String = mainClass!!) {
		val libraryFiles = ArrayList<String>()
		for (i in moduleDependencies) {
			libraryFiles.add(i.jvmJar.absolutePath)
		}
		//println("classpath: " + System.getProperty("java.class.path"))
		expandLibraryDependencies(jvmLibraryDependencies, libraryFiles)

		val processBuilder = ProcessBuilder("java", "-cp", System.getProperty("java.class.path") + PATH_SEPARATOR + libraryFiles.joinToString(PATH_SEPARATOR) + PATH_SEPARATOR + jvmJar.absolutePath, className, *args)
		val process = processBuilder.start()
		Thread(LogStreamReader(process.inputStream)).start()
		Thread(LogStreamReader(process.errorStream, System.err)).start()
		val code = process.waitFor()
		if (code != 0)
			throw Exception("Error executing jar $name")
	}

	//----------------------------
	// Util
	//----------------------------

	fun walkDependenciesBottomUp(callback: (Module) -> Unit) = walkDependenciesBottomUp(HashMap(), callback)

	fun walkDependenciesBottomUp(exclude: HashMap<Module, Boolean>, callback: (Module) -> Unit) {
		for (i in moduleDependencies) {
			i.walkDependenciesBottomUp(exclude, callback)
		}
		if (!exclude.containsKey(this)) {
			exclude[this] = true
			callback(this)
		}
	}

	companion object {

		var verbose: Boolean = false

		var force: Boolean = false

		/**
		 * Populates the [out] list with a list of jars, provided from directories and individual jars from [dependencies]
		 * @return Returns the [out] parameter.
		 */
		fun expandLibraryDependencies(dependencies: List<File>, out: MutableList<String>): MutableList<String> {
			for (i in dependencies) {
				if (i.isDirectory) {
					for (j in i.listFiles()!!) {
						if (j.extension.toLowerCase() == "jar") out.add(j.absolutePath)
					}
				} else {
					if (i.extension.toLowerCase() == "jar") out.add(i.absolutePath)
				}
			}
			return out
		}

	}
}

