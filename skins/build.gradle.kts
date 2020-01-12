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
import com.acornui.build.plugins.tasks.createBitmapFontGeneratorConfig
import com.acornui.build.plugins.tasks.createPackTexturesConfig
import com.acornui.build.plugins.util.addResourceProcessingTasks
import org.gradle.kotlin.dsl.java as javaExt // KT-35888

plugins {
	id("org.gradle.java")
	`maven-publish`
}

buildscript {
	val props = java.util.Properties()
	props.load(projectDir.resolve("../gradle.properties").inputStream())
	val version = props["version"]!!
	dependencies {
		classpath("com.acornui:gradle-app-plugins:$version")
	}
}

val props = java.util.Properties()
props.load(projectDir.resolve("../gradle.properties").inputStream())
version = props["version"]!!

subprojects {
	extra["acornVersion"] = version
	com.acornui.build.AcornDependencies.addVersionProperties(extra)
	apply<JavaPlugin>()
	apply<MavenPublishPlugin>()
	javaExt {
		sourceCompatibility = JavaVersion.VERSION_1_6
		targetCompatibility = JavaVersion.VERSION_1_6
	}
	group = "com.acornui.skins"

	sourceSets {
		main {
			resources {
				setSrcDirs(listOf("resources"))
			}
		}
	}

	publishing {
		publications {
			create<MavenPublication>("maven") {
				groupId = group.toString()
				artifactId = project.name
				version = project.version.toString()

				from(components["java"])
			}
		}
	}

//	createBitmapFontGeneratorConfig()
//	createPackTexturesConfig()

//	val processAcornResources = tasks.create<AcornUiResourceProcessorTask>("processAcornResources") {
//		from(file("resources"))
//		into(buildDir.resolve("processedResources"))
//	}

//	tasks.named<ProcessResources>("processResources") {
////		dependsOn(processAcornResources)
//		enabled = false
//	}
}

