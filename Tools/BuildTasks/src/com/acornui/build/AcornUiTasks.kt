package com.acornui.build

import com.acornui.build.model.*
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
import java.util.jar.JarFile

open class AcornUiTasks(private val config: AcornUiTasksConfig) {

	@Task
	open suspend fun JsModuleVo.build(logPrefix: String = "") {
		buildAssets(logPrefix)
		buildJs(logPrefix)
	}

	@Task
	open suspend fun JvmModuleVo.build(logPrefix: String = "") {
		buildAssets(logPrefix)
		buildJvm(logPrefix)
	}

	@Task
	open suspend fun CommonModuleVo.build(logPrefix: String = "") {
		buildAssets(logPrefix)
		buildJvm(logPrefix)
		buildJs(logPrefix)
	}

	@Task
	open suspend fun AcornUiJsBackendVo.buildAssets(logPrefix: String = "") {
		(this as JsModuleVo).buildAssets(logPrefix)
		extractKotlinJs(logPrefix)
	}

	@Task
	open suspend fun ModuleVo.clean() {
		config.doWork("Clean Assets", true) {
			sourcesJar.delete()
			outAssets.deleteRecursively()
			dist.deleteRecursively()
			clearIdempotentCache()
		}
	}

	@Task
	open suspend fun JsModuleVo.clean() {
		(this as ModuleVo).clean()
		config.doWork("Clean JS", true) {
			jsSettings.jsJar.delete()
			jsSettings.outJs.deleteRecursively()
		}
	}

	@Task
	open suspend fun JvmModuleVo.clean() {
		(this as ModuleVo).clean()
		config.doWork("Clean JVM", true) {
			jvmSettings.jvmJar.delete()
			jvmSettings.outJvm.deleteRecursively()
		}
	}

	@Task
	open suspend fun CommonModuleVo.clean() {
		(this as JsModuleVo).clean()
		(this as JvmModuleVo).clean()
	}

	@Task
	open suspend fun ModuleVo.buildAssets(logPrefix: String = "") {
		idempotent(logPrefix) {
			Log.info("$logPrefix:$name buildAssets")
			moduleDependencies.asyncForEach {
				it.buildAssets("$logPrefix\t")
			}
			config.doWork("$logPrefix\t> Build $name Assets", sourcesAreNewer(resources, outAssets)) {
				outAssets.clean()
				for (resDir in resources) {
					if (!resDir.exists()) continue
					println("Copying assets for $name")
					resDir.copyRecursively(outAssets)
				}
			}
		}
	}

	@Task
	open suspend fun AcornUiJsBackendVo.extractKotlinJs(logPrefix: String = "") = idempotent {
		// Pull the kotlin-jslib out of the kotlin runtime jar.
		val cp = System.getProperty("java.class.path")
		val indexB = cp.indexOf("kotlin-stdlib.jar")
		val indexA = cp.lastIndexOf(PATH_SEPARATOR, indexB) + 1
		val runtimeLibFolder = File(cp.substring(indexA, indexB))
		config.doWork("$logPrefix\t> Extract kotlin.js", sourcesAreNewer(listOf(runtimeLibFolder) + resources, File(outAssets, "lib/kotlin.js"))) {
			outAssets.clean()
			val jsLib = File(runtimeLibFolder, "kotlin-stdlib-js.jar")
			val jsLibJar = JarFile(jsLib)
			JarUtil.extractFromJar(jsLibJar, outAssets, {
				if (it.name == "kotlin.js") "lib/kotlin.js" // place the kotlin.js file in the outAssets/lib directory.
				else null
			})
			for (resDir in resources) {
				if (resDir.exists()) resDir.copyRecursively(File(outAssets, "assets/"))
			}
		}
	}

	@Task
	open suspend fun JvmModuleVo.buildJvm(logPrefix: String = "") {
		idempotent(logPrefix) {
			Log.info("$logPrefix:$name buildJvm")
			val sourceFolders = ArrayList<File>(sources)
			val jvmSrc = rel("jvmSrc")
			if (jvmSrc.exists())
				sourceFolders.add(jvmSrc)

			moduleDependencies.asyncForEach {
				it.buildJvm("$logPrefix\t")
			}

			val libraryFiles = moduleDependencies.flatMap { it.moduleDependencies }.map { it.jvmSettings.outJvm.absolutePath }.toMutableSet()
			expandLibraryDependencies(jvmSettings.jvmLibraryDependencies, libraryFiles)
			val compilerArgs = K2JVMCompilerArguments().apply {
				jvmTarget = "1.8"
				apiVersion = "1.3"
				destination = jvmSettings.outJvm.absolutePath
				if (libraryFiles.isNotEmpty())
					classpath = libraryFiles.joinToString(PATH_SEPARATOR)
				includeRuntime = jvmSettings.includeKotlinJvmRuntime
				freeArgs = sourceFolders.map { it.absolutePath }
			}

			config.doWork("$logPrefix\t> Compile $name for JVM", sourcesAreNewer(sourceFolders + libraryFiles.map(::File), jvmSettings.outJvm)) {
				jvmSettings.outJvm.clean()
				val exitCode = K2JVMCompiler().exec(BasicMessageCollector(verbose = Module.verbose), Services.EMPTY, compilerArgs)
				if (exitCode != ExitCode.OK) System.exit(exitCode.code)
			}
		}
	}

