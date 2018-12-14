/*
 * Copyright 2018 Poly Forest, LLC
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

import com.acornui.io.file.FilesManifestSerializer
import com.acornui.jvm.io.file.ManifestUtil
import com.acornui.serialization.JsonSerializer
import java.io.File

object JsSources {

	fun writeManifest(source: File, dest: File, root: File) {
		val manifest = ManifestUtil.createManifest(source, root)
		dest.let {
			it.mkdirs()
			File(it, "files.js").writeText("var manifest = ${JsonSerializer.write(manifest, FilesManifestSerializer)};")
		}
	}
}