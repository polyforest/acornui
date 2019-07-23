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
abstract class DrawableComponent<T : Renderable?>(
		owner: Owned
) : UiComponentImpl(owner) {

	protected abstract val drawable: T?

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val drawable = drawable ?: return
		drawable.setSize(explicitWidth, explicitHeight)
		out.set(drawable.bounds)
	}

	override fun draw(renderContext: RenderContextRo) {
		drawable?.render(renderContext)
	}

	override fun updateDrawRegion(out: MinMax) {
		out.set(drawable?.drawRegion)
	}
}

class DrawableComponentImpl<T: Renderable>(owner: Owned, override val drawable: T) : DrawableComponent<T>(owner)

fun <T: Renderable> Owned.drawableC(drawable: T, init: ComponentInit<DrawableComponentImpl<T>> = {}): DrawableComponentImpl<T> {
	val d = DrawableComponentImpl(this, drawable)
	d.init()
	return d
}