	@Task
	open suspend fun JsModuleVo.buildJs(logPrefix: String = "") {
		idempotent(logPrefix) {
			Log.info("$logPrefix:$name buildJs")
			val sourceFolders = ArrayList<File>(sources)
			val jsSrc = rel("jsSrc")
			if (jsSrc.exists())
				sourceFolders.add(jsSrc)
			Log.info("sourceFolders: ${sourceFolders.joinToString(", ")}")

			val libraryFiles = moduleDependencies.flatMap { it.moduleDependencies }.map {
				val metaFile = File(it.jsSettings.outJs, "${it.name}.meta.js")
				metaFile.absolutePath
			}.toMutableSet()
			expandLibraryDependencies(jsSettings.jsLibraryDependencies, libraryFiles)
			val compilerArgs = K2JSCompilerArguments().apply {
				moduleKind = config.moduleKind.name.toLowerCase()
				outputFile = File(jsSettings.outJs, "$name.js").absolutePath
				sourceMap = true
				metaInfo = true
				if (libraryFiles.isNotEmpty())
					libraries = libraryFiles.joinToString(PATH_SEPARATOR)
				freeArgs = sourceFolders.map { it.absolutePath }
			}

			config.doWork("$logPrefix\t> Compile $name for JS", sourcesAreNewer(sourceFolders + libraryFiles.map(::File), jsSettings.outJs)) {
				jsSettings.outJs.clean()
				val exitCode = K2JSCompiler().exec(BasicMessageCollector(verbose = Module.verbose), Services.EMPTY, compilerArgs)
				if (exitCode != ExitCode.OK) System.exit(exitCode.code)
			}
		}
	}

	companion object {

		// Utility

		/**
		 * Populates the [out] list with a list of jars, provided from directories and individual jars from [dependencies]
		 * @return Returns the [out] parameter.
		 */
		private fun expandLibraryDependencies(dependencies: List<File>, out: MutableSet<String>): MutableSet<String> {
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

open class AcornUiModules {

	@ModelProp
	val acornUtils = CommonModuleVoImpl(File(ACORNUI_HOME, "AcornUtils"), out = ACORNUI_OUT, dist = ACORNUI_DIST)

	@ModelProp
	val acornUiCore = CommonModuleVoImpl(File(ACORNUI_HOME, "AcornUiCore"), out = ACORNUI_OUT, dist = ACORNUI_DIST).apply {
		moduleDependencies = listOf(acornUtils)
	}

	@ModelProp
	val acornUiGame = CommonModuleVoImpl(File(ACORNUI_HOME, "AcornGame"), out = ACORNUI_OUT, dist = ACORNUI_DIST).apply {
		moduleDependencies = listOf(acornUiCore)
	}

	@ModelProp
	val acornUiSpine = CommonModuleVoImpl(File(ACORNUI_HOME, "AcornSpine"), out = ACORNUI_OUT, dist = ACORNUI_DIST).apply {
		moduleDependencies = listOf(acornUiCore)
	}

	@ModelProp
	val acornUiJsBackend = AcornUiJsBackendVo().apply {
		moduleDependencies = listOf(acornUiCore)
	}
}

class AcornUiJsBackendVo : JsModuleVoImpl(File(ACORNUI_HOME, "AcornUiJsBackend"), out = ACORNUI_OUT, dist = ACORNUI_DIST)

fun main(args: Array<String>) {
	runCommands(args, { AcornUiTasksConfig() }, { AcornUiModules() }, { AcornUiTasks(it) })
}

class AcornUiTasksConfig {

	/**
	 * The module type for JS output.
	 */
	@ConfigProp
	var moduleKind by Freezable(ModuleKind.UMD)

	@ConfigProp
	var dry by Freezable(false)

	@ConfigProp
	var force by Freezable(false)
}

fun AcornUiTasksConfig.doWork(message: String, outOfDate: Boolean, work: () -> Unit) {
	if (force || outOfDate) {
		Log.info("$message: ${if (!outOfDate && force) "UP-TO-DATE-FORCED" else "OUT-OF-DATE"}${if (dry) "-DRY" else ""}")
		if (!dry)
			work()
	} else {
		Log.info("$message:  UP-TO-DATE")
	}
}

