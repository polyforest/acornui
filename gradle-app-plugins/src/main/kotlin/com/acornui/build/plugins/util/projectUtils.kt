@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.util

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.concurrent.TimeUnit

fun Project.preventSnapshotDependencyCaching() {
    allprojects {
        configurations.all {
            // Gradle has a bug where snapshots aren't marked as changing when their versions are dynamically applied.
            it.resolutionStrategy {
                it.cacheChangingModulesFor(0, TimeUnit.SECONDS)
                it.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
            }
            dependencies {
                components {
                    it.all {
                        if (it.id.version.endsWith("-SNAPSHOT")) {
                            it.isChanging = true
                        }
                    }
                }
            }
        }
    }
}

val Project.kotlinExt: KotlinMultiplatformExtension
    get() = extensions.getByType()

fun Project.idea(configure: IdeaModel.() -> Unit): Unit =
    extensions.configure("idea", configure)

