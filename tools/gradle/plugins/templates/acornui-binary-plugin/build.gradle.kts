/*
 * Copyright 2018 Poly Forest, LLC
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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val SHARED_PLUGINS_SCRIPTS_ROOT: String by gradle.startParameter.projectProperties
apply(from = "$SHARED_PLUGINS_SCRIPTS_ROOT/shared-plugins.build.gradle.kts")
plugins {
	`kotlin-dsl`
}

val acornConfig: MutableMap<String, String> = gradle.startParameter.projectProperties
val DEFAULT_ACORNUI_PLUGIN_VERSION by acornConfig
version = with(file("version.txt")) {
	if (exists())
		readText().trim()
	else
		"1.0"
//				DEFAULT_ACORNUI_PROJECT_VERSION.also { writeText(it) }
}

val TARGET_JVM_VERSION by acornConfig
// Refactoring the block below into the plugins common buildscript works for composite but not executing
// a single multiproject build from a composite in the IDE.
// TODO - MP: Write a test that will fail when this is possible and directs dev to refactor.
tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = TARGET_JVM_VERSION
		javaParameters = true
		verbose = true
	}
}

// TODO - MP - TEST: Test that multiple plugin declaration works for declaration below
// TODO - MP - TEST: Test that multiple plugin declaration works for resources route
// TODO - MP: Generate examples of short id descriptors for bundled plugins

/**
 * Note that declaring descriptor information via resources folder is unnecessary when using the block below, but can
 * be useful for declaring additional short-name id descriptors and those files have been left as examples for
 * posterity's sake.  Having them present will not interfere with the block below.
 */
val ACORNUI_BUILD_UTIL_PACKAGE by acornConfig
configure<GradlePluginDevelopmentExtension> {
	(plugins) {
		"acornUiProject" {
			id = "${project.group}.${project.name}.project"
			implementationClass = "$ACORNUI_BUILD_UTIL_PACKAGE.AcornUiProjectPlugin"
		}
		"acornUiSettings" {
			id = "${project.group}.${project.name}.settings"
			implementationClass = "$ACORNUI_BUILD_UTIL_PACKAGE.AcornUiSettingsPlugin"
		}
	}
}
