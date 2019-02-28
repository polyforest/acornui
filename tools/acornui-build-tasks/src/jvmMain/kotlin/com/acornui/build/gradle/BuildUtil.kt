/*
 * Copyright 2019 PolyForest
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

package com.acornui.build.gradle

fun main(args: Array<String>) {
	val argMap = ArgumentMap(args)
	val target = getTarget(argMap.get("target", default = Targets.ASSETS.name))
	val usage = """Usage: -target=[assets|asset-manifest|lib-manifest|bust-script-cache] -src=<dir> -dest=<dir> -root=<dir>
		|${"\t"}Note - `-target=assets` does not require root or dest
	""".trimMargin()

	if (target == null) {
		println(usage)
		System.exit(-1)
	}

	with(argMap) {
		when (target) {

			Targets.ASSETS -> run {
				val src = getFileArg("src")
				com.acornui.build.util.AcornAssets.packAssets(src, src)
			}

			Targets.ASSET_MANIFEST -> run {
				val (src, root) = getFileArgs("src", "root")
				// Source directory is also the destination of the generated manifest file
				com.acornui.build.util.AcornAssets.writeManifest(src, root)
			}

			Targets.LIB_MANIFEST -> run {
				val (src, dest, root) = getFileArgs("src", "dest", "root")
				JsSources.writeManifest(src, dest, root)
			}

			else -> null
		}
	}
}

fun getTarget(target: String): Targets? {
	return try {
		Targets.valueOf(target.toUpperCase().replace('-', '_'))
	} catch (e: Throwable) {
		null
	}
}

enum class Targets {
	ASSETS,
	ASSET_MANIFEST,
	LIB_MANIFEST,
	BUST_SCRIPT_CACHE
}
