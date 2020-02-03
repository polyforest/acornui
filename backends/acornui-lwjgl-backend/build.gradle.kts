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
	id("com.acornui.kotlin-jvm")
}

kotlin {
	sourceSets {
		named("jvmMain") {
			dependencies {
				implementation(project(":acornui-utils"))
				implementation(project(":acornui-core"))

				val lwjglVersion: String by project
				val jorbisVersion: String by project
				val jlayerVersion: String by project
				val lwjglGroup = "org.lwjgl"
				val lwjglName = "lwjgl"
				val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd", "tinyfd")

				implementation("$lwjglGroup:$lwjglName:$lwjglVersion")
				extensions.forEach { implementation("$lwjglGroup:$lwjglName-$it:$lwjglVersion") }
				implementation("com.badlogicgames.jlayer:jlayer:$jlayerVersion-gdx")
				implementation("org.jcraft:jorbis:$jorbisVersion")

				for (os in listOf("linux", "macos", "windows")) {
					runtimeOnly("$lwjglGroup:$lwjglName:$lwjglVersion:natives-$os")
					extensions.forEach {
						runtimeOnly("$lwjglGroup:$lwjglName-$it:$lwjglVersion:natives-$os")
					}
				}
			}
		}
		named("jvmTest") {
			dependencies {
			}
		}
	}
}