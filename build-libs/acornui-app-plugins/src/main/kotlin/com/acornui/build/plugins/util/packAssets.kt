package com.acornui.build.plugins.util

import com.acornui.async.launch
import com.acornui.asset.AssetManager
import com.acornui.di.inject
import com.acornui.io.file.Files
import com.acornui.jvm.JvmHeadlessApplication
import com.acornui.serialization.json
import com.acornui.texturepacker.AcornTexturePacker
import com.acornui.texturepacker.jvm.writer.JvmTextureAtlasWriter
import java.io.File

fun packAssets(srcDir: File, destDir: File, unpackedSuffix: String) {
    JvmHeadlessApplication(srcDir.path).start {
        val files = inject(Files)
        val assets = inject(AssetManager)

        val writer = JvmTextureAtlasWriter()
        val rel = srcDir.toRelativeString(File(".").absoluteFile)
        val dirEntry = files.getDir(rel) ?: error("Could not resolve directory $rel")

        val atlasName = srcDir.name.removeSuffix(unpackedSuffix)
        launch {
            val packedData = AcornTexturePacker(assets, json).pack(dirEntry, quiet = true)
            writer.writeAtlas("$atlasName.json", "$atlasName{0}", packedData, destDir)
        }
    }
}