/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.component.drawing

import com.acornui.collection.Clearable
import com.acornui.collection.ClearableObjectPool
import com.acornui.component.ComponentInit
import com.acornui.component.UiComponentImpl
import com.acornui.component.ValidationFlags
import com.acornui.component.validationProp
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.filter.colorTransformationFilter
import com.acornui.gl.core.*
import com.acornui.graphics.Color
import com.acornui.math.*

/**
 * A UiComponent for drawing static [MeshRegion] with uniforms for model and color transformation.
 *
 * @author nbilyk
 *
 */
open class StaticMeshComponent(
		owner: Owned
) : UiComponentImpl(owner) {

	var intersectionType = MeshIntersectionType.BOUNDING_BOX

	private val glState = inject(GlState)

	private val globalBoundingBox = Box()

	var mesh: StaticMesh? by validationProp(null, ValidationFlags.LAYOUT)

	private val colorTransformationFilter = colorTransformationFilter()

	init {
		validation.addNode(GLOBAL_BOUNDING_BOX, ValidationFlags.CONCATENATED_TRANSFORM or ValidationFlags.LAYOUT, this::updateGlobalBoundingBox)
		renderFilters.add(colorTransformationFilter)
	}

	private fun updateGlobalBoundingBox() {
		val mesh = mesh
		if (mesh != null) {
			globalBoundingBox.set(mesh.boundingBox).mul(concatenatedTransform)
		} else {
			globalBoundingBox.inf()
		}
	}

	/**
	 * Overrides intersectsGlobalRay to check against each drawn triangle.
	 */
	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
		val mesh = mesh ?: return false
		validate(GLOBAL_BOUNDING_BOX)

		if (!globalBoundingBox.intersects(globalRay, intersection)) {
			return false
		}
		if (intersectionType == MeshIntersectionType.EXACT) {
			globalToLocal(localRay.set(globalRay))
			if (!mesh.intersects(localRay, intersection)) {
				return false
			}
		}
		return true
	}

	override fun updateConcatenatedColorTransform() {
		super.updateConcatenatedColorTransform()

		colorTransformationFilter.enabled = _concatenatedColorTint != Color.WHITE
		if (colorTransformationFilter.enabled) {
			colorTransformationFilter.colorTransformation.tint(_concatenatedColorTint)
		}
	}

	override fun draw(viewport: MinMaxRo) {
		val mesh = mesh ?: return
		glState.batch.flush(true)
		glState.camera(camera, concatenatedTransform) // Use the concatenated transform as the model matrix.
		mesh.render()
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val boundingBox = mesh?.boundingBox ?: return
		if (explicitWidth == null) out.width = boundingBox.max.x
		if (explicitHeight == null) out.height = boundingBox.max.y
	}

	override fun dispose() {
		super.dispose()
		mesh = null
	}

	companion object {

		private val localRay = Ray()

		private const val GLOBAL_BOUNDING_BOX = 1 shl 16
	}
}

fun Owned.staticMeshC(init: ComponentInit<StaticMeshComponent> = {}): StaticMeshComponent {
	val s = StaticMeshComponent(this)
	s.init()
	return s
}

/**
 * Feeds mesh data to index and vertex buffers, and can [render] static mesh data.
 *
 * @author nbilyk
 */
