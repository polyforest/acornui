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
	id("com.acornui.plugins.kotlin-jvm")
}

val kotlinJvmTarget: String by extra
val kotlinLanguageVersion: String by extra

kotlin {
	sourceSets {
		named("jvmMain") {
			dependencies {
				implementation("com.acornui:acornui-core")
				implementation("com.acornui:acornui-utils")

				val lwjglVersion: String by extra
				val jorbisVersion: String by extra
				val jlayerVersion: String by extra
				val lwjglGroup = "org.lwjgl"
				val lwjglName = "lwjgl"
				val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd", "tinyfd")

				implementation("$lwjglGroup:$lwjglName:$lwjglVersion")
				extensions.forEach { implementation("$lwjglGroup:$lwjglName-$it:$lwjglVersion") }
				implementation("com.badlogicgames.jlayer:jlayer:$jlayerVersion-gdx")
				implementation("org.jcraft:jorbis:$jorbisVersion")
			}
		}
		named("jvmTest") {
			dependencies {
				val mockitoVersion: String by extra
				val objenesisVersion: String by extra
				implementation("org.mockito:mockito-core:$mockitoVersion")
				implementation("org.objenesis:objenesis:$objenesisVersion")
			}
		}
	}

	// Lwjgl backend doesn't need metadata publication:
	metadata {
		mavenPublication {
			val targetPublication = this@mavenPublication
			tasks.withType<AbstractPublishToMaven>()
				.matching { it.publication == targetPublication }
				.all { onlyIf { false } }
		}
	}
}