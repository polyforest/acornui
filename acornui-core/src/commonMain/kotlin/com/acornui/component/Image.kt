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

import com.acornui.component.layout.SingleElementLayoutContainerImpl
import com.acornui.component.layout.algorithm.ScaleLayout
import com.acornui.component.layout.algorithm.ScaleLayoutData
import com.acornui.component.layout.algorithm.ScaleLayoutStyle
import com.acornui.di.Owned
import com.acornui.graphic.Texture
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A scale box layout
 */
open class Image(owner: Owned) : SingleElementLayoutContainerImpl<ScaleLayoutStyle, ScaleLayoutData>(owner, ScaleLayout()), SingleElementContainer<UiComponent> {

	override fun onElementChanged(oldElement: UiComponent?, newElement: UiComponent?) {
		super.onElementChanged(oldElement, newElement)
		oldElement?.layoutData = null
		if (newElement != null && newElement.layoutData == null) {
			val layoutData = ScaleLayoutData()
			layoutData.maxScaleX = 1f
			layoutData.maxScaleY = 1f
			newElement.layoutData = layoutData
		}
	}
}

inline fun Owned.image(init: ComponentInit<Image> = {}): Image  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val i = Image(this)
	i.init()
	return i
}

inline fun Owned.image(path: String?, init: ComponentInit<Image> = {}): Image  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val i = Image(this)
	i.init()
	i.contentsImage(path)
	return i
}

inline fun Owned.image(atlasPath: String, region: String, init: ComponentInit<Image> = {}): Image  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val i = Image(this)
	i.init()
	i.contentsAtlas(atlasPath, region)
	return i
}

/**
 * Creates a texture component and uses it as the element of a single element container.
 */
fun SingleElementContainer<UiComponent>.contentsImage(value: String?) {
	createOrReuseElement { textureC() }.path = value
}

/**
 * Creates a texture component and uses it as the element of a single element container.
 */
fun SingleElementContainer<UiComponent>.contentsTexture(value: Texture?) {
	createOrReuseElement { textureC() }.texture = value
}
