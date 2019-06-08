/*
 * Copyright 2019 Poly Forest, LLC
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
import com.acornui.serialization.json
import com.acornui.serialization.write
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.cli.js.dce.K2JSDce
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * A module for an AcornUI Application
 */
open class AppModule(

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

	/**
	 * The directory js files will be placed.
	 */
	protected open val jsLibDir: String = "lib"

	var outWww = rel("src/jsMain/www")
	var distWww = rel("src/jsMain/wwwDist")

	/**
	 * If true, the deploy js step will minimize the output.
	 */
	var minimize = true

	/**
	 * If true, the deploy js step will monkeypatch the output.
	 */
	var optimize = true

	var jvmOutWorking = File(out, "jvm/production/$name-jvm/")

	/**
	 * Don't extract the manifest or meta.js files from the js jar, or any folders, just the compiled js.
	 */
	private fun extractFilter(entry: JarEntry): String? {
		val entryName = entry.name
		return if (entryName.startsWith("MANIFEST") || entryName.startsWith("META-INF") || entryName.endsWith("meta.js")) null
		else {
			if (entryName.contains("/")) null
			else "$jsLibDir/$entryName"
		}
	}

	/**
	 * Pull built assets from dependent modules, merging them into one folder.
	 */
	fun mergeAssetsJvm() {
		if (!hasJvm) return
		walkDependenciesBottomUp {
			it.outAssets.copyRecursively(jvmOutWorking, onError = BuildUtil.copyIfNewer)
		}
		// Copy version txt
		if (BuildUtil.buildVersion.exists())
			BuildUtil.buildVersion.copyTo(File(jvmOutWorking, "assets/build.txt"), overwrite = true)
		AcornAssets.writeManifest(File(jvmOutWorking, "assets/"), jvmOutWorking)
	}

	/**
	 * Pull built assets from dependent modules, merging them into one folder.
	 */
	fun mergeAssetsJs() {
		if (!hasJs) return
		println("Merging assets $name minimize=false optimize=false")
		_mergeAssets(outWww, minimize = false, optimize = false)
	}

	override fun deployJs() {
		println("Deploying $name minimize=$minimize optimize=$optimize")
		distWww.clean()
		_mergeAssets(distWww, minimize = minimize, optimize = optimize)
	}

	private fun _mergeAssets(dest: File, minimize: Boolean, optimize: Boolean) {
		val libDirFile = File(dest, jsLibDir)
		libDirFile.mkdirs()

		// Extract all library js files from the deployed Jars.
		val libraryFiles = expandLibraryDependencies(jsLibraryDependencies, ArrayList())

		walkDependenciesBottomUp {
			it.outAssets.copyRecursively(dest, onError = BuildUtil.copyIfNewer) // Copy all assets from the dependent modules.
			if (it.hasJs) {
				File(it.outJs, "${it.name}.js").copyIfNewer(File(libDirFile, "${it.name}.js"))
				if (!minimize && !optimize) File(it.outJs, "${it.name}.js.map").copyIfNewer(File(libDirFile, "${it.name}.js.map"))
			}
		}

		for (i in libraryFiles) {
			JarUtil.extractFromJar(JarFile(i), dest, ::extractFilter)
		}

		if (minimize) dce(dest)
		if (optimize) optimize(dest)
//		if (minimize) uglify(dest)

		val manifest = ManifestUtil.createManifest(File(dest, "lib/"), dest)
		val filesJs = File(dest, "lib/files.js")
		println("Writing js files manifest: ${filesJs.absolutePath}")
		filesJs.writeText("var manifest = ${json.write(manifest, FilesManifestSerializer)};")

		val m = SourceFileManipulator()
		m.addProcessor(ScriptCacheBuster::replaceVersionWithModTime, *ScriptCacheBuster.extensions)
		m.process(dest)

		// Copy version txt
		if (BuildUtil.buildVersion.exists())
			BuildUtil.buildVersion.copyTo(File(dest, "assets/build.txt"), overwrite = true)
		AcornAssets.writeManifest(File(dest, "assets/"), dest)
	}

	private fun optimize(dest: File) {
		println("Optimizing")
		// Apply transformations on the source files.
		val jsPatcher = SourceFileManipulator()
		jsPatcher.addProcessor(KotlinMonkeyPatcher::optimizeProductionCode, "js")
		jsPatcher.process(dest)
	}

	private fun dce(dest: File) {
		println("Dce")
		val sources = arrayListOf(File(dest, "$jsLibDir/kotlin.js"))
		walkDependenciesBottomUp {
			if (it.hasJs)
				sources.add(File(dest, "$jsLibDir/${it.name}.js"))
		}

		val compilerArgs = K2JSDceArguments().apply {
			outputDirectory = File(dest, jsLibDir).absolutePath
			freeArgs = sources.map { it.absolutePath }
			devMode = false
			printReachabilityInfo = false
		}
		val exitCode = K2JSDce().exec(BasicMessageCollector(verbose = verbose), Services.EMPTY, compilerArgs)
		if (exitCode != ExitCode.OK) System.exit(exitCode.code)
	}

//	private fun uglify(dest: File) {
//		println("Uglifying")
//		val compiler = Compiler()
//		val options = CompilerOptions()
//		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options)
//		options.languageIn = CompilerOptions.LanguageMode.ECMASCRIPT5
////		options.languageOut = CompilerOptions.LanguageMode.ECMASCRIPT5
//		WarningLevel.QUIET.setOptionsForWarningLevel(options)
//
//		val sources = arrayListOf(File(dest, "$jsLibDir/kotlin.js"))
//		walkDependenciesBottomUp {
//			sources.add(File(dest, "$jsLibDir/${it.name}.js"))
//		}
//		compiler.compile(listOf(), sources.map { SourceFile.fromFile(it.absolutePath) }, options)
//
//		for (message in compiler.errors) {
//			System.err.println("Error message: $message")
//		}
//		val minFile = File(dest, "$jsLibDir/$name.js")
//		for (source in sources) {
//			// Remove source files that were merged. Don't delete the file we're going to use for the minified version.
//			if (source.absolutePath != minFile.absolutePath)
//				source.delete()
//		}
//		minFile.writeText(compiler.toSource())
//	}

	/**
	 * Creates a single, runnable jar.
	 */
	open fun fatJar() {
	}

//	/**
//	 * Uses Packr to create a win64 executable.
//	 *
//	 * Windows troubleshooting:
//	 * If double clicking the exe does nothing, use your.exe -c --console -v  to see the problem.
//	 */
//	open fun win64() {
//		println("Creating executable for $name")
//		val config = createBasePackrConfig()
//		config.platform = PackrConfig.Platform.Windows64
//		config.jdk = "http://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-win_x64.zip"
//		config.outDir = File("out-win64")
//		Packr().pack(config)
//	}
//
//	/**
//	 * Uses Packr to create a mac64 executable.
//	 */
//	open fun mac64() {
//		println("Creating dmg for $name")
//		val config = createBasePackrConfig()
//		config.platform = PackrConfig.Platform.MacOS
//		config.jdk = "http://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-macosx_x64.zip"
//		config.outDir = File("out-mac")
//		Packr().pack(config)
//	}
//
//	/**
//	 * Uses Packr to create a linux64 executable.
//	 */
//	open fun linux64() {
//		println("Creating for $name")
//		val config = createBasePackrConfig()
//		config.platform = PackrConfig.Platform.Linux64
//		config.jdk = "http://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-linux_x64.tar.gz"
//		config.outDir = File("out-linux64")
//		Packr().pack(config)
//	}
//
//	protected open fun createBasePackrConfig(): PackrConfig {
//		val config = PackrConfig()
//		config.executable = name
//
//		val libraryFiles = ArrayList<String>()
//		walkDependenciesBottomUp {
//			libraryFiles.add(it.jvmJar.absolutePath)
//			Module.expandLibraryDependencies(it.jvmLibraryDependencies, libraryFiles)
//			Module.expandLibraryDependencies(it.jvmRuntimeDependencies, libraryFiles)
//		}
//		libraryFiles.add(jvmJar.absolutePath)
//
//		val cp = System.getProperty("java.class.path")
//		val indexB = cp.indexOf("kotlin-runtime.jar")
//		val indexA = cp.lastIndexOf(PATH_SEPARATOR, indexB) + 1
//		libraryFiles.add(cp.substring(indexA, indexB + "kotlin-runtime.jar".length))
//		config.classpath = libraryFiles
//		config.mainClass = mainClass!!
//		config.vmArgs = arrayListOf("-Xmx1G")
//		config.minimizeJre = "hard"
//		config.resources = jvmOutWorking.listFiles().toList()
//		return config
//	}

	override fun clean() {
		super.clean()
		outWww.deleteRecursively()
		distWww.deleteRecursively()
	}
}



