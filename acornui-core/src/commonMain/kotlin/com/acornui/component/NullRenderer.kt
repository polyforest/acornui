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

package com.acornui.component

import com.acornui.component.layout.ListRenderer
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.spacer
import com.acornui.component.style.SkinPart
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad

/**
 * A NullRenderer is a renderer for a virtual list that represents a null object.
 * It contains no data, and is not toggleable.
 */
class NullRenderer(
		owner: Owned
) : ContainerImpl(owner), ListRenderer {

	override var index: Int = -1

	val style = bind(NullRendererStyle())

	private var contents: UiComponent? = null

	init {
		styleTags.add(Companion)
		interactivityMode = InteractivityMode.NONE

		watch(style) {
			contents?.dispose()
			contents = addChild(it.contents(this))
		}
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		val contents = contents ?: return
		out.width.min = style.padding.expandWidth2(contents.minWidth ?: 0f)
		out.height.min = style.padding.expandHeight2(contents.minHeight?: 0f)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val contents = contents ?: return
		val pad = style.padding
		contents.setSize(pad.expandWidth(explicitWidth), pad.expandHeight(explicitHeight))
		contents.moveTo(pad.left, pad.top)
		out.set(pad.expandWidth2(contents.width), pad.expandHeight2(contents.height))
	}

	companion object : StyleTag
}

class NullRendererStyle : StyleBase() {

	override val type: StyleType<NullRendererStyle> = NullRendererStyle

	var padding by prop(Pad())

	var contents by prop<SkinPart>({ spacer(15f, 15f) })

	companion object : StyleType<NullRendererStyle>
}

fun Owned.nullItemRenderer(init: ComponentInit<NullRenderer> = {}): NullRenderer {
	val renderer = NullRenderer(this)
	renderer.init()
	return renderer
}
