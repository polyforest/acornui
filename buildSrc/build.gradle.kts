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

plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

val kotlinVersion: String by extra
dependencies {
    implementation(kotlin("gradle-plugin", version = kotlinVersion))
    implementation("com.jcraft:jsch:0.1.55")
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
            displayName = "Kotlin jvm configuration for Acorn UI"
            implementationClass = "com.acornui.build.plugins.KotlinJvmPlugin"
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