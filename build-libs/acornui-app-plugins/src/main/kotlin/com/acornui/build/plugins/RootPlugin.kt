package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.preventSnapshotDependencyCaching
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.repositories

@Suppress("unused")
class RootPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            val acornUiHome: String? by extra
            val acornVersion: String by extra
            val isComposite = acornUiHome != null && file(acornUiHome!!).exists()
            val r = this
            logger.lifecycle("isComposite: $isComposite")

            preventSnapshotDependencyCaching()

            target.pluginManager.apply("org.jetbrains.dokka")

            allprojects {
                AcornDependencies.addVersionProperties(extra)
                
                project.pluginManager.apply("org.gradle.idea")

                repositories {
                    mavenLocal()
                    jcenter()

                    maven {
                        url = uri("https://artifacts.acornui.com/mvn/")
                    }
                }

                configurations.all {
                    resolutionStrategy {
                        eachDependency {
                            when {
                                requested.group.startsWith("com.acornui") -> {
                                    useVersion(acornVersion)
                                }
                            }
                        }
                    }
                }

                if (isComposite) {
                    configurations.all {
                        resolutionStrategy.dependencySubstitution {
                            listOf("utils", "core", "game", "spine", "test-utils").forEach {
                                val id = ":acornui:acornui-$it"
                                if (findProject(id) != null) {
                                    r.project(id) {
                                        group = "com.acornui"
                                        version = acornVersion
                                    }
                                    substitute(module("com.acornui:acornui-$it")).with(project(":acornui:acornui-$it"))
                                }
                            }
                            listOf("lwjgl", "webgl").forEach {
                                val id = ":acornui:backends:acornui-$it-backend"
                                if (findProject(id) != null) {
                                    r.project(id) {
                                        group = "com.acornui"
                                        version = acornVersion
                                    }
                                    substitute(module("com.acornui:acornui-$it-backend")).with(project(id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}