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

import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

typealias ProjectConsumer<T> = (Project) -> T
val acornConfig: Map<String, String> = gradle.startParameter.projectProperties
val ACORNUI_SHARED_BUILD_PATH by acornConfig
val ACORNUI_HOME: String by acornConfig
apply(from = "$ACORNUI_HOME/$ACORNUI_SHARED_BUILD_PATH")

plugins {
	kotlin("jvm")
}

val acornAllProjects: ProjectConsumer<Unit> by extra
acornAllProjects(project)

val DEFAULT_ACORNUI_PROJECT_VERSION by acornConfig
version = with(rootProject.file("version.txt")) {
	if (exists())
		readText().trim()
	else
		"1.0"
//				DEFAULT_ACORNUI_PROJECT_VERSION.also { writeText(it) }
}

val jvmProjectConfiguration: ProjectConsumer<Unit> by extra
jvmProjectConfiguration(project)
configure<KotlinJvmProjectExtension> {
	experimental.coroutines = Coroutines.ENABLE
}

val TARGET_JVM_VERSION by acornConfig
tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = TARGET_JVM_VERSION
		javaParameters = true
		verbose = true
	}
}

val ACORNUI_GROUP by acornConfig
dependencies {
	implementation(project(":acornui-core:acornui-core-jvm"))
	implementation(project(":tools:acornui-utils:acornui-utils-jvm"))
	implementation(project(":tools:acornui-texturepacker:acornui-texturepacker-jvm"))
}
