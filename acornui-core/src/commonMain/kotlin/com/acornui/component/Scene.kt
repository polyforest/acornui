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
import com.acornui.di.Owned
import com.acornui.graphic.orthographicCamera
import com.acornui.gl.core.useViewportFromCanvasTransform
import com.acornui.math.*

/**
 * A Scene renders its children within an unrotated window according to its explicit size and position.
 *
 * Does not support z translation, rotations, or custom transformations.
 */
class Scene(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {

	private val cam = orthographicCamera(autoCenter = false)

	init {
		cameraOverride = cam
		_naturalRenderContext.modelTransformOverride = Matrix4.IDENTITY
		_naturalRenderContext.clipRegionOverride = MinMaxRo.POSITIVE_INFINITY
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
	private val canvasTransformOverride = Rectangle()

	override fun updateRenderContext() {
		super.updateRenderContext()
		_naturalRenderContext.parentContext.localToCanvas(region.set(x, y, width, height).translate(-originX, -originY))
		_naturalRenderContext.canvasTransformOverride = canvasTransformOverride.set(
				region.xMin,
				region.yMin,
				region.width,
				region.height
		)
	}

	override fun draw(renderContext: RenderContextRo) {
		glState.useViewportFromCanvasTransform(renderContext.canvasTransform) {
			super.draw(renderContext)
		}
	}

}

fun Owned.scene(init: ComponentInit<Scene>): Scene {
	val s = Scene(this)
	s.init()
	return s
}
