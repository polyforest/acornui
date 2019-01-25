package com.acornui.build

import com.acornui.build.model.*
import com.acornui.build.util.*
import com.acornui.logging.Log
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

open class AcornUiTasks(private val config: AcornUiTasksConfig) {

	@Task
	open fun JvmModuleVo.buildJvm(logPrefix: String = "") {
		idempotent(logPrefix) {
			Log.info("$logPrefix:$name buildJvm")
			val sourceFolders = ArrayList<File>(sources)
			val jvmSrc = rel("jvmSrc")
			if (jvmSrc.exists())
				sourceFolders.add(jvmSrc)

			val libraryFiles = ArrayList<String>()
			for (it in moduleDependencies) {
				if (it !is JvmModuleVo) throw ConfigurationException("Module $name cannot depend on a non-jvm module: ${it.name}")
				it.buildJvm("$logPrefix\t")
				libraryFiles.add(it.jvmSettings.outJvm.absolutePath)
			}

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

	companion object {

		// Utility

		/**
		 * Populates the [out] list with a list of jars, provided from directories and individual jars from [dependencies]
		 * @return Returns the [out] parameter.
		 */
		private fun expandLibraryDependencies(dependencies: List<File>, out: MutableList<String>): MutableList<String> {
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
		moduleDependencies += listOf(acornUtils)
	}

	@ModelProp
	val acornUiGame = CommonModuleVoImpl(File(ACORNUI_HOME, "AcornGame"), out = ACORNUI_OUT, dist = ACORNUI_DIST).apply {
		moduleDependencies += listOf(acornUiCore)
	}

	@ModelProp
	val acornUiSpine = CommonModuleVoImpl(File(ACORNUI_HOME, "AcornSpine"), out = ACORNUI_OUT, dist = ACORNUI_DIST).apply {
		moduleDependencies += listOf(acornUiCore)
	}

	@ModelProp
	val acornUiJsBackend = JsModuleVoImpl(File(ACORNUI_HOME, "AcornUiJsBackend"), out = ACORNUI_OUT, dist = ACORNUI_DIST).apply {
			moduleDependencies = listOf(acornUiCore)
	}
}

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

