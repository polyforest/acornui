@file:Suppress("UnstableApiUsage")

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
	kotlin("jvm")
}

val gdxVersion: String by extra
dependencies {
	implementation(kotlin("stdlib"))
	implementation(kotlin("stdlib-jdk8"))
	implementation("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
	implementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
	runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
	runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")

	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))
}

publishing {
	publications {
		create<MavenPublication>("default") {
			from(components["java"])
		}
	}
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}