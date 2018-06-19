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
import com.acornui.io.file.FilesManifestSerializer
import com.acornui.jvm.io.file.ManifestUtil
import com.acornui.serialization.JsonSerializer
import com.google.javascript.jscomp.*
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.cli.js.dce.K2JSDce
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * A JS module is for js backend modules.
 */
open class JsModule(

		/**
		 * The base directory of the module. (Must exist)
		 */
		baseDir: File,

		/**
		 * The name of the module, will be used when matching command line parameters.
		 */
		name: String = baseDir.name,

		/**
		 * The directory for compilation output.
		 */
		out: File = File("out"),

		/**
		 * The distribution directory for jars and compiled assets.
		 */
		dist: File = File("dist")
) : Module(baseDir, name, out, dist) {

	init {
		hasJvm = false
	}

	/**
	 * The directory js files will be placed.
	 */
	protected open val libDir: String = "lib"

	var outWww = rel("www")
	var distWww = rel("wwwDist")

	/**
	 * If true, the deploy js step will minimize the output.
	 */
	var minimize = true

	/**
	 * If true, the deploy js step will monkeypatch the output.
	 */
	var optimize = true

	/**
	 * Don't extract the manifest or meta.js files from the js jar, or any folders, just the compiled js.
	 */
	private fun extractFilter(entry: JarEntry): String? {
		val entryName = entry.name
		return if (entryName.startsWith("MANIFEST") || entryName.startsWith("META-INF") || entryName.endsWith("meta.js")) null
		else {
			if (entryName.contains("/")) null
			else "$libDir/$entryName"
		}
	}

	override fun mergeAssets() {
		println("Merging assets $name minimize=false optimize=false")
		_mergeAssets(outWww, minimize = false, optimize = false)
	}

	override fun deployJs() {
		println("Deploying $name minimize=$minimize optimize=$optimize")
		distWww.clean()
		_mergeAssets(distWww, minimize = minimize, optimize = optimize)
	}

	private fun _mergeAssets(dest: File, minimize: Boolean, optimize: Boolean) {
		val libDirFile = File(dest, libDir)
		libDirFile.mkdirs()

		// Extract all library js files from the deployed Jars.
		val libraryFiles = expandLibraryDependencies(jsLibraryDependencies, ArrayList())

		walkDependenciesBottomUp {
			it.outAssets.copyRecursively(dest, onError = BuildUtil.copyIfNewer) // Copy all assets from the dependent modules.
			File(it.outJs, "${it.name}.js").copyIfNewer(File(libDirFile, "${it.name}.js"))
			if (!minimize && !optimize) File(it.outJs, "${it.name}.js.map").copyIfNewer(File(libDirFile, "${it.name}.js.map"))
		}
		for (i in libraryFiles) {
			JarUtil.extractFromJar(JarFile(i), dest, this::extractFilter)
		}

		if (minimize) dce(dest)
		if (optimize) optimize(dest)

		val manifest = ManifestUtil.createManifest(File(dest, "lib/"), dest)
		val filesJs = File(dest, "lib/files.js")
		println("Writing js files manifest: ${filesJs.absolutePath}")
		filesJs.writeText("var manifest = ${JsonSerializer.write(manifest, FilesManifestSerializer)};")

		val m = SourceFileManipulator()
		m.addProcessor(ScriptCacheBuster::replaceVersionWithModTime, *ScriptCacheBuster.extensions)
		m.process(dest)

		// Copy version txt
		if (BuildUtil.buildVersion.exists())
			BuildUtil.buildVersion.copyTo(File(dest, "assets/build.txt"), overwrite = true)
		AcornAssets.writeManifest(File(dest, "assets/"), dest)
	}

	private fun optimize(dest: File) {
		// Apply transformations on the source files.
		val jsPatcher = SourceFileManipulator()
		jsPatcher.addProcessor(KotlinMonkeyPatcher::optimizeProductionCode, "js")
		jsPatcher.process(dest)
	}

	private fun dce(dest: File) {
		val sources = arrayListOf(File(dest, "$libDir/kotlin.js"))
		walkDependenciesBottomUp {
			sources.add(File(dest, "$libDir/${it.name}.js"))
		}

		val compilerArgs = K2JSDceArguments().apply {
			outputDirectory = File(dest, libDir).absolutePath
			freeArgs = sources.toStringList()
			devMode = false
			printReachabilityInfo = false
		}
		val exitCode = K2JSDce().exec(BasicMessageCollector(verbose = verbose), Services.EMPTY, compilerArgs)
		if (exitCode != ExitCode.OK) System.exit(exitCode.code)
	}

	private fun uglify(dest: File) {
		val compiler = Compiler()
		val options = CompilerOptions()
		options.languageIn = CompilerOptions.LanguageMode.ECMASCRIPT5
		options.languageOut = CompilerOptions.LanguageMode.ECMASCRIPT5
		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options)
		WarningLevel.QUIET.setOptionsForWarningLevel(options)

		val sources = arrayListOf(File(dest, "$libDir/kotlin.js"))
		walkDependenciesBottomUp {
			sources.add(File(dest, "$libDir/${it.name}.js"))
		}
		compiler.compile(listOf(), sources.map { SourceFile.fromFile(it) }, options)

		for (message in compiler.errors) {
			System.err.println("Error message: $message")
		}
		val minFile = File(dest, "$libDir/$name.js")
		for (source in sources) {
			// Remove source files that were merged. Don't delete the file we're going to use for the minified version.
			if (source.absolutePath != minFile.absolutePath)
				source.delete()
		}
		minFile.writeText(compiler.toSource())
	}


	override fun clean() {
		super.clean()
		outWww.delete()
	}
}



