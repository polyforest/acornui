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

import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.MinMax

/**
 * @author nbilyk
 */
abstract class RenderableComponent<T : Renderable?>(
		owner: Owned
) : UiComponentImpl(owner) {

	protected abstract val renderable: T?

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val drawable = renderable ?: return
		drawable.setSize(explicitWidth, explicitHeight)
		out.set(drawable.bounds)
	}

	override fun draw(renderContext: RenderContextRo) {
		renderable?.render(renderContext)
	}

	override fun updateDrawRegion(out: MinMax) {
		out.set(renderable?.drawRegion)
	}
}

class RenderableComponentImpl<T: Renderable>(owner: Owned, override val renderable: T) : RenderableComponent<T>(owner)

fun <T: Renderable> Owned.drawableC(drawable: T, init: ComponentInit<RenderableComponentImpl<T>> = {}): RenderableComponentImpl<T> {
	val d = RenderableComponentImpl(this, drawable)
	d.init()
	return d
}
