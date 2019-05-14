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

import com.acornui.graphic.ColorRo
import com.acornui.math.*

class PaddedDrawable(
		private val wrapped: BasicDrawable
) : BasicDrawable {

	val padding = Pad()

	override val naturalWidth: Float
		get() = padding.expandWidth2(wrapped.naturalWidth)

	override val naturalHeight: Float
		get() = padding.expandHeight2(wrapped.naturalHeight)

	override fun updateVertices(width: Float, height: Float, x: Float, y: Float, z: Float, rotation: Float, originX: Float, originY: Float) {
		wrapped.updateVertices(padding.reduceWidth2(width), padding.reduceHeight2(height), x, y, z, rotation, originX - padding.left, originY - padding.top)
	}

	override fun render(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		wrapped.render(clip, transform, tint)
	}
}
