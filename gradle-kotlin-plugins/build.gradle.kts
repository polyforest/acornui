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

@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import org.gradle.kotlin.dsl.java as javax

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    signing
}

apply(from = "../gradle/mavenPublish.gradle.kts")
apply(plugin = "kotlin-sam-with-receiver")

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.samWithReceiver(configure: SamWithReceiverExtension.() -> Unit): Unit = extensions.configure("samWithReceiver", configure)

val version: String by project
val group: String by project
project.version = version
project.group = group

logger.lifecycle("Kotlin plugins $group:$name:$version")

repositories {
    jcenter()
    gradlePluginPortal()
//    maven("https://dl.bintray.com/kotlin/kotlin-eap/")
}

val kotlinVersion: String by project
val dokkaVersion: String by project

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(gradleApi())
    implementation(kotlin("gradle-plugin", version = kotlinVersion))
    implementation(kotlin("gradle-plugin-api", version = kotlinVersion))
    implementation(kotlin("serialization", version = kotlinVersion))
//    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

    testImplementation(gradleKotlinDsl())
    testImplementation(kotlin("test", version = kotlinVersion))
    testImplementation(kotlin("test-junit", version = kotlinVersion))
}

kotlin {
    sourceSets.configureEach {
        languageSettings.apply {
            enableLanguageFeature("InlineClasses")
            useExperimentalAnnotation("kotlin.Experimental")
            useExperimentalAnnotation("kotlin.time.ExperimentalTime")
        }
    }
    target {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }
}

javax {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create("kotlinMpp") {
            id = "com.acornui.kotlin-mpp"
            implementationClass = "com.acornui.build.plugins.KotlinMppPlugin"
            displayName = "Kotlin multi-platform configuration for Acorn UI"
            description = "Configures a project for Kotlin multi-platform builds using JS and JVM targets."
        }
        create("kotlinJvm") {
            id = "com.acornui.kotlin-jvm"
            implementationClass = "com.acornui.build.plugins.KotlinJvmPlugin"
            displayName = "Kotlin jvm configuration for Acorn UI"
            description = "Configures a project for Kotlin multi-platform builds using the JVM target."
        }
        create("kotlinJs") {
            id = "com.acornui.kotlin-js"
            implementationClass = "com.acornui.build.plugins.KotlinJsPlugin"
            displayName = "Kotlin js configuration for Acorn UI"
            description = "Configures a project for Kotlin multi-platform builds using the JS target."
        }
    }
}