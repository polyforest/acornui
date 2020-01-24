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
dependencies {
	compileOnly(gradleKotlinDsl())
	compileOnly(gradleApi())
	compileOnly(kotlin("compiler", version = kotlinVersion))
	compileOnly(kotlin("gradle-plugin", version = kotlinVersion))

	testImplementation(gradleKotlinDsl())
	testImplementation(kotlin("test", version = kotlinVersion))
	testImplementation(kotlin("test-junit", version = kotlinVersion))
}

val kotlinLanguageVersion: String by project.extra
val kotlinJvmTarget: String by project.extra

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
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
		create("root-settings") {
			id = "com.acornui.root-settings"
			implementationClass = "com.acornui.build.plugins.RootSettingsPlugin"
			displayName = "Settings configuration for an acorn ui project."
			description = "Configuration of root settings for an Acorn UI application."
		}
	}
}