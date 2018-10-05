/*
 * Copyright 2018 Nicholas Bilyk
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

import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.MinMaxRo

/**
 * @author nbilyk
 */
abstract class DrawableComponent(
		owner: Owned
) : UiComponentImpl(owner) {

	protected abstract val drawable: BasicDrawable?

	init {
		validation.addNode(VERTICES, ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM or ValidationFlags.CONCATENATED_TRANSFORM) { updateVertices() }
	}

	fun invalidateVertices() {
		invalidate(VERTICES)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val drawable = drawable ?: return
		out.set(explicitWidth ?: drawable.naturalWidth, explicitHeight ?: drawable.naturalHeight)
	}

	protected open fun updateVertices() {
		drawable?.updateWorldVertices(concatenatedTransform, width, height)
	}

	override fun draw(clip: MinMaxRo) {
		val drawable = drawable ?: return
		glState.setCamera(camera)
		drawable.draw(glState, concatenatedColorTint)
	}

	companion object {
		const val VERTICES = 1 shl 16
	}
}