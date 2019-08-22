package com.acornui.build.plugins.util

import com.acornui.asset.Loaders
import com.acornui.async.globalLaunch
import com.acornui.di.inject
import com.acornui.graphic.exit
import com.acornui.headless.JvmHeadlessApplication
import com.acornui.io.file.Files
import com.acornui.texturepacker.AcornTexturePacker
import com.acornui.texturepacker.jvm.writer.JvmTextureAtlasWriter
import java.io.File

fun packAssets(srcDir: File, destDir: File, unpackedSuffix: String) {
    JvmHeadlessApplication(srcDir.path).start {
        val files = inject(Files)
        val writer = JvmTextureAtlasWriter()
        val rel = srcDir.absoluteFile.toRelativeString(File(".").absoluteFile)
        val dirEntry = files.getDir(rel) ?: error("Could not resolve directory from manifest. \n\tRelative path: $rel\n\tsrcDir: ${srcDir.absolutePath}")

		val atlasName = srcDir.name.removeSuffix(unpackedSuffix)
		globalLaunch {
			val packedData = AcornTexturePacker(inject(Loaders.textLoader), inject(Loaders.rgbDataLoader)).pack(dirEntry, quiet = true)
			writer.writeAtlas("$atlasName.json", "$atlasName{0}", packedData, destDir)
			exit()
		}
	}
}