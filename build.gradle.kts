import com.acornui.build.AcornDependencies
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult


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

val kotlinJvmTarget: String by extra
val kotlinLanguageVersion: String by extra

plugins {
	kotlin("multiplatform")
	`maven-publish`
	id("org.jetbrains.dokka")
}

subprojects {
	apply {
		plugin("org.gradle.maven-publish")
	}
}

allprojects {
	AcornDependencies.addVersionProperties(extra)

	repositories {
		jcenter()
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			languageVersion = kotlinLanguageVersion
			apiVersion = kotlinLanguageVersion
		}
	}

	// Inter-project dependencies are not expressed as project(id) because at the time of writing, IDEA cannot handle
	// multi-platform composite builds.  (KT-30285) So as a workaround, projects needing to work alongside acorn code
	// will include acorn libraries individually via include() and not includeBuild().
	configurations.all {
		resolutionStrategy.dependencySubstitution.all {
			requested.let { r ->
				if (r is ModuleComponentSelector && r.group.startsWith("com.acornui")) {
					arrayOf("", "tools:", "backends:", "build-libs:").firstNotNullResult {
						findProject(":$it${r.module}")
					}?.let { targetProject ->
						useTarget(targetProject)
					}
				}
			}
		}
	}

	publishing {
		repositories {
			maven {
				url = uri(rootProject.buildDir.resolve("artifacts"))
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