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

package com.acornui.dom

import org.w3c.dom.Node
import org.w3c.dom.Range
import org.w3c.dom.Window

fun Window.getSelection(): Selection {
	@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
	return window.asDynamic().getSelection() as Selection
}

external interface Selection {
	val anchorNode: Node?
	val anchorOffset: Int
	val focusNode: Node?
	val focusOffset: Int
	val isCollapsed: Boolean;
	val rangeCount: Int
	val type: String;
	fun addRange(range: Range)
	fun collapse(node: Node?, offset: Int = definedExternally)
	fun collapseToEnd()
	fun collapseToStart()
	fun containsNode(node: Node, allowPartialContainment: Boolean = definedExternally): Boolean;
	fun deleteFromDocument()
	fun empty()
	fun extend(node: Node, offset: Int = definedExternally)
	fun getRangeAt(index: Int): Range;
	fun removeAllRanges()
	fun removeRange(range: Range)
	fun selectAllChildren(node: Node)
	fun setBaseAndExtent(anchorNode: Node, anchorOffset: Int, focusNode: Node, focusOffset: Int)
	fun setPosition(node: Node?, offset: Int = definedExternally)
}

