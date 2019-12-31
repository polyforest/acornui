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

import java.util.Properties

plugins {
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm")
}

val props = Properties()
props.load(projectDir.resolve("../gradle.properties").inputStream())
version = props["version"]!!

repositories {
    jcenter()
    gradlePluginPortal()
}

val kotlinVersion: String by props
val dokkaVersion: String by props

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(gradleApi())
    implementation(kotlin("gradle-plugin", version = kotlinVersion))
    implementation(kotlin("gradle-plugin-api", version = kotlinVersion))
    implementation(kotlin("serialization", version = kotlinVersion))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

    testImplementation(gradleKotlinDsl())
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test", version = kotlinVersion))
    testImplementation(kotlin("test-junit", version = kotlinVersion))
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }
}

gradlePlugin {
    plugins {
        create("kotlinMpp") {
            id = "com.acornui.kotlin-mpp"
            implementationClass = "com.acornui.build.plugins.KotlinMppPlugin"
            displayName = "Kotlin multi-platform configuration for Acorn UI"
            description = "Configures an Acorn UI library project for Kotlin multi-platform."
        }
        create("kotlinJvm") {
            id = "com.acornui.kotlin-jvm"
            implementationClass = "com.acornui.build.plugins.KotlinJvmPlugin"
            displayName = "Kotlin jvm configuration for Acorn UI"
            description = "Configures an Acorn UI library project for Kotlin jvm."
        }
        create("kotlinJs") {
            id = "com.acornui.kotlin-js"
            implementationClass = "com.acornui.build.plugins.KotlinJsPlugin"
            displayName = "Kotlin js configuration for Acorn UI"
            description = "Configures an Acorn UI library project for Kotlin js."
        }
    }
}