@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.util

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinJsSubTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.util.concurrent.TimeUnit

fun Project.preventSnapshotDependencyCaching() {
	allprojects {
		configurations.all {
			// Gradle has a bug where snapshots aren't marked as changing when their versions are dynamically applied.
			resolutionStrategy {
				cacheChangingModulesFor(0, TimeUnit.SECONDS)
				cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
			}
			dependencies {
				components {
					all {
						if (id.version.endsWith("-SNAPSHOT")) {
							isChanging = true
						}
					}
				}
			}
		}
	}
}

fun Project.getRunnableCompilation(target: String, compilationName: String): AbstractKotlinCompilationToRunnableFiles<*> {
	val unconfiguredDepError = "Target platform \"$target\" was not found for $displayName. Ensure that this dependency applies a kotlin multiplatform plugin."
	val kotlinTarget: KotlinTarget = project.kotlinExt.targets.named(target).orNull
			?: error(unconfiguredDepError)
	return (kotlinTarget.compilations.named(compilationName).orNull
			?: error(unconfiguredDepError)) as AbstractKotlinCompilationToRunnableFiles<*>
}

val Project.kotlinExt: KotlinMultiplatformExtension
	get() = extensions.getByType()

val NamedDomainObjectCollection<KotlinTarget>.jvm: KotlinJvmTarget
	get() = getByName("jvm") as KotlinJvmTarget

val NamedDomainObjectCollection<KotlinTarget>.js: KotlinJsTarget
	get() = getByName("js") as KotlinJsTarget

fun Project.idea(configure: IdeaModel.() -> Unit): Unit =
		extensions.configure("idea", configure)

fun Project.jvmCompilation(compilationName: String, configure: KotlinDependencyHandler.() -> Unit): KotlinJvmCompilation {
	return kotlinExt.targets.jvm.compilations.create(compilationName) {
		dependencies(configure)
	}
}