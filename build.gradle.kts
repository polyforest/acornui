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

	// Necessary to avoid the warning:
	// "The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may
	// break the build."
	kotlin("jvm") apply false
	kotlin("js") apply false
	kotlin("multiplatform") apply false

//	id("org.jetbrains.dokka")
}

buildscript {
	val kotlinVersion: String by project
	dependencies {
		classpath("com.acornui:gradle-kotlin-plugins:$version")
		classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$kotlinVersion")
	}
}

val kotlinVersion: String by project
allprojects {
	configurations.configureEach {
		resolutionStrategy {
			dependencySubstitution.all {
				val requested = requested
				if (requested is ModuleComponentSelector) {
					if (requested.group == "org.jetbrains.kotlin") {
						useTarget("${requested.group}:${requested.module}:$kotlinVersion")
					}
				}
			}
		}
	}
}

subprojects {
	apply<MavenPublishPlugin>()

	repositories {
		mavenLocal()
		gradlePluginPortal()
		jcenter()
		maven("https://dl.bintray.com/kotlin/kotlin-eap/")
	}

	publishing {
		repositories {
			maven("https://maven.pkg.github.com/polyforest/acornui") {
				credentials {
					username = project.findProperty("githubActor") as String? ?: System.getenv("GITHUB_ACTOR")
					password = project.findProperty("githubToken") as String? ?: System.getenv("GITHUB_TOKEN")
				}
			}
		}
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

// Publish skins when this project is published.
for (taskName in listOf("publish", "publishToMavenLocal")) {
	val skinTask = tasks.register<GradleBuild>("${taskName}Skins") {
		subprojects.forEach {
			dependsOn(":${it.path}:publishToMavenLocal")
		}
		dir = file("skins")
		tasks = listOf("build", taskName)
		buildName = "${taskName}Skins"
		val githubActor: String by project
		val githubToken: String by project
		startParameter.projectProperties = mapOf("version" to version.toString(), "githubActor" to githubActor, "githubToken" to githubToken)
	}
	tasks.named(taskName) {
		dependsOn(skinTask)
	}
}