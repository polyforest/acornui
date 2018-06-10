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

import com.acornui._assert
import com.acornui.assertionsEnabled
import com.acornui.collection.Clearable
import com.acornui.collection.ClearableObjectPool
import com.acornui.component.ComponentInit
import com.acornui.component.UiComponentImpl
import com.acornui.component.ValidationFlags
import com.acornui.component.drawing.DrawElementsCall.Companion
import com.acornui.core.TreeWalk
import com.acornui.core.childWalkLevelOrder
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Texture
import com.acornui.core.io.BufferFactory
import com.acornui.core.iterateChildren
import com.acornui.gl.core.*
import com.acornui.io.NativeBuffer
import com.acornui.math.*
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

/**
 * A UiComponent for drawing static [MeshData] with uniforms for model and color transformation.
 *
 * @author nbilyk
 */
open class StaticMeshComponent(
		owner: Owned
) : UiComponentImpl(owner) {

	var intersectionType = MeshIntersectionType.BOUNDING_BOX

	private val glState = inject(GlState)
	private val gl = inject(Gl20)

	private val globalBoundingBox = Box()

	private val meshChangedHandler = {
		invalidate(ValidationFlags.LAYOUT)
		Unit
	}

	private var _mesh: StaticMesh? = null

	var mesh: StaticMesh?
		get() = _mesh
		set(value) {
			_mesh?.changed?.remove(meshChangedHandler)
			if (isActive)
				_mesh?.refDec(gl)
			_mesh = value
			if (isActive)
				_mesh?.refInc(gl)
			_mesh?.changed?.add(meshChangedHandler)
			invalidate(ValidationFlags.LAYOUT)
		}

	init {
		validation.addNode(GLOBAL_BOUNDING_BOX, ValidationFlags.CONCATENATED_TRANSFORM or ValidationFlags.LAYOUT, {
			if (mesh != null) {
				globalBoundingBox.set(mesh!!.boundingBox).mul(concatenatedTransform)
			} else {
				globalBoundingBox.inf()
			}
		})
	}

	override fun onActivated() {
		_mesh?.refInc(gl)
	}

	override fun onDeactivated() {
		_mesh?.refDec(gl)
	}

	/**
	 * Overrides intersectsGlobalRay to check against each drawn triangle.
	 */
	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
		if (mesh == null) return false
		validate(GLOBAL_BOUNDING_BOX)

		if (!globalBoundingBox.intersects(globalRay, intersection)) {
			return false
		}
		if (intersectionType == MeshIntersectionType.EXACT) {
			globalToLocal(localRay.set(globalRay))
			if (!mesh!!.intersects(localRay, intersection)) {
				return false
			}
		}
		return true
	}

	override fun draw(viewport: MinMaxRo) {
		val mesh = mesh ?: return
		glState.camera(camera, concatenatedTransform) // Use the concatenated transform as the model matrix.
		mesh.render(gl, glState)
	}

	// TODO: test
	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val boundingBox = mesh?.boundingBox ?: return
		out.ext(boundingBox.max.x, boundingBox.max.y)
	}

	override fun dispose() {
		super.dispose()
		mesh = null
	}

	companion object {

		private val localRay = Ray()

		private val GLOBAL_BOUNDING_BOX = 1 shl 16
	}
}

fun Owned.staticMeshC(init: ComponentInit<StaticMeshComponent> = {}): StaticMeshComponent {
	val s = StaticMeshComponent(this)
	s.init()
	return s
}

fun Owned.staticMeshC(mesh: StaticMesh, init: ComponentInit<StaticMeshComponent> = {}): StaticMeshComponent {
	val s = StaticMeshComponent(this)
	s.init()
	s.mesh = mesh
	return s
}


/**
 * Feeds mesh data to index and vertex buffers, and can [render] static mesh data.
 *
 * @author nbilyk
 */
class StaticMesh {

	private val _changed = Signal0()
	val changed: Signal<() -> Unit>
		get() = _changed

	var vertexAttributes: VertexAttributes = standardVertexAttributes
	val boundingBox = Box()

	private var indicesBuffer: GlBufferRef? = null
	private var indices: NativeBuffer<Short>? = null

