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

@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.util

import com.acornui.build.plugins.tasks.AcornUiResourceProcessorTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Avoids the standard process resources task and adds bitmap font and texture packing capabilities.
 * This will automatically be added to Application projects, but modules must invoke this if they have resources that
 * need processing in this way.
 */
fun Project.configureResourceProcessingTasks() {
	kotlinExt.targets.configureEach {
		compilations.configureEach {
			val processResourcesName = disambiguateName("processResources")
			if (tasks.findByName(processResourcesName) != null) {
				val processAcornResources = tasks.register<AcornUiResourceProcessorTask>(disambiguateName("processAcornResources")) {
					val processResources = tasks.getByName<ProcessResources>(processResourcesName)
					allKotlinSourceSets.map {
						from(it.resources.srcDirs)
					}
					into(processResources.destinationDir)
				}
				tasks.named<ProcessResources>(processResourcesName) {
					dependsOn(processAcornResources)
					enabled = false
				}
			}
		}
	}
}

