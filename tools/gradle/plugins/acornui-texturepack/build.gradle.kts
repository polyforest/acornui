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

val KOTLIN_VERSION by acornConfig
dependencies {
	implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")
}

val TARGET_JVM_VERSION by acornConfig
tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = TARGET_JVM_VERSION
		javaParameters = true
		verbose = true
	}
}

val ACORNUI_BUILD_UTIL_PACKAGE by acornConfig
configure<GradlePluginDevelopmentExtension> {
	plugins {
		create("acornUiTexturePackProject") {
			id = "${project.group}.${project.name}"
			implementationClass = "$ACORNUI_BUILD_UTIL_PACKAGE.AcornUiTexturePackPlugin"
		}
	}
}
