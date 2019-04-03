/*
 * Copyright 2019 PolyForest
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
    id("com.polyforest.acornui.basic")
    `maven-publish`
}

val MOCKITO_VERSION: String by extra
val OBJENESIS_VERSION: String by extra
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":acornui-utils"))
            }
        }
        commonTest {
            dependencies {
                implementation(project(":acornui-test-utils"))
            }
        }
        named("jvmTest") {
            dependencies {
                implementation(kotlin("reflect"))
                implementation("org.mockito:mockito-core:$MOCKITO_VERSION")
                implementation("org.objenesis:objenesis:$OBJENESIS_VERSION")
            }
        }
    }
}
