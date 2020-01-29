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

import java.util.*
import org.gradle.kotlin.dsl.java as javaExt // KT-35888

buildscript {

	// Gradle doesn't support sharing property files between multiple included builds.
	// This approach of loading the properties and setting them on extra if they weren't already set seems to be better
	// than duplicating or sym-linking the property files.

	val props = java.util.Properties()
	props.load(File("$rootDir/../gradle.properties").inputStream())
	for (entry in props.entries) {
		val key = entry.key.toString()
		val value = entry.value.toString()
		if (!extra.has(key))
			extra[key] = value
	}
	gradle.allprojects {
		for (entry in props.entries) {
			val key = entry.key.toString()
			val value = entry.value.toString()
			if (!extra.has(key) && project.findProperty(key) == null)
				extra[key] = value
		}
		if (version == Project.DEFAULT_VERSION)
			version = props["version"]!!
		if (group.toString().isEmpty())
			group = props["group"]!!
	}
}