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

@file:Suppress("UNUSED_VARIABLE", "UnstableApiUsage")

package com.acornui.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate

@Suppress("unused")
open class KotlinMppPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		configure(project)
	}

	companion object {

		fun configure(project: Project) {
			KotlinCommonOptions.configure(project)
			if (project.jsEnabled)
				KotlinJsPlugin.configure(project)
			if (project.jvmEnabled)
				KotlinJvmPlugin.configure(project)
		}
	}
}

val Project.jsEnabled: Boolean
	get() {
		val jsEnabled: String? by this
		return jsEnabled?.toBoolean() ?: true
	}

val Project.jvmEnabled: Boolean
	get() {
		val jvmEnabled: String? by this
		return jvmEnabled?.toBoolean() ?: true
	}