	private var vertexComponentsBuffer: GlBufferRef? = null
	private var vertexComponents: NativeBuffer<Float>? = null

	private var drawCall = DrawElementsCall.obtain()
	private val drawCalls = arrayListOf(drawCall)

	private var refCount = 0
	private var needsUpload = false

	init {
	}

	/**
	 * Increments the number of places this Mesh is used. When the number of references becomes non-zero,
	 * the buffers will be created.
	 */
	fun refInc(gl: Gl20) {
		if (refCount++ == 0) {
			create(gl)
		}
	}

	/**
	 * Decrements the number of places this Mesh is used. If the count reaches zero, the mesh's buffers will be deleted.
	 */
	fun refDec(gl: Gl20) {
		if (--refCount == 0) {
			delete(gl)
		}
	}

	private fun create(gl: Gl20) {
		indicesBuffer = gl.createBuffer()
		vertexComponentsBuffer = gl.createBuffer()
	}

	private fun delete(gl: Gl20) {
		needsUpload = true
		if (indicesBuffer != null) {
			gl.deleteBuffer(indicesBuffer!!)
			indicesBuffer = null
		}
		if (vertexComponentsBuffer != null) {
			gl.deleteBuffer(vertexComponentsBuffer!!)
			vertexComponentsBuffer = null
		}
	}

	/**
	 * Resets the line and fill styles
	 */
	inline fun buildMesh(inner: MeshData.() -> Unit) {
		fillStyle.clear()
		lineStyle.clear()
		val meshData = MeshData()
		meshData.inner()
		feed(meshData)
	}

	fun feed(meshData: MeshData) {
		needsUpload = true
		var indicesL = 0
		var verticesL = 0
		meshData.childWalkLevelOrder {
			indicesL += it.indices.size
			verticesL += it.vertices.size
			TreeWalk.CONTINUE
		}

		// Recycle the draw calls.
		for (i in 0..drawCalls.lastIndex) {
			DrawElementsCall.free(drawCalls[i])
		}
		drawCalls.clear()

		drawCall = DrawElementsCall.obtain()
		drawCalls.add(drawCall)

		indices = BufferFactory.instance.shortBuffer(indicesL)
		vertexComponents = BufferFactory.instance.floatBuffer(verticesL * vertexAttributes.vertexSize)
		boundingBox.inf()
		populateMeshData(meshData)
		boundingBox.update()
		indices!!.flip()
		vertexComponents!!.flip()
		_changed.dispatch()
	}

	private fun populateMeshData(meshData: MeshData) {
		val indices = meshData.indices
		val vertices = meshData.vertices

		if (meshData.flushBatch)
			nextDrawCall()

		if (indices.isNotEmpty()) {
			checkDrawCall(meshData.texture, meshData.blendMode, false, meshData.drawMode)
			drawCall.count += indices.size

			if (assertionsEnabled) {
				if (meshData.drawMode == Gl20.LINES) {
					_assert(indices.size % 2 == 0, { "indices size ${indices.size} not evenly divisible by 2" })
				} else if (meshData.drawMode == Gl20.TRIANGLES) {
					_assert(indices.size % 3 == 0, { "indices size ${indices.size} not evenly divisible by 3" })
				}
				_assert(vertices.isNotEmpty(), "Indices pushed with no vertices.")
			}

			val vertexComponents = vertexComponents!!
			val n = vertexComponents.position / vertexAttributes.vertexSize
			for (j in 0..vertices.lastIndex) {
				val v = vertices[j]
				vertexAttributes.putVertex(vertexComponents, v.position, v.normal, v.colorTint, v.u, v.v)
				boundingBox.ext(v.position, update = false)
			}
			for (j in 0..indices.lastIndex) {
				val index = (n + indices[j]).toShort()
				this.indices!!.put(index)
			}
		}
		meshData.iterateChildren {
			populateMeshData(it)
			true
		}
		if (meshData.flushBatch)
			nextDrawCall()
	}

