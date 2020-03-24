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

import com.acornui.di.Context
import com.acornui.math.Bounds
import com.acornui.math.Matrix4

/**
 * A RenderableComponent renders a single [BasicRenderable] element.
 */
abstract class RenderableComponent<T : BasicRenderable?>(
		owner: Context
) : UiComponentImpl(owner) {

	protected abstract val renderable: T?

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val drawable = renderable ?: return
		out.set(explicitWidth ?: drawable.naturalWidth, explicitHeight ?: drawable.naturalHeight)
	}

	private val translationTransform = Matrix4()

	override fun updateVerticesGlobal() {
		super.updateVerticesGlobal()
		if (width <= 0f || height <= 0f) return
		renderable?.updateGlobalVertices(width, height, translationTransform.setTranslation(vertexTranslation), colorTintGlobal)
	}

	override fun draw() {
		if (width <= 0f || height <= 0f) return
		renderable?.render()
	}
}

class RenderableComponentImpl<T: BasicRenderable>(owner: Context, override val renderable: T) : RenderableComponent<T>(owner)

fun <T: BasicRenderable> Context.drawableC(drawable: T, init: ComponentInit<RenderableComponentImpl<T>> = {}): RenderableComponentImpl<T> {
	val d = RenderableComponentImpl(this, drawable)
	d.init()
	return d
}
