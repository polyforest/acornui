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

import com.acornui.build.plugins.util.RunJvmTask
import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class AcornUiApplicationPluginTest {

	@Test fun addsRunJvmTask() {
		val project = ProjectBuilder.builder().build()
		project.extra["acornVersion"] = "test"
		project.pluginManager.apply("com.acornui.root")
		project.pluginManager.apply("com.acornui.app")
		assertTrue(project.tasks.getByName("runJvm") is RunJvmTask)
	}
}