	private fun nextDrawCall() {
		if (drawCall.count != 0) {
			val e = drawCall.offset + drawCall.count
			drawCall = DrawElementsCall.obtain()
			drawCall.offset = e
			drawCalls.add(drawCall)
		}
	}

	private fun checkDrawCall(texture: Texture?, blendMode: BlendMode, premultipliedAlpha: Boolean, mode: Int): Boolean {
		if (drawCall.texture != texture ||
				drawCall.blendMode != blendMode ||
				drawCall.premultipliedAlpha != premultipliedAlpha ||
				drawCall.mode != mode) {
			nextDrawCall()
			drawCall.texture = texture
			drawCall.blendMode = blendMode
			drawCall.premultipliedAlpha = premultipliedAlpha
			drawCall.mode = mode
			return true
		} else {
			return false
		}
	}

	fun intersects(localRay: Ray, intersection: Vector3): Boolean {
		val vertexData = vertexComponents ?: return false
		val indices = indices ?: return false
		vertexData.rewind()
		indices.rewind()

		val vertexAttributes = vertexAttributes
		val vertexSize = vertexAttributes.vertexSize
		for (i in drawCalls.lastIndex downTo 0) {
			val drawCall = drawCalls[i]
			if (drawCall.count != 0) {
				indices.position = drawCall.offset
				if (drawCall.mode == Gl20.TRIANGLES) {
					for (j in 0..drawCall.count - 1 step 3) {
						vertexData.position = indices.get() * vertexSize
						vertexAttributes.getVertex(vertexData, v0)
						vertexData.position = indices.get() * vertexSize
						vertexAttributes.getVertex(vertexData, v1)
						vertexData.position = indices.get() * vertexSize
						vertexAttributes.getVertex(vertexData, v2)
						if (localRay.intersects(v0.position, v1.position, v2.position, intersection)) {
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

	fun render(gl: Gl20, glState: GlState) {
		if (drawCalls.isEmpty()) return

		val indices = indices ?: return // Return if no indices to render.
		val vertexComponents = vertexComponents ?: return

		gl.bindBuffer(Gl20.ARRAY_BUFFER, vertexComponentsBuffer)
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, indicesBuffer)
		vertexAttributes.bind(gl, glState.shader!!)

		if (needsUpload) {
			needsUpload = false

			indices.rewind()
			gl.bufferData(Gl20.ELEMENT_ARRAY_BUFFER, indices.limit shl 1, Gl20.DYNAMIC_DRAW) // Allocate
			gl.bufferDatasv(Gl20.ELEMENT_ARRAY_BUFFER, indices, Gl20.DYNAMIC_DRAW) // Upload

			vertexComponents.rewind()
			gl.bufferData(Gl20.ARRAY_BUFFER, vertexComponents.limit shl 2, Gl20.DYNAMIC_DRAW) // Allocate
			gl.bufferDatafv(Gl20.ARRAY_BUFFER, vertexComponents, Gl20.DYNAMIC_DRAW) // Upload
		}

		for (i in 0..drawCalls.lastIndex) {
			val drawCall = drawCalls[i]
			if (drawCall.count != 0) {
				glState.blendMode(drawCall.blendMode, drawCall.premultipliedAlpha)
				glState.setTexture(drawCall.texture ?: glState.whitePixel)
				gl.drawElements(drawCall.mode, drawCall.count, Gl20.UNSIGNED_SHORT, drawCall.offset shl 1)
			}
		}

		vertexAttributes.unbind(gl, glState.shader!!)
		gl.bindBuffer(Gl20.ARRAY_BUFFER, null)
		gl.bindBuffer(Gl20.ELEMENT_ARRAY_BUFFER, null)
	}
	
	companion object {
		private val v0 = Vertex()
		private val v1 = Vertex()
		private val v2 = Vertex()
	}

}

class DrawElementsCall private constructor() : Clearable {

	var texture: Texture? = null
	var blendMode = BlendMode.NORMAL
	var premultipliedAlpha = false
	var mode = Gl20.TRIANGLES
	var count = 0
	var offset = 0

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
	}
}

fun staticMesh(init: StaticMesh.() -> Unit = {}): StaticMesh {
	val m = StaticMesh()
	m.init()
	return m
}