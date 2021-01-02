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

package com.acornui

import com.acornui.collection.find
import com.acornui.di.Context
import com.acornui.di.ContextMarker.Companion.MAIN
import com.acornui.di.contextKey
import com.acornui.di.dependencyFactory
import com.acornui.dom.head
import com.acornui.string.isDigit

typealias Version = KotlinVersion

/**
 * Parses this String into a [Version] object.
 * The String is expected to have up to 3 parts: major.minor.patch
 *
 * Valid values:
 * `1.2.3`
 * `1.2.3-SNAPSHOT`
 * `1.2`
 * `1`
 *
 * @throws NumberFormatException If a part does not begin with a valid number.
 */
fun String.toVersion(): Version {
	val split = split(".")
	try {
		return Version(major = split.versionPart(0), minor = split.versionPart(1), patch = split.versionPart(2))
	} catch (e: NumberFormatException) {
		throw NumberFormatException("Invalid Version format \'$this\'")
	}
}

private fun List<String>.versionPart(i: Int): Int {
	val str = getOrNull(i) ?: return 0
	return str.takeWhile { it.isDigit() }.toInt()
}

val versionKey = object : Context.Key<Version> {

	override val factory = dependencyFactory(MAIN) {
		val versionMeta = head.getElementsByTagName("META").find {
			it.attributes.getNamedItem("name")?.value == "version"
		}
		val version = versionMeta?.attributes?.getNamedItem("content")?.value
		version?.toVersion() ?: KotlinVersion(0, 0)
	}
}

val Context.version: Version
	get() = inject(versionKey)