class StaticMesh(
		override val injector: Injector,
		val vertexAttributes: VertexAttributes = standardVertexAttributes
) : Scoped, Disposable, Clearable {

	private val gl = inject(Gl20)
	private val glState = inject(GlState)

	private val _boundingBox = Box()
	val boundingBox: BoxRo
		get() = _boundingBox

	private val batch = StaticShaderBatchImpl(gl, glState, vertexAttributes)

	init {
		val positionAttribute = vertexAttributes.getAttributeByUsage(VertexAttributeUsage.POSITION) ?: throw IllegalArgumentException("A static mesh must at least have a position attribute.")
		if (positionAttribute.numComponents != 3) throw IllegalArgumentException("position must be 3 components.")
	}

	/**
	 * Resets the line and fill styles
	 */
	fun buildMesh(inner: MeshRegion.() -> Unit) {
		MeshBuilderStyle.clear()
		val previousBatch = glState.batch
		glState.setTexture(glState.whitePixel)
		glState.batch = batch
		batch.begin()
		glState.blendMode(BlendMode.NORMAL, false)
		meshData(batch) {
			inner()
		}
		batch.flush(true)
		batch.resetRenderCount()
		glState.batch = previousBatch
		updateBoundingBox()
	}

	private fun updateBoundingBox() {
		_boundingBox.inf()
		val c = vertexAttributes.getAttributeByUsage(VertexAttributeUsage.POSITION)!!.numComponents
		batch.iterateVertexAttribute(VertexAttributeUsage.POSITION) {
			val x = it.get()
			val y = it.get()
			val z = if (c >= 3) it.get() else 0f
			_boundingBox.ext(x, y, z)
		}
		_boundingBox.update()
	}

	override fun clear() = batch.clear()

	fun intersects(localRay: RayRo, intersection: Vector3): Boolean {
		val vertexComponents = batch.vertexComponents
		val indices = batch.indices
		val vertexAttributes = vertexAttributes
		val vertexSize = vertexAttributes.vertexSize
		val positionOffset = vertexAttributes.getOffsetByUsage(VertexAttributeUsage.POSITION) ?: return false

		vertexComponents.rewind()
		indices.rewind()

		for (i in batch.drawCalls.lastIndex downTo 0) {
			val drawCall = batch.drawCalls[i]
			if (drawCall.count != 0) {
				indices.position = drawCall.offset
				if (drawCall.mode == Gl20.TRIANGLES) {
					for (j in 0..drawCall.count - 1 step 3) {
						vertexComponents.position = indices.get() * vertexSize + positionOffset
						v0.set(vertexComponents.get(), vertexComponents.get(), vertexComponents.get())
						vertexComponents.position = indices.get() * vertexSize + positionOffset
						v1.set(vertexComponents.get(), vertexComponents.get(), vertexComponents.get())
						vertexComponents.position = indices.get() * vertexSize + positionOffset
						v2.set(vertexComponents.get(), vertexComponents.get(), vertexComponents.get())
						if (localRay.intersects(v0, v1, v2, intersection)) {
							return true
						}
					}
				} else {
					// TODO: Implement intersections for TRIANGLE_STRIP and TRIANGLE_FAN
				}
			}
		}
		return false
	}

	fun render() {
		batch.render()
	}

	override fun dispose() {
		batch.dispose()
	}

	companion object {
		private val v0 = Vector3()
		private val v1 = Vector3()
		private val v2 = Vector3()
	}
}

interface DrawElementsCallRo {
	val texture: Texture?
	val blendMode: BlendMode
	val premultipliedAlpha: Boolean
	val mode: Int
	val count: Int
	val offset: Int
}

class DrawElementsCall private constructor() : Clearable, DrawElementsCallRo {

	override var texture: Texture? = null
	override var blendMode = BlendMode.NORMAL
	override var premultipliedAlpha = false
	override var mode = Gl20.TRIANGLES
	override var count = 0
	override var offset = 0

	override fun clear() {
		texture = null
		blendMode = BlendMode.NORMAL
		premultipliedAlpha = false
		mode = Gl20.TRIANGLES
		count = 0
		offset = 0
	}

	companion object {
		private val pool = ClearableObjectPool { DrawElementsCall() }

		fun obtain(): DrawElementsCall {
			return pool.obtain()
		}

		fun free(call: DrawElementsCall) {
			pool.free(call)
		}

		fun freeAll(calls: List<DrawElementsCall>) {
			pool.freeAll(calls)
		}
	}
}

fun Scoped.staticMesh(init: StaticMesh.() -> Unit = {}): StaticMesh {
	val m = StaticMesh(injector)
	m.init()
	return m
}