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

plugins {
	//	id("org.gradle.kotlin.kotlin-dsl") version "1.2.9"
	`kotlin-dsl`
	`maven-publish`
	id("kotlinx-serialization")
}

kotlinDslPluginOptions {
	experimentalWarning.set(false)
}

val kotlinVersion: String by extra
val kotlinSerializationVersion: String by extra

dependencies {
	implementation(kotlin("compiler", version = kotlinVersion))
	implementation(kotlin("gradle-plugin", version = kotlinVersion))
	implementation(kotlin("serialization", version = kotlinVersion))
	implementation("com.acornui:acornui-utils:$version")
	implementation("com.acornui:acornui-core:$version")
	implementation("com.acornui:acornui-lwjgl-backend:$version")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")

	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	compile(rootProject.files("buildSrc/build/classes"))
}

kotlin {
	sourceSets {
		main {
			// This is gross, but as far as I know there's no way to publish plugins from the buildSrc project,
			// and this is less gross than duplicating code.
			kotlin.srcDirs(rootProject.file("buildSrc/src/main/kotlin"))
		}
	}
}

gradlePlugin {
	plugins {
		create("app") {
			id = "com.acornui.app"
			implementationClass = "com.acornui.build.plugins.AcornUiApplicationPlugin"
			displayName = "Acorn UI Multi-Platform Application"
			description = "Configuration of an Acorn UI Application."
		}

		create("root") {
			id = "com.acornui.root"
			implementationClass = "com.acornui.build.plugins.RootPlugin"
			displayName = "Root project plugin for a multi-module Acorn UI application."
			description = "Configuration of a root project for a multi-module Acorn UI application."
		}
		create("kotlinMpp") {
			id = "com.acornui.kotlin-mpp"
			implementationClass = "com.acornui.build.plugins.KotlinMppPlugin"
			displayName = "Kotlin multi-platform configuration for Acorn UI"
			description = "Configures an Acorn UI library project for Kotlin multi-platform."
		}
		create("kotlinJvm") {
			id = "com.acornui.kotlin-jvm"
			displayName = "Kotlin jvm configuration for Acorn UI"
			implementationClass = "com.acornui.build.plugins.KotlinJvmPlugin"
			description = "Configures an Acorn UI library project for Kotlin jvm."
		}
		create("kotlinJs") {
			id = "com.acornui.kotlin-js"
			implementationClass = "com.acornui.build.plugins.KotlinJsPlugin"
			displayName = "Kotlin js configuration for Acorn UI"
			description = "Configures an Acorn UI library project for Kotlin js."
		}
	}
}