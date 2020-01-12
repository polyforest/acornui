@file:Suppress("UnstableApiUsage")

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

import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension

plugins {
	`maven-publish`
	`java-gradle-plugin`
	kotlin("jvm")
}

buildscript {
	val kotlinVersion: String by extra
	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$kotlinVersion")
	}
}

apply(plugin = "kotlin-sam-with-receiver")

samWithReceiver {
	annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.samWithReceiver(configure: SamWithReceiverExtension.() -> Unit): Unit = extensions.configure("samWithReceiver", configure)

val kotlinVersion: String by extra
val kotlinSerializationVersion: String by extra
val dokkaVersion: String by extra

dependencies {
	compileOnly(gradleKotlinDsl())
	compileOnly(gradleApi())
	implementation(kotlin("compiler", version = kotlinVersion))
	implementation(kotlin("gradle-plugin", version = kotlinVersion))
	implementation(kotlin("serialization", version = kotlinVersion))
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")
	implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

	implementation(project(":acornui-utils"))
	implementation(project(":acornui-core"))
	implementation(project(":backends:acornui-lwjgl-backend"))

	testImplementation(gradleKotlinDsl())
	testImplementation(kotlin("test", version = kotlinVersion))
	testImplementation(kotlin("test-junit", version = kotlinVersion))

	implementation("com.acornui:gradle-kotlin-plugins:$version")
}

val kotlinLanguageVersion: String by project.extra
val kotlinJvmTarget: String by project.extra

java {
	withSourcesJar()
}

kotlin {
	sourceSets.all {
		languageSettings.useExperimentalAnnotation("kotlin.Experimental")
	}
	target {
		compilations.all {
			kotlinOptions {
				jvmTarget = kotlinJvmTarget
				languageVersion = kotlinLanguageVersion
				apiVersion = kotlinLanguageVersion
			}
		}
	}
}

gradlePlugin {
	plugins {
		create("root") {
			id = "com.acornui.root"
			implementationClass = "com.acornui.build.plugins.RootPlugin"
			displayName = "Root project plugin for a multi-module Acorn UI application."
			description = "Configuration of a root project for a multi-module Acorn UI application."
		}

		create("app") {
			id = "com.acornui.app"
			implementationClass = "com.acornui.build.plugins.AcornUiApplicationPlugin"
			displayName = "Acorn UI Multi-Platform Application"
			description = "Configuration of an Acorn UI Application."
		}
	}
}

tasks.named<ProcessResources>("processResources").configure {
	filesMatching("acornDependencies.txt") {
		expand(project.properties)
	}
}