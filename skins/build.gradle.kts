@file:Suppress("UnstableApiUsage")

/*
* Copyright 2020 Poly Forest, LLC
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

import com.acornui.build.plugins.tasks.AcornUiResourceProcessorTask
import com.acornui.build.util.delegateLifecycleTasksToSubProjects
import org.gradle.kotlin.dsl.java as javaExt // KT-35888

plugins {
	id("org.gradle.java")
	id("com.acornui.root")
	id("com.acornui.app") apply false
	signing
	`maven-publish`
}

allprojects {
	group = "com.acornui.skins"
	logger.lifecycle("Skins version $group:$name:$version")
}

subprojects {
	apply(from = "$rootDir/../gradle/mavenPublish.gradle.kts")
	extra["acornVersion"] = version
	apply<JavaPlugin>()
	version = rootProject.version

	sourceSets {
		main {
			resources {
				setSrcDirs(listOf("resources"))
			}
		}
	}

	val processAcornResources = tasks.register<AcornUiResourceProcessorTask>("processAcornResources") {
		from(file("resources"))
		into(tasks.getByName<ProcessResources>("processResources").destinationDir)
	}

	tasks.getByName("processResources") {
		dependsOn(processAcornResources)
		enabled = false
	}

	delegateLifecycleTasksToSubProjects()

	publishing {
		publications {
			create<MavenPublication>("default") {
				from(components["java"])
			}
		}
	}
}

extra