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
import com.acornui.gl.core.useCamera
import com.acornui.gl.core.useViewportFromCanvasTransform
import com.acornui.graphic.Camera
import com.acornui.graphic.OrthographicCamera
import com.acornui.math.*

/**
 * A Scene renders its children within an unrotated window according to its explicit size and position.
 *
 * Does not support z translation, rotations, or custom transformations.
 */
open class Scene(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {

	var camera: Camera = OrthographicCamera()
		set(value) {
			field = value
			cameraOverride = field
			invalidate(ValidationFlags.LAYOUT)
		}

	init {
		cameraOverride = camera
		_renderContext.modelTransformOverride = Matrix4.IDENTITY
		_renderContext.clipRegionOverride = MinMaxRo.POSITIVE_INFINITY
		validation.addNode(CANVAS_TRANSFORM, dependencies = ValidationFlags.LAYOUT, dependents = ValidationFlags.RENDER_CONTEXT, onValidate = {})
	}

	override fun onChildInvalidated(child: UiComponent, flagsInvalidated: Int) {
		// Don't invalidate the scene's size when a child's layout has invalidated.
		childrenNeedValidation = true
		invalidate(flagsInvalidated and bubblingFlags)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		out.set(explicitWidth ?: window.width, explicitHeight ?: window.height)
		camera.setViewport(out.width, out.height)
		camera.moveToLookAtRect(0f, 0f, out.width, out.height)
		elementsToLayout.forEach2 {
			// Elements of the stage all are explicitly sized to the dimensions of the stage.
			it.setSize(explicitWidth, explicitHeight)
		}
	}

	private val region = MinMax()
	private val canvasTransformOverride = Rectangle()

	override fun updateRenderContext() {
		super.updateRenderContext()
		_renderContext.parentContext.localToCanvas(region.set(x, y, right, bottom).translate(-originX, -originY))
		_renderContext.canvasTransformOverride = canvasTransformOverride.set(
				region.xMin,
				region.yMin,
				region.width,
				region.height
		)
	}

	override fun render() {
		if (needsRedraw) {
			glState.uniforms.useCamera(camera) {
				glState.useViewportFromCanvasTransform(renderContext.canvasTransform) {
					draw()
				}
			}
		}
	}

	companion object {
		private const val CANVAS_TRANSFORM = 1 shl 16
	}
}

fun Owned.scene(init: ComponentInit<Scene>): Scene {
	val s = Scene(this)
	s.init()
	return s
}
