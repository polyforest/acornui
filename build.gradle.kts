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
	`maven-publish`
	id("org.jetbrains.dokka")
}

subprojects {
	apply {
		plugin("org.gradle.maven-publish")
	}
}

allprojects {

	repositories {
		gradlePluginPortal()
		jcenter()
	}

	publishing {
		repositories {
			maven {
				url = uri(rootProject.buildDir.resolve("artifacts"))
			}
		}
	}

	afterEvaluate {
		tasks {
			withType<TestReport> {
				this.destinationDir = rootProject.buildDir.resolve("reports/${this@allprojects.name}/")
			}
			withType<Test> {
				this.testLogging {
					this.showStandardStreams = true
				}
				this.reports.html.destination = rootProject.buildDir.resolve("reports/${this@allprojects.name}/")
			}
		}
	}
}

tasks {
	dokka {
		outputDirectory = "${project.buildDir}/dokka/$version"
		reportUndocumented = false
		kotlinTasks {
			// dokka fails to retrieve sources from MPP-tasks so they must be set empty to avoid exception
			emptyList()
		}
	}
}

val cleanArtifacts = tasks.register<Delete>("cleanArtifacts") {
	group = "publishing"
	delete(rootProject.buildDir.resolve("artifacts"))
}