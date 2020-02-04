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

import javax.inject.Inject

plugins {
	kotlin("jvm")
	id("org.gradle.java")
}

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation(project(":gradle-app-plugins"))
	implementation(gradleKotlinDsl())
	implementation(gradleTestKit())
}

tasks {
	compileKotlin {
		kotlinOptions.jvmTarget = "1.8"
	}
	compileTestKotlin {
		kotlinOptions.jvmTarget = "1.8"
	}
}

val skinsProject = project

subprojects {
	group = "com.acornui.skins"
	apply<JavaPlugin>()

	sourceSets {
		main {
			resources {
				setSrcDirs(listOf("resources"))
			}
		}
	}

	val processAcornResources = tasks.register<SkinsResourceProcessorTask>("processAcornResources") {
		dependsOn(skinsProject.tasks.findByName("classes"))
		from(file("resources"))
		into(tasks.getByName<ProcessResources>("processResources").destinationDir)

		val compilation = skinsProject.kotlin.target.compilations["main"]
		classpath = skinsProject.files(
				compilation.runtimeDependencyFiles,
				compilation.output.allOutputs
		)
	}

	tasks.getByName("processResources") {
		dependsOn(processAcornResources)
		enabled = false
	}

	publishing {
		publications {
			create<MavenPublication>("default") {
				from(components["java"])
			}
		}
	}
}

val Project.kotlinExt: org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
	get() = extensions.getByType()

/**
 * To process the resources in the skin projects and publish them, the skins project must have access to the
 * acorn resources task.
 * At the time of writing, you cannot add a project() build dependency in gradle.
 *
 * Our options are:
 * 	1) Composite builds
 * 	2) Using GradleBuild tasks
 * 	3) JavaExec task using gradle test kit
 *
 * 	We chose #3 because options #1 and #2 have difficulties with shared configuration, integrating with the
 * 	multi-project build, and IDEA support.
 *
 *
 */
open class SkinsResourceProcessorTask @javax.inject.Inject constructor(objects: ObjectFactory) : DefaultTask() {

	@Incremental
	@PathSensitive(PathSensitivity.RELATIVE)
	@InputFiles
	val sources: ConfigurableFileCollection = objects.fileCollection()

	/**
	 * Adds the given file tree to the list of sources.
	 */
	fun from(tree: FileTree): SkinsResourceProcessorTask {
		sources.from(tree)
		return this
	}

	fun from(source: File): SkinsResourceProcessorTask {
		sources.from(source)
		return this
	}

	fun from(sources: Iterable<File>): SkinsResourceProcessorTask {
		this.sources.from(sources)
		return this
	}

	fun into(directory: File?) {
		outputDir.set(directory)
	}

	fun into(directory: Directory?) {
		outputDir.set(directory)
	}

	@get:OutputDirectory
	val outputDir: DirectoryProperty = objects.directoryProperty()

	@Classpath
	var classpath: FileCollection = objects.fileCollection()

	@Inject
	protected open fun getExecActionFactory(): org.gradle.process.internal.ExecActionFactory =
			throw UnsupportedOperationException()

	@TaskAction
	fun execute(inputChanges: InputChanges) {
		getExecActionFactory().newJavaExecAction().apply {
			classpath = this@SkinsResourceProcessorTask.classpath
			workingDir = project.projectDir
			main = "com.acornui.skins.MainKt"

			val changes = inputChanges.getFileChanges(sources).joinToString(";") { change: FileChange ->
				listOf(change.file.absolutePath, change.changeType, change.fileType, change.normalizedPath).joinToString(",")
			}
			args = listOf(outputDir.asFile.get().absolutePath, changes)
			execute()
		}
	}
}