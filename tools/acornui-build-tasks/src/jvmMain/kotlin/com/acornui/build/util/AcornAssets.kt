/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.build.util

import com.acornui.core.asset.AssetManager
import com.acornui.core.di.inject
import com.acornui.core.io.JSON_KEY
import com.acornui.core.io.file.Files
import com.acornui.io.file.FilesManifestSerializer
import com.acornui.jvm.JvmHeadlessApplication
import com.acornui.jvm.io.file.ManifestUtil
import com.acornui.serialization.JsonSerializer
import com.acornui.serialization.write
import com.acornui.texturepacker.jvm.TexturePackerUtil
import java.io.File


object AcornAssets {

	fun packAssets(source: File, dest: File) {
		println("Source directory: " + source.path)
		if (source != dest) {
			println("Assets directory: " + dest.path)
			copyAssets(source, dest)
		}

		JvmHeadlessApplication(dest.path).start {
			// Pack the assets in all directories in the dest folder with a name ending in "_unpacked"
			TexturePackerUtil(inject(Files), inject(AssetManager), inject(JSON_KEY)).packAssets(dest, File("."))

			dest.setLastModified(System.currentTimeMillis())
		}
	}

	fun writeManifest(dest: File, root: File) {
		println("Creating manifest")
		val manifest = ManifestUtil.createManifest(dest, root)
		File(dest, "files.json").writeText(JsonSerializer.write(manifest, FilesManifestSerializer))
	}

	private fun copyAssets(source: File, destination: File) {
		if (!source.exists()) {
			throw Exception(source.path + " does not exist.")
		}
		if (destination.exists()) {
			destination.delete()
		}
		destination.mkdir()
		source.copyRecursively(destination, true)
	}

}