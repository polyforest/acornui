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

import com.acornui.di.Owned
import com.acornui.math.Bounds

/**
 * @author nbilyk
 */
abstract class RenderableComponent<T : BasicRenderable?>(
		owner: Owned
) : UiComponentImpl(owner) {

	protected abstract val renderable: T?

	init {
		draws = true
		validation.addNode(ValidationFlags.VERTICES, ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM or ValidationFlags.RENDER_CONTEXT, ::updateWorldVertices)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val drawable = renderable ?: return
		out.set(explicitWidth ?: drawable.naturalWidth, explicitHeight ?: drawable.naturalHeight)
	}

	protected open fun updateWorldVertices() {
		if (width <= 0f || height <= 0f) return
		val renderContext = renderContext
		renderable?.updateWorldVertices(width, height, renderContext.modelTransform, renderContext.colorTint)
	}

	override fun draw() {
		if (width <= 0f || height <= 0f) return
		renderable?.render()
	}
}

class RenderableComponentImpl<T: BasicRenderable>(owner: Owned, override val renderable: T) : RenderableComponent<T>(owner)

fun <T: BasicRenderable> Owned.drawableC(drawable: T, init: ComponentInit<RenderableComponentImpl<T>> = {}): RenderableComponentImpl<T> {
	val d = RenderableComponentImpl(this, drawable)
	d.init()
	return d
}
