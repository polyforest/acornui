/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.io.file

class Path(v: String) : Comparable<Path> {

	constructor(baseDir: String, child: String) : this("${baseDir.trim('/')}/${child.trim('/')}")

	val value: String = v.replace('\\', '/').trim('/')

	val name: String
		get() = value.substringAfterLast('/')

	val nameNoExtension: String
		get() = name.substringBeforeLast('.')

	val extension: String
		get() = value.substringAfterLast('.')

	fun hasExtension(extension: String): Boolean {
		return this.extension.equals(extension, ignoreCase = true)
	}

	/**
	 * Returns the number of '/' characters.
	 */
	val depth: Int
		get() = value.count { it == '/' }

	fun resolve(child: String): Path = Path("$value/$child")

	fun sibling(sibling: String): Path {
		return parent.resolve(sibling)
	}

	fun stripComponents(count: Int): Path {
		return Path(value.split("/").drop(count).joinToString("/"))
	}

	val parent: Path
		get() = Path(value.substringBeforeLast("/"))

	/**
	 * Note, files are sorted case-sensitively. This is to ensure consistent order when two files have names
	 * that differ only in case.
	 */
	override fun compareTo(other: Path): Int {
		return if (depth == other.depth) {
			value.compareTo(other.value)
		} else {
			depth.compareTo(other.depth)
		}
	}

	override fun toString(): String = value
}