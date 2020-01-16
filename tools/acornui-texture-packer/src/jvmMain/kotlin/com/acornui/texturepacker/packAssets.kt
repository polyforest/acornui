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

package com.acornui.texturepacker

import com.acornui.AppConfig
import com.acornui.asset.Loaders
import com.acornui.di.inject
import com.acornui.graphic.exit
import com.acornui.headless.headlessApplication
import com.acornui.io.file.Path
import com.acornui.texturepacker.writer.writeAtlas
import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) {
	val srcDir = File(args[0])
	val destDir = File(args[1])
	val unpackedSuffix: String = args.getOrNull(2) ?: "_unpacked"
	packAssets(srcDir, destDir, unpackedSuffix)
}

fun packAssets(srcDir: File, destDir: File, unpackedSuffix: String = "_unpacked") = runBlocking {
	headlessApplication(AppConfig()) {
		val atlasName = srcDir.name.removeSuffix(unpackedSuffix)
		val packer = AcornTexturePacker(inject(Loaders.textLoader), inject(Loaders.rgbDataLoader))
		runBlocking {
			val packedData = packer.pack(collectSrcPaths(srcDir), quiet = true)
			writeAtlas("$atlasName.json", "$atlasName{0}", packedData, destDir)
			exit()
		}
	}
}

fun collectSrcPaths(srcDir: File): List<Path> {
	val out = ArrayList<Path>()
	out.addAll(srcDir.walkTopDown().map { Path(it.path) })
	return out
}