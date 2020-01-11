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

package com.acornui.build.plugins.tasks

import com.acornui.build.plugins.util.kotlinExt
import com.acornui.toCamelCase
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

open class RunJvmTask : JavaExec() {

	init {
		dependsOn("jvmMainClasses", "jvmProcessResources")
		group = "application"
		val jvmTarget: KotlinTarget = project.kotlinExt.targets["jvm"]
		val compilation =
				jvmTarget.compilations["main"] as KotlinJvmCompilation

		classpath = project.files(
				compilation.runtimeDependencyFiles,
				compilation.output.allOutputs
		)
		workingDir = compilation.output.resourcesDir
		main =
				"${project.rootProject.group}.${project.rootProject.name}.jvm.${project.rootProject.name.toCamelCase().capitalize()}JvmKt"

		@Suppress("INACCESSIBLE_TYPE")
		this.jvmArgs = if (OperatingSystem.current() == OperatingSystem.MAC_OS) listOf("-XstartOnFirstThread") else emptyList()
	}

	@Input
	var debugMode: Boolean = false
		set(value) {
			field = value
			val debugArg = "-Ddebug=true"
			jvmArgs = if (value)
				jvmArgs!! + debugArg
			else
				jvmArgs!! - debugArg
		}
}