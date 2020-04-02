/*
 * Copyright 2020 Poly Forest, LLC
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

rootProject.name = "basic-acorn-project"

pluginManagement {
	val acornVersion: String by settings
	buildscript {
		repositories {
			if (acornVersion.endsWith("-SNAPSHOT")) {
				mavenLocal()
				maven("https://oss.sonatype.org/content/repositories/snapshots")
			}
			mavenCentral()
			jcenter()
		}
//		dependencies {
//			classpath("com.acornui:gradle-app-plugins:$acornVersion")
//		}
	}
}
apply(plugin = "com.acornui.settings")