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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.testkit.runner.TaskOutcome.*
import java.io.File
import kotlin.test.assertEquals

class AcornUiApplicationPluginTest {

	@Rule
	@JvmField
	var testProjectDir: TemporaryFolder = TemporaryFolder()

	@Test fun addsRunJvmTask() {
		val project = ProjectBuilder.builder().build()
		project.extra["acornVersion"] = "test"
		project.pluginManager.apply("com.acornui.root")
		project.pluginManager.apply("com.acornui.app")
		assertTrue(project.tasks.getByName("runJvm") is RunJvmTask)
		assertNotNull(project.plugins.findPlugin("org.jetbrains.kotlin.multiplatform"))
	}

	@Test fun jsProcessResourcesTask() {

		println(File(".").absolutePath)
		println(File("build/resources/test/basicAcornProject/").absolutePath)
		assertTrue(File("build/resources/test/basicAcornProject/").exists())
//
//		println(File("test/resources/basicAcornProject").absolutePath)

		val buildFileContent = """
			plugins {
				id("com.acornui.root")
				id("com.acornui.app")
			}
		"""
		val buildFile = testProjectDir.newFile("build.gradle.kts")
		buildFile.writeText(buildFileContent)

		testProjectDir.newFile("gradle.properties").writeText("""
			acornVersion=+
		""".trimIndent())



		val result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments("jsProcessResources")
				.withPluginClasspath()
				.build()

//		assertTrue(result.getOutput().contains("Hello world!"))
//		println("result.getOutput() ${result.output}")
//		println("result.task(\":jsProcessResources\")!!.outcome ${result.task(":jsProcessResources")!!.outcome}")
//		assertEquals(SUCCESS, result.task(":jsProcessResources")!!.outcome)
	}
}