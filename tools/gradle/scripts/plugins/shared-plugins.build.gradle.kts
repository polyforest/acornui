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

buildscript {
	val KOTLIN_VERSION: String by gradle.startParameter.projectProperties

	repositories {
		mavenCentral()
	}

	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")
	}
}

apply(plugin = "org.gradle.maven-publish")
apply(plugin = "org.gradle.java-gradle-plugin")

repositories {
	mavenCentral()
}

val acornConfig: MutableMap<String, String> = gradle.startParameter.projectProperties
val ACORNUI_GROUP by acornConfig
group = ACORNUI_GROUP

val ACORNUI_PLUGINS_REPO by acornConfig
afterEvaluate {
	configure<PublishingExtension> {
		repositories {
			maven(url = "$buildDir/repository")
			maven(url = ACORNUI_PLUGINS_REPO)
		}
	}
}

tasks {
	val jar by getting(Jar::class) {
		finalizedBy(":publish")
	}

	val cleanLocalPluginsRepo by creating(Delete::class) {
		group = "build"
		description = "Delete local acornui plugins repo."
		delete(ACORNUI_PLUGINS_REPO)
	}

	val cleanPlugins by creating(Delete::class) {
		group = "build"
		description = "Delete local acornui plugins repo and the build directory."
		dependsOn(":cleanLocalPluginsRepo", ":clean")
	}

	val buildAll by creating(DefaultTask::class) {
		group = "other-helpers"
		description = "Builds this project and all subprojects for composite builds."
		dependsOn(getTasksByName("build", true))
	}

	val cleanAll by creating(Delete::class) {
		group = "other-helpers"
		description = "Deletes build directories of all subprojects for composite builds."
		dependsOn(getTasksByName("clean", true))
	}
}

val GRADLE_VERSION by acornConfig
tasks.withType<Wrapper> {
	gradleVersion = GRADLE_VERSION
	distributionType = Wrapper.DistributionType.ALL
}
