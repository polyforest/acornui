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
	base
	idea
	`maven-publish`
	signing
	id("org.jetbrains.dokka")
	//id("com.acornui.kotlin-mpp") apply false

	// Necessary to avoid the warning:
	// "The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may
	// break the build."
	kotlin("jvm") apply false
	kotlin("js") apply false
	kotlin("multiplatform") apply false
}

buildscript {
	dependencies {
		classpath("com.acornui:gradle-kotlin-plugins:$version")
	}
}

subprojects {
//	apply<MavenPublishPlugin>()
//	apply<SigningPlugin>()
	apply(from = "$rootDir/gradle/mavenPublish.gradle.kts")

	repositories {
		mavenLocal()
		gradlePluginPortal()
		jcenter()
		maven("https://dl.bintray.com/kotlin/kotlin-eap/")
	}

	afterEvaluate {
		tasks {
			withType<TestReport> {
				this.destinationDir = rootProject.buildDir.resolve("reports/${project.name}/")
			}
			withType<Test> {
				this.testLogging {
					this.showStandardStreams = true
				}
				this.reports.html.destination = rootProject.buildDir.resolve("reports/${project.name}/")
			}
		}
	}
}

val publishPluginsToMavenLocal = tasks.register("publishPluginsToMavenLocal") {
	dependsOn(gradle.includedBuild("gradle-kotlin-plugins").task(":publishToMavenLocal"))
}

// All tasks depend on the common kotlin plugins
tasks.configureEach {
	if (name != "publishPluginsToMavenLocal")
		dependsOn(publishPluginsToMavenLocal)
}

for (taskName in listOf("check", "build", "publish", "publishToMavenLocal")) {
	tasks.named(taskName) {
		dependsOn(gradle.includedBuild("gradle-kotlin-plugins").task(":$taskName"))
	}
}

// Publish skins when this project is published.
for (taskName in listOf("publish", "publishToMavenLocal")) {
	val skinTask = tasks.register<GradleBuild>("${taskName}Skins") {
		subprojects.forEach {
			dependsOn(":${it.path}:publishToMavenLocal")
		}
		dir = file("skins")
		tasks = listOf("build", taskName)
		buildName = "${taskName}Skins"
		startParameter.projectProperties = gradle.startParameter.projectProperties + mapOf("version" to version.toString(), "acornVersion" to version.toString())
	}
	tasks.named(taskName) {
		dependsOn(skinTask)
	}
}