package com.acornui.build

import com.acornui.build.util.AcornAssets
import com.acornui.build.util.clean
import com.acornui.build.util.sourcesAreNewer
import java.io.File

/**
 * A core module compiles to both js and jvm, and runs the asset tools such as the texture packer on the asset
 * directory.
 */
open class CoreModule(

		/**
		 * The base directory of the module. (Must exist)
		 */
		baseDir: File,

		/**
		 * The name of the module, will be used when matching command line parameters.
		 */
		name: String = baseDir.name,

		/**
		 * The directory for compilation output.
		 */
		out: File = File("out"),

		/**
		 * The distribution directory for jars and compiled assets.
		 */
		dist: File = File("dist")
) : Module(baseDir, name, out, dist) {

	override fun buildAssets() {
		if (sourcesAreNewer(resources, outAssets)) {
			outAssets.clean()
			for (resDir in resources) {
				if (resDir.exists())
					AcornAssets.packAssets(resDir, outAssets, outAssets.parentFile!!)
			}
		}
	}
}