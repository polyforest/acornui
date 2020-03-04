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

import org.apache.tools.ant.filters.ReplaceTokens

plugins {
	base
	idea
	`maven-publish`
	signing

	// Necessary to avoid the warning:
	// "The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may
	// break the build."
	kotlin("jvm") apply false
	kotlin("js") apply false
	kotlin("multiplatform") apply false
}

idea {
	module {
		excludeDirs = excludeDirs + file("templates")
	}
}

buildscript {
	dependencies {
		classpath("com.acornui:gradle-kotlin-plugins:$version")
	}
}

subprojects {
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

val buildTemplatesTask = tasks.register<Sync>("buildTemplates") {
	exclude("**/build")
	exclude("**/.idea")
	exclude("**/.gradle")
	into(buildDir.resolve("templates"))
	from("templates") {
		filesMatching("**/*.properties") {
			filter(mapOf("tokens" to mapOf("acornVersion" to version)), ReplaceTokens::class.java)
		}
		filesMatching("**/*.txt") {
			filter(mapOf("tokens" to mapOf("acornVersion" to version)), ReplaceTokens::class.java)
		}
	}
}

val archiveBasicTemplate = tasks.register<Zip>("archiveBasicTemplate") {
	exclude("**/build")
	exclude("**/.idea")
	exclude("**/.gradle")
	group = "publishing"
	dependsOn(buildTemplatesTask)
	archiveBaseName.set("acornUi")
	from(buildDir.resolve("templates/basic"))
}

tasks.named("publishToMavenLocal") {
	dependsOn(archiveBasicTemplate)
}

tasks.named("publish") {
	dependsOn(archiveBasicTemplate)
}