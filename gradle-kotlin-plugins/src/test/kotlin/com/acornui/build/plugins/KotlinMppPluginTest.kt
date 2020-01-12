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

package com.acornui.build.plugins

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.kotlin.dsl.*

class KotlinMppPluginTest {

	@Test
	fun apply() {
		val project = ProjectBuilder.builder().build()
		project.extra["kotlinVersion"] = "1.3.61"
		project.extra["kotlinCoroutinesVersion"] = "1.3.0-RC"
		project.extra["kotlinSerializationVersion"] = "0.14.0"
		project.extra["kotlinLanguageVersion"] = "1.3"
		project.extra["kotlinJvmTarget"] = "1.8"
		project.pluginManager.apply(KotlinMppPlugin::class.java)
		assertTrue(project.extensions.getByName("kotlin") is KotlinMultiplatformExtension)
	}
}