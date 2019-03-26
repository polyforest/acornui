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
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo

/**
 * A list of paragraphs to be laid out vertically.
 */
class TextVerticalGroup(owner: Owned) : TextNodeContainer<TextNode>(owner) {

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
}

class TextVerticalGroupStyle : StyleBase() {

	override val type = Companion

	/**
	 * The vertical gap between lines.
	 */
	var verticalGap by prop(0f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * The number of space char widths a tab should occupy.
	 */
	var tabSize: Int by prop(4)

	var horizontalAlign by prop(FlowHAlign.LEFT)

	companion object : StyleType<TextFlowStyle>
}

fun Owned.paragraphs(init: ComponentInit<TextVerticalGroup> = {}): TextVerticalGroup {
	val s = TextVerticalGroup(this)
	s.init()
	return s
}