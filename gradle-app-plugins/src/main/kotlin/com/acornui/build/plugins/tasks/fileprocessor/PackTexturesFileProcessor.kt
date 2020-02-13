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

package com.acornui.build.plugins.tasks.fileprocessor

import com.acornui.texturepacker.packAssets
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import java.io.File
import kotlin.time.Duration
import kotlin.time.minutes

class PackTexturesFileProcessor : DirectoryChangeProcessorBase() {

	@Input
	var suffix = "_unpacked"
		set(value) {
			field = value
			directoryRegex = Regex(".*$suffix$")
		}

	override var directoryRegex: Regex = Regex(".*$suffix$")

	/**
	 * If the assets fail to pack within this duration, an exception will be thrown.
	 */
	var timeout: Duration = 10.minutes

	private val packedExtensions = arrayOf("json", "png")

	override fun process(sourceDir: File, destinationDir: File, task: Task) {
		if (sourceDir.exists()) {
			task.logger.lifecycle("Packing assets: ${sourceDir.path} dest: ${destinationDir.parentFile.path}")
			packAssets(sourceDir, destinationDir.parentFile, suffix, timeout)
		} else {
			task.logger.lifecycle("Removing packed assets: " + sourceDir.path)
			val name = sourceDir.name.removeSuffix(suffix)
			destinationDir.parentFile.listFiles()?.forEach { child ->
				if (child.name.startsWith(name) && packedExtensions.contains(child.extension.toLowerCase()))
					child.delete()
			}
		}
	}
}