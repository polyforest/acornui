/*
 * Copyright 2018 PolyForest
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

package com.acornui.core.graphics

import com.acornui.component.*
import com.acornui.core.di.Owned
import com.acornui.gl.core.*
import com.acornui.math.*
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * A Scene renders its children within an unrotated window according to its explicit size and position.
 *
 * Does not support z translation, rotations, or custom transformations.
 */
class Scene(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {

	/**
	 * The viewport this scene would have rendered to had we not overriden.
	 */
	private var superViewport: RectangleRo = RectangleRo.EMPTY

	private val globalPosition = Vector3()
	private val globalScale = Vector3()

	init {
		validation.addNode(1 shl 16, ValidationFlags.LAYOUT or ValidationFlags.CONCATENATED_TRANSFORM or ValidationFlags.CAMERA or ValidationFlags.VIEWPORT, this::updateViewport2)
	}


	override var rotationX: Float
		get() = super.rotationX
		set(value) {
			throw UnsupportedOperationException("Cannot set rotation on Scene")
		}

	override var rotationY: Float
		get() = super.rotationY
		set(value) {
			throw UnsupportedOperationException("Cannot set rotation on Scene")
		}

	override var rotation: Float
		get() = super.rotation
		set(value) {
			throw UnsupportedOperationException("Cannot set rotation on Scene")
		}

	override var z: Float
		get() = super.z
		set(value) {
			if (value != 0f) throw UnsupportedOperationException("Cannot set z translation on Scene")
		}

	override fun setPosition(x: Float, y: Float, z: Float) {
		super.setPosition(x, y, z)
		if (z != 0f)
			throw UnsupportedOperationException("Cannot set z translation on Scene")
	}

	override fun setRotation(x: Float, y: Float, z: Float) {
		throw UnsupportedOperationException("Cannot set rotation on Scene")
	}

	override var customTransform: Matrix4Ro?
		get() = super.customTransform
		set(value) {
			throw UnsupportedOperationException("Cannot set custom transformation on Scene")
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		out.set(explicitWidth ?: window.width, explicitHeight ?: window.height)
	}

	override fun updateConcatenatedTransform() {
		val pCT = parent?.concatenatedTransform ?: Matrix4.IDENTITY
		if (pCT.mode == MatrixMode.FULL)
			throw Exception("A Scene must not be rotated.")
		pCT.getTranslation(globalPosition).add(position)
		if (globalPosition.z != 0f)
			throw UnsupportedOperationException("Cannot set z translation on Scene")
		globalPosition.x = round(globalPosition.x)
		globalPosition.y = round(globalPosition.y)
		pCT.getScale(globalScale).scl(_scale)
		if (globalScale.x < 0f || globalScale.y < 0f) {
			throw Exception("A Scene cannot have negative scaling.")
		}
	}

	private val topLeft = Vector3()
	private val bottomRight = Vector3()
	private val sceneViewport = Rectangle()

	private fun updateViewport2() {
		superViewport = parent?.viewport ?: stage.viewport
		val camera = camera
		if (camera.combined.mode == MatrixMode.FULL)
			throw Exception("Scene components cannot be rotated, even via their camera.")

		localToCanvas2(topLeft.set(0f, 0f, 0f))
		localToCanvas2(bottomRight.set(width, height, 0f))
		val x = topLeft.x.roundToInt()
		val y = topLeft.y.roundToInt()
		_viewport = sceneViewport.set(x, y, bottomRight.x.roundToInt() - x, bottomRight.y.roundToInt() - y)
	}

	private val oldGlViewport = IntRectangle()
	private val frameBuffer = FrameBufferInfo()

	override fun draw(clip: MinMaxRo) {
		glState.getFramebuffer(frameBuffer)
		glState.getViewport(oldGlViewport)
		frameBuffer.glViewport(glState, viewport)

		// TODO: we can probably bound the draw viewport.
		super.draw(MinMaxRo.POSITIVE_INFINITY)

		glState.setViewport(oldGlViewport)
	}

	/**
	 * Sets the gl viewport from a UiComponent viewport.
	 */
	private fun FrameBufferInfoRo.glViewport(glState: GlState, viewport: RectangleRo) {
		glState.setViewport(
				(viewport.x * scaleX).roundToInt(),
				(height - viewport.bottom * scaleY).roundToInt(),
				(viewport.width * scaleX).roundToInt(),
				(viewport.height * scaleY).roundToInt()
		)
	}

	/**
	 * Converts the local coordinate to canvas coordinates using the [superViewport]
	 */
	private fun localToCanvas2(localCoord: Vector3): Vector3 {
		localToGlobal2(localCoord)
		camera.project(localCoord, superViewport)
		return localCoord
	}

	private fun localToGlobal2(localCoord: Vector3): Vector3 {
		return localCoord.scl(globalScale).add(globalPosition)
	}
}

fun Owned.scene(init: ComponentInit<Scene>): Scene {
	val s = Scene(this)
	s.init()
	return s
}