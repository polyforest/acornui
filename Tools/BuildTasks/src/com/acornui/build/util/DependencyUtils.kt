/*
 * Copyright 2019 Nicholas Bilyk
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

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

fun dependency(path: String, module: String, includeSources: Boolean = true, includeDocs: Boolean = true) {
	downloadJars(path, "$ACORNUI_HOME_PATH/$module/externalLib/compile/${path.substringAfterLast("/")}", includeSources, includeDocs)
}

fun runtimeDependency(path: String, module: String) {
	downloadJars(path, "$ACORNUI_HOME_PATH/$module/externalLib/runtime/${path.substringAfterLast("/")}", includeSources = false, includeDocs = false)
}

fun testDependency(path: String, includeSources: Boolean = true, includeDocs: Boolean = true) {
	downloadJars(path, "$ACORNUI_HOME_PATH/externalLib/test/${path.substringAfterLast("/")}", includeSources, includeDocs)
}

fun downloadJars(path: String, destination: String, includeSources: Boolean, includeDocs: Boolean) {
	download("$path.jar", "$destination.jar")
	if (includeSources) download("$path-sources.jar", "$destination-sources.jar")
	if (includeDocs) download("$path-javadoc.jar", "$destination-javadoc.jar")
}

fun download(path: String, destination: String) {
	val dest = File(destination)
	if (dest.exists()) return // Already up-to-date.
	dest.parentFile.mkdirs()
	val connection = URL(path).openConnection()
	val outStream = FileOutputStream(destination)
	val inChannel = Channels.newChannel(connection.inputStream)
	var position = 0L
	val contentLength = connection.contentLength
	println("Downloading $path")
	print(".")
	val bars = 100
	var currentBars = 1
	do {
		val transferred = outStream.channel.transferFrom(inChannel, position, 1024 * 32)
		position += transferred
		val desiredBars = bars * position.toFloat() / contentLength
		while (currentBars++ < desiredBars) {
			print(".")
		}
	} while (transferred > 0)
	println("")
}