/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.build

import com.acornui.build.util.AcornAssets
import com.badlogicgames.packr.Packr
import com.badlogicgames.packr.PackrConfig
import java.io.File

// TODO: bundled jar that can be executed for deploy.

/**
 * A JVM module is for jvm backend modules.
 */
open class JvmModule(

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

	var outWorking = File(out, "working/production/$name/")

	init {
		hasJs = false
	}

	override fun mergeAssets() {
		walkDependenciesBottomUp {
			it.outAssets.copyRecursively(outWorking, onError = BuildUtil.copyIfNewer)
		}
		// Copy version txt
		if (BuildUtil.buildVersion.exists())
			BuildUtil.buildVersion.copyTo(File(outWorking, "assets/build.txt"), overwrite = true)
		AcornAssets.writeManifest(File(outWorking, "assets/"), outWorking)
	}

	/**
	 * Creates a single, runnable jar.
	 */
	open fun oneJar() {

	}

	/**
	 * Uses Packr to create a win64 executable.
	 *
	 * Windows troubleshooting:
	 * If double clicking the exe does nothing, use your.exe -c --console -v  to see the problem.
	 */
	open fun win64() {
		println("Creating executable for $name")
		val config = createBasePackrConfig()
		config.platform = PackrConfig.Platform.Windows64
		config.jdk = "http://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-win_x64.zip"
		config.outDir = File("out-win64")
		Packr().pack(config)
	}

	/**
	 * Uses Packr to create a mac64 executable.
	 */
	open fun mac64() {
		println("Creating dmg for $name")
		val config = createBasePackrConfig()
		config.platform = PackrConfig.Platform.MacOS
		config.jdk = "http://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-macosx_x64.zip"
		config.outDir = File("out-mac")
		Packr().pack(config)
	}

	/**
	 * Uses Packr to create a linux64 executable.
	 */
	open fun linux64() {
		println("Creating for $name")
		val config = createBasePackrConfig()
		config.platform = PackrConfig.Platform.Linux64
		config.jdk = "http://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-linux_x64.tar.gz"
		config.outDir = File("out-linux64")
		Packr().pack(config)
	}

	protected open fun createBasePackrConfig(): PackrConfig {
		val config = PackrConfig()
		config.executable = name

		val libraryFiles = ArrayList<String>()
		walkDependenciesBottomUp {
			libraryFiles.add(it.jvmJar.absolutePath)
			Module.expandLibraryDependencies(it.jvmLibraryDependencies, libraryFiles)
			Module.expandLibraryDependencies(it.jvmRuntimeDependencies, libraryFiles)
		}
		libraryFiles.add(jvmJar.absolutePath)

		val cp = System.getProperty("java.class.path")
		val indexB = cp.indexOf("kotlin-runtime.jar")
		val indexA = cp.lastIndexOf(PATH_SEPARATOR, indexB) + 1
		libraryFiles.add(cp.substring(indexA, indexB + "kotlin-runtime.jar".length))
		config.classpath = libraryFiles
		config.mainClass = mainClass!!
		config.vmArgs = arrayListOf("-Xmx1G")
		config.minimizeJre = "hard"
		config.resources = outWorking.listFiles().toList()
		return config
	}
}