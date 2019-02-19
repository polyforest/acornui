/*
 * Copyright 2019 Nicholas Bilyk
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

import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

val KOTLIN_LANGUAGE_VERSION: String by extra
val KOTLIN_JVM_TARGET: String by extra
kotlin {
    js {
        compilations.named("test") {
            runtimeDependencyFiles
        }
        compilations.all {
            kotlinOptions {
                moduleKind = "amd"
                sourceMap = true
                sourceMapEmbedSources = "always"
                main = "noCall"
            }
        }
    }
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = KOTLIN_JVM_TARGET
            }
        }
    }
    targets.all {
        compilations.all {
            kotlinOptions {
                languageVersion = KOTLIN_LANGUAGE_VERSION
                apiVersion = KOTLIN_LANGUAGE_VERSION
                verbose = true
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":acornui-core"))
                implementation(project(":acornui-utils"))
                implementation(project(":backends:acornui-lwjgl-backend"))
                implementation(project(":tools:acornui-texture-packer"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        named("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("compiler"))
            }
        }
        named("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        named("jsMain") {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        named("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
