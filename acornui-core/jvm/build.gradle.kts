/*
 * Copyright 2018 Poly Forest, LLC
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
    kotlin("platform.jvm")
}

val lwjglVersion = "3.1.6"
val lwjglGroup = "org.lwjgl"
val lwjglName = "lwjgl"
val natives = arrayOf("windows", "macos", "linux")
val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd")

dependencies {
    expectedBy(project(":acornui-core:acornui-core-common"))

    implementation(project(":tools:acornui-utils:acornui-utils-jvm"))
    implementation(lwjglGroup, lwjglName, lwjglVersion)
    extensions.forEach { implementation(lwjglGroup,"$lwjglName-$it", lwjglVersion) }
    implementation("com.badlogicgames.jlayer:jlayer:1.0.2-gdx")
    implementation("org.jcraft:jorbis:0.0.17")

    for (native in natives) {
        runtimeOnly(lwjglGroup, lwjglName, lwjglVersion, classifier = "natives-$native")
        extensions.forEach { runtimeOnly(lwjglGroup, "$lwjglName-$it", lwjglVersion, classifier = "natives-$native") }
    }

    testImplementation(project(":tools:test-utils:test-utils-jvm"))
    testImplementation(kotlin("reflect"))
}
