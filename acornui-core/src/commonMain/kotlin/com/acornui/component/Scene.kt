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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.component

import com.acornui.collection.forEach2
import com.acornui.core.di.Owned
import com.acornui.core.graphic.orthographicCamera
import com.acornui.gl.core.useViewportFromCanvasTransform
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A Scene renders its children within an unrotated window according to its explicit size and position.
 *
 * Does not support z translation, rotations, or custom transformations.
 */
class Scene(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {

	private val cam = orthographicCamera(autoCenter = false)

	init {
		validation.addNode(1 shl 16, ValidationFlags.LAYOUT or ValidationFlags.RENDER_CONTEXT, ::updateViewport)
		cameraOverride = cam
		_renderContext.modelTransformOverride = Matrix4.IDENTITY
		_renderContext.clipRegionOverride = MinMaxRo.POSITIVE_INFINITY
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		out.set(explicitWidth ?: window.width, explicitHeight ?: window.height)
		cam.setViewport(out.width, out.height)
		cam.moveToLookAtRect(0f, 0f, out.width, out.height)
		elementsToLayout.forEach2 {
			// Elements of the stage all are explicitly sized to the dimensions of the stage.
			it.setSize(explicitWidth, explicitHeight)
		}
	}

	private val region = MinMax()
	private val canvasTransformOverride = IntRectangle()

	private fun updateViewport() {
		_renderContext.parentContext.localToCanvas(region.set(x, y, width, height))
		_renderContext.canvasTransformOverride = canvasTransformOverride.set(
				floor(region.xMin).toInt(),
				floor(region.yMin).toInt(),
				ceil(region.width).toInt(),
				ceil(region.height).toInt()
		)
	}

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		glState.useViewportFromCanvasTransform(canvasTransform) {
			super.draw(clip, transform, tint)
		}
	}

}

fun Owned.scene(init: ComponentInit<Scene>): Scene {
	val s = Scene(this)
	s.init()
	return s
}
