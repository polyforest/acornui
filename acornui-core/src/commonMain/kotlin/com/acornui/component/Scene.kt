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
import com.acornui.component.ValidationFlags.LAYOUT
import com.acornui.component.ValidationFlags.VIEW_PROJECTION
import com.acornui.di.Context
import com.acornui.gl.core.useCamera
import com.acornui.gl.core.useViewport
import com.acornui.graphic.Camera
import com.acornui.graphic.OrthographicCamera
import com.acornui.graphic.setViewport
import com.acornui.math.*

/**
 * A Scene renders its children within an unrotated window according to its explicit size and position.
 *
 * Does not support z translation, rotations, or custom transformations.
 */
open class Scene(owner: Context) : ElementContainerImpl<UiComponent>(owner) {

	var camera: Camera = OrthographicCamera()
		set(value) {
			field = value
			cameraOverride = field
			invalidate(ValidationFlags.LAYOUT)
		}

	private val _canvasTransform = MinMax()

	/**
	 * The canvas transformation for the scene will be based on
	 */
	override val viewport: RectangleRo by validationProp(ValidationFlags.DRAW_REGION) {
		canvasTransformOverride ?: run {
			parent?.localToCanvas(_canvasTransform.set(x, y, right, bottom).translate(-originX, -originY))
			_canvasTransform
		}
	}

	init {
		cameraOverride = camera
		transformGlobalOverride = Matrix4.IDENTITY
		canvasClipRegionOverride = MinMaxRo.POSITIVE_INFINITY
		validation.addNode(1 shl 16, LAYOUT, VIEW_PROJECTION) {} // Update layout changes camera
	}

	override fun onChildInvalidated(child: UiComponent, flagsInvalidated: Int) {
		// Don't invalidate the scene's size when a child's layout has invalidated.
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

	override fun render() {
		if (visible && colorTint.a > 0f) {
			gl.uniforms.useCamera(camera) {
				gl.useViewport(viewport, window.scaleX, window.scaleY) {
					draw()
				}
			}
		}
	}
}

fun Context.scene(init: ComponentInit<Scene>): Scene {
	val s = Scene(this)
	s.init()
	return s
}
