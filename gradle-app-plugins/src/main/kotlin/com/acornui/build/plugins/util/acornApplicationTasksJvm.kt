/*
 * Copyright 2020 Poly Forest, LLC
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

@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.util

import com.acornui.build.plugins.tasks.RunJvmTask
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget


fun Project.configureRunJvmTask() {
	tasks.register<RunJvmTask>("runJvm") {
		debugMode = true
	}
}

fun Project.configureUberJarTask() {
	tasks.register<Jar>("uberJar") {
		dependsOn("jvmJar")
		group = "build"
		archiveBaseName.set("${project.name}-uber")
		val mainClass = tasks.getByName<JavaExec>("runJvm").main
		manifest {
			attributes["Implementation-Version"] = project.version.toString()
			attributes["Main-Class"] = mainClass
		}
		val jvmTarget: KotlinTarget = kotlinExt.targets["jvm"]
		val compilation = jvmTarget.compilations["main"] as KotlinCompilationToRunnableFiles<KotlinCommonOptions>
		from({
			compilation.runtimeDependencyFiles.filter { it.name.endsWith("jar") }.map { zipTree(it) }
		})
		from(compilation.output.resourcesDir)
		with(tasks["jvmJar"] as CopySpec)
	}
}