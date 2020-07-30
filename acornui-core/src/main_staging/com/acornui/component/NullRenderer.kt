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

import com.acornui.component.layout.spacer
import com.acornui.component.style.SkinPart
import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.math.Bounds
import com.acornui.math.Pad
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A NullRenderer is a renderer for a virtual list that represents a null object.
 * It contains no data, and is not toggleable.
 */
class NullRenderer(
		owner: Context
) : ContainerImpl(owner), ListRenderer {

	override var index: Int = -1

	val style = bind(NullRendererStyle())

	private var contents: UiComponent? = null

	init {
		addClass(Companion)
		interactivityMode = InteractivityMode.NONE

		watch(style) {
			contents?.dispose()
			contents = addChild(it.contents(this))
		}
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val contents = contents ?: return
		val pad = style.padding
		contents.size(pad.expandWidth(explicitWidth), pad.expandHeight(explicitHeight))
		contents.position(pad.left, pad.top)
		out.set(pad.expandWidth(contents.width), pad.expandHeight(contents.height))
	}

	companion object : StyleTag
}

class NullRendererStyle : ObservableBase() {

	override val type: StyleType<NullRendererStyle> = NullRendererStyle

	var padding by prop(Pad())

	var contents by prop<SkinPart> { spacer(15.0, 15.0) }

	companion object : StyleType<NullRendererStyle>
}

inline fun Context.nullItemRenderer(init: ComponentInit<NullRenderer> = {}): NullRenderer  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val renderer = NullRenderer(this)
	renderer.init()
	return renderer
}