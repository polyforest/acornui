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

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformCommonPlugin
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JsPluginWrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

buildscript {
	val DOKKA_VERSION: String by gradle.startParameter.projectProperties

	repositories {
		jcenter()
	}

	dependencies {
		classpath("org.jetbrains.dokka:dokka-gradle-plugin:$DOKKA_VERSION")
	}
}

typealias ProjectConsumer<T> = (Project) -> T
val acornConfig: Map<String, String> = gradle.startParameter.projectProperties
val ACORNUI_SHARED_BUILD_PATH by acornConfig
apply(from = ACORNUI_SHARED_BUILD_PATH)

plugins {
	kotlin("jvm")
	id("org.jetbrains.dokka")
}

val TARGET_JVM_VERSION by acornConfig
val acornAllProjects: ProjectConsumer<Unit> by extra
val DEFAULT_ACORNUI_PROJECT_VERSION by acornConfig
fun Project.allProjectConfiguration() {
	with(this) {
		acornAllProjects(this)
		version = with(rootProject.file("version.txt")) {
			if (exists())
				readText().trim()
			else
				"1.0"
//				DEFAULT_ACORNUI_PROJECT_VERSION.also { writeText(it) }
		}

		tasks.withType<KotlinCompile> {
			kotlinOptions {
				jvmTarget = TARGET_JVM_VERSION
				javaParameters = true
				verbose = true
			}
		}
	}
}
project.allProjectConfiguration()

val TARGET_ECMA_VERSION by acornConfig
val commonProjectConfiguration: ProjectConsumer<Unit> by extra
val jsProjectConfiguration: ProjectConsumer<Unit> by extra
val jvmProjectConfiguration: ProjectConsumer<Unit> by extra
subprojects {
	val subProject = this
	subProject.allProjectConfiguration()

	plugins.withType<KotlinPlatformCommonPlugin> {
		commonProjectConfiguration(subProject)
	}

	plugins.withType<Kotlin2JsPluginWrapper> {
		jsProjectConfiguration(subProject)
	}

	plugins.withType<KotlinPluginWrapper> {
		jvmProjectConfiguration(subProject)
	}

	tasks.withType<Kotlin2JsCompile> {
		kotlinOptions {
			// TODO - MP - TEST: Test "noCall" main for acornui-js modules
			// May want noCall.  Not sure what this affects in the target js file
			//            main = "noCall" // or "noCall", "call" is default
			moduleKind = "amd"
			sourceMap = true
            sourceMapEmbedSources = "always"
			target = TARGET_ECMA_VERSION
		}
	}
}

val javaConvention: JavaPluginConvention = convention.getPlugin(JavaPluginConvention::class.java)
val main: SourceSet by sourceSets
tasks {
	val jar by getting(Jar::class) {
		manifest.attributes.apply {
			put("Implementation-Title", project.name)
			put("Implementation-Version", project.version)
			// TODO - MP: Check if needed for downstream apps.  They were taken from a java library example.
		}
	}

	val sourcesJar by creating(Jar::class) {
		dependsOn(JavaPlugin.CLASSES_TASK_NAME)
		group = "build"
		description = "Assembles a jar archive containing the source."
		classifier = "sources"
		from(main.allSource)
	}

	val javadocJar by creating(Jar::class) {
		dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
		group = "build"
		description = "Assembles a jar archive containing the docs."
		classifier = "javadoc"
		from(file(javaConvention.docsDir))
	}

	artifacts {
		add("archives", sourcesJar)
		add("archives", javadocJar)
	}
}

// TODO - MP: Dokka
// https://guides.gradle.org/building-kotlin-jvm-libraries/#step_3_add_docs_with_dokka

//tasks {
//    // API docs generation using dokka
//    val dokka by getting(DokkaTask::class) {
//        dependsOn(gradleApiSources, gradleKotlinDslApiSources, gradlePluginsAccessors)
//        group = null
//        moduleName = "api"
//        outputDirectory = "$buildDir/docs/dokka"
//        jdkVersion = 8
//        classpath = dokkaDependencies
//        sourceDirs = listOf(
//                gradleKotlinDslApiSources.sourceDir,
//                gradleApiSources.sourceDir,
//                gradlePluginsAccessors.accessorsDir)
//        includes = listOf("src/dokka/kotlin-dsl.md")
//        doFirst {
//            file(outputDirectory).deleteRecursively()
//        }
//    }
//}

tasks {
	val writeFileManifest by creating(DefaultTask::class) {
		group = "build"
		description = "Writes file manifest for acornui."
	}

	val packTextures by creating(DefaultTask::class) {
		group = "build"
		description = "Prep textures for use in an acornui app."
	}
}

// TODO - MP: Make publish task that bumps version based on whether someone passes in positional keywords (get them from old version build code)
// TODO - MP: Get build numbers into acorn and demo project
// TODO - MP: Optimize build metadata file not to interfere with build caching
