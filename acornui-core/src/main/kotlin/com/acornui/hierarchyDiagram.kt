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

package com.acornui

/**
 * Returns a human-readable string representing a hierarchy.
 * Example:
 * 	root
 *	├──a
 *	│  ├──a.1
 *	│  │  ├──a.1.a
 *	│  │  └──a.1.b
 *	│  ├──a.2
 *	│  └──a.3
 *	├──b
 *	│  ├──b.1
 *	│  └──b.2
 *	└──c
 *	├──c.1
 *	└──c.2
 *
 * https://www.baeldung.com/java-print-binary-tree-diagram
 */
fun <T : Node> T.toDiagram(toNodeString: T.() -> String = Node::toString): String {
	val sb = StringBuilder()
	traverseNodes(sb, "", toNodeString)
	return sb.toString()
}

private fun <T : Node> T.traverseNodes(
	sb: StringBuilder,
	padding: String,
	toNodeString: T.() -> String
) {
	sb.append(toNodeString())
	val childPadding = "$padding│  "
	val lastChildPadding = "$padding   "
	for (i in 0..children.lastIndex) {
		val isLast = i == children.lastIndex
		sb.append("\n")
		sb.append(padding)
		sb.append(if (isLast) "└──" else "├──")

		@Suppress("UNCHECKED_CAST")
		(children[i] as T).traverseNodes(sb, if (isLast) lastChildPadding else childPadding, toNodeString)
	}
}