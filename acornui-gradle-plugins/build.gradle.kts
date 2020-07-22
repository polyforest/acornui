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
	val kotlinVersion: String by project
	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$kotlinVersion")
	}
}

apply(plugin = "kotlin-sam-with-receiver")

samWithReceiver {
	annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.samWithReceiver(configure: SamWithReceiverExtension.() -> Unit): Unit = extensions.configure("samWithReceiver", configure)

val kotlinVersion: String by project

dependencies {
	compileOnly(gradleKotlinDsl())
	compileOnly(gradleApi())
	implementation(kotlin("compiler", version = kotlinVersion))
	implementation(kotlin("gradle-plugin", version = kotlinVersion))

	testImplementation(gradleKotlinDsl())
	testImplementation(gradleTestKit())
	testImplementation(kotlin("test", version = kotlinVersion))
	testImplementation(kotlin("test-junit", version = kotlinVersion))
}

val kotlinLanguageVersion: String by project

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
	sourceSets.configureEach {
		languageSettings.apply {
			enableLanguageFeature("InlineClasses")
			useExperimentalAnnotation("kotlin.Experimental")
			useExperimentalAnnotation("kotlin.time.ExperimentalTime")
			useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
		}
	}
	target {
		compilations.configureEach {
			kotlinOptions {
				jvmTarget = "1.8"
				languageVersion = kotlinLanguageVersion
				apiVersion = kotlinLanguageVersion
			}
		}
	}
}

gradlePlugin {
	plugins {
		create("acornui") {
			id = "com.acornui.js"
			implementationClass = "com.acornui.build.plugins.AcornUiJsPlugin"
			displayName = "Acorn UI JS Configuration"
			description = "Configuration of an Acorn UI Application project."
		}
	}
}

tasks.named<ProcessResources>("processResources").configure {
	filesMatching("acorn.properties") {
		expand(project.properties)
	}
}

tasks.named<ProcessResources>("processTestResources").configure {
	filesMatching("**/gradle.properties") {
		expand(project.properties)
	}
}