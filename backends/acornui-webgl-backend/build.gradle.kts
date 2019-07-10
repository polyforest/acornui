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

plugins {
    id("com.acornui.plugins.kotlin-js")
}

kotlin {
    sourceSets {
        named("jsMain") {
            dependencies {
                implementation("com.acornui:acornui-core")
                implementation("com.acornui:acornui-utils")
            }
        }
    }

    // Webgl backend doesn't need metadata publication:
    metadata {
        mavenPublication {
            val targetPublication = this@mavenPublication
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .all { onlyIf { false } }
        }
    }
}
