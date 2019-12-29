@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.tasks

import com.acornui.build.plugins.logging.BasicMessageCollector
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.js.dce.K2JSDce
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.gradle.tasks.throwGradleExceptionIfError

open class DceTask @javax.inject.Inject constructor(objects: ObjectFactory) : DefaultTask() {

    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.ABSOLUTE)
    @InputFiles
    val source = objects.fileCollection()

    @get:OutputDirectory
    val outputDir = objects.directoryProperty()

    @Input
    val keep: MutableList<String> = ArrayList()

    fun keep(vararg fqn: String) {
        keep.addAll(fqn)
    }

    @TaskAction
    fun executeTask() {
        val sources = source.files.filter {
            it.extension.equals("js", ignoreCase = true) &&
                    !it.name.endsWith("meta.js", ignoreCase = true) &&
                    !it.name.endsWith("js.map", ignoreCase = true) &&
                    !it.name.startsWith("kotlin-test-nodejs-runner")
        }.map { it.path }.toList()
        logger.lifecycle("Dead Code Elimination on files: $sources")

        val dce = K2JSDce()
        val args = dce.createArguments().apply {
            declarationsToKeep = keep.toTypedArray()
            outputDirectory = outputDir.asFile.get().absolutePath
            freeArgs = sources
            devMode = false
            printReachabilityInfo = false
        }
        val exitCode = dce.exec(BasicMessageCollector(logger), Services.EMPTY, args)
        throwGradleExceptionIfError(exitCode)
    }
}