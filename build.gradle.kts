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
	kotlin("multiplatform") apply false
	`maven-publish`
	idea
}

subprojects {
	apply {
		plugin("org.gradle.maven-publish")
	}
}

allprojects {
	apply {
		plugin("org.gradle.idea")
	}
	repositories {
		jcenter()
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			languageVersion = kotlinLanguageVersion
			apiVersion = kotlinLanguageVersion
		}
	}

	configurations.all {
		resolutionStrategy.dependencySubstitution.all {
			requested.let { r ->
				if (r is ModuleComponentSelector && r.group == group) {
					arrayOf("", "tools:", "backends:").firstNotNullResult {
						findProject(":$it${r.module}")
					}?.let { targetProject ->
						useTarget(targetProject)
					}
				}
			}
		}
	}

	val acornUiGradlePluginRepository: String? by extra
	if (acornUiGradlePluginRepository != null) {
		publishing {
			repositories {
				maven {
					url = uri(acornUiGradlePluginRepository!!)
				}
			}
		}

		tasks.register<Delete>("cleanSnapshots") {
			logger.lifecycle("Deleting old snapshots ${project.name} ${project.version}")
			delete(fileTree(acornUiGradlePluginRepository!!).matching {
				include("**/com/acornui/${project.name}-*/${project.version}/**")
			})
		}

		tasks.publish.configure {
			dependsOn("cleanSnapshots")
		}
	}
}