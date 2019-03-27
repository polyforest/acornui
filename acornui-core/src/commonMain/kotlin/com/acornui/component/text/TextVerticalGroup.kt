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

package com.acornui.component.text

import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.ComponentInit
import com.acornui.core.di.Owned
import com.acornui.math.Bounds

/**
 * A list of paragraphs to be laid out vertically.
 */
class TextVerticalGroup(owner: Owned) : TextElementContainerImpl<TextNode>(owner) {

	override val multiline = true

	override fun getSelectionIndex(x: Float, y: Float): Int {
		val index = elements.sortedInsertionIndex(y, matchForwards = true) {
			yVal, node ->
			yVal.compareTo(node.y)
		} - 1
		if (index < 0) return index
		return elements[index].getSelectionIndex(x, y)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		var y = 0f
		val elements = elements
		var measuredWidth = 0f
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			element.y = y
			element.setSize(explicitWidth, null)
			measuredWidth = maxOf(measuredWidth, element.width)
			y += element.height
		}
		out.set(explicitWidth ?: measuredWidth, y)
	}

	override fun clone(newOwner: Owned): TextNode =
			configureClone(TextVerticalGroup(newOwner))
}

fun Owned.paragraphs(init: ComponentInit<TextVerticalGroup> = {}): TextVerticalGroup {
	val s = TextVerticalGroup(this)
	s.init()
	return s
}