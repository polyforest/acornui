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

package com.acornui.component.drawing

import com.acornui.Disposable
import com.acornui.collection.forEach2
import com.acornui.component.*
import com.acornui.di.Injector
import com.acornui.di.Owned
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.gl.core.*
import com.acornui.graphic.BlendMode
import com.acornui.graphic.TextureRo
import com.acornui.math.*
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import com.acornui.recycle.freeAll
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

	private val globalBoundingBox = Box()

	private var _mesh: StaticMesh? = null
	var mesh: StaticMesh?
		get() = _mesh
		set(value) {
			if (isActive) _mesh?.refDec()
			if (_mesh === value) return
			_mesh = value
			if (isActive) value?.refInc()
			invalidateLayout()
		}

	private val colorTransformation = colorTransformation()

	init {
		draws = true
		validation.addNode(GLOBAL_BOUNDING_BOX, ValidationFlags.LAYOUT or ValidationFlags.RENDER_CONTEXT, ValidationFlags.REDRAW_REGIONS, ::updateGlobalBoundingBox)
	}

	private fun updateGlobalBoundingBox() {
		val mesh = mesh
		if (mesh != null) {
			globalBoundingBox.set(mesh.boundingBox).mul(modelTransform)
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

	fun buildMesh(inner: MeshRegion.() -> Unit) {
		if (mesh == null) mesh = staticMesh()
		mesh!!.buildMesh(inner)
		invalidateLayout()
	}

	override fun onActivated() {
		super.onActivated()
		mesh?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		mesh?.refDec()
	}

	override fun draw() {
		val mesh = mesh ?: return
		val renderContext = renderContext
		colorTransformation.tint(renderContext.colorTint)
		glState.uniforms.useColorTransformation(colorTransformation) {
			glState.uniforms.useCamera(renderContext, useModel = true) {
				mesh.render()
			}
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val boundingBox = mesh?.boundingBox ?: return
		if (explicitWidth == null) out.width = boundingBox.max.x
		if (explicitHeight == null) out.height = boundingBox.max.y
	}

	override fun updateDrawRegionLocal(out: Box) {
		val boundingBox = mesh?.boundingBox ?: return
		out.set(boundingBox)
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

inline fun Owned.staticMeshC(init: ComponentInit<StaticMeshComponent> = {}): StaticMeshComponent {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = StaticMeshComponent(this)
	s.init()
	return s
}

inline fun Owned.staticMeshC(mesh: StaticMesh, init: ComponentInit<StaticMeshComponent> = {}): StaticMeshComponent {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = StaticMeshComponent(this)
	s.mesh = mesh
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
	val boundingBox: BoxRo = _boundingBox

	private val batch = ShaderBatchImpl(gl, glState, vertexAttributes, isDynamic = false)
	private val textures = HashSet<TextureRo>()
	private val oldTextures = ArrayList<TextureRo>()

	init {
		val positionAttribute = vertexAttributes.getAttributeByUsage(VertexAttributeUsage.POSITION)
				?: throw IllegalArgumentException("A static mesh must at least have a position attribute.")
		if (positionAttribute.numComponents != 3) throw IllegalArgumentException("position must be 3 components.")
	}

	private var refCount = 0

	fun refInc() {
		if (refCount == 0) {
			textures.forEach {
				it.refInc()
			}
			batch.upload()
		}
		refCount++
	}

	fun refDec() {
		refCount--
		if (refCount == 0) {
			textures.forEach {
				it.refDec()
			}
			batch.delete()
		}
	}

	/**
	 * Draws the [inner] contents into this static mesh.
	 */
	fun buildMesh(inner: MeshRegion.() -> Unit) {
		if (refCount > 0) {
			oldTextures.addAll(textures)
			batch.delete()
		}
		glState.useBatch(batch) {
			glState.setTexture(glState.whitePixel)
			batch.clear()
			batch.begin()
			glState.blendMode(BlendMode.NORMAL, false)
			mesh(batch) {
				inner()
			}
			updateBoundingBox()
			textures.clear()
			for (i in 0..batch.drawCalls.lastIndex) {
				// Keeps track of the textures used so we can reference count them.
				val texture = batch.drawCalls[i].texture ?: glState.whitePixel
				if (!textures.contains(texture))
					textures.add(texture)
			}
			if (refCount > 0) {
				batch.upload()
				textures.forEach {
					it.refInc()
				}
				oldTextures.forEach2 {
					it.refDec()
				}
				oldTextures.clear()
			}
		}
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
	}

	override fun clear() = batch.clear()

	fun intersects(localRay: RayRo, intersection: Vector3): Boolean {
		val vertexComponents = batch.vertexComponents
		val indices = batch.indices
		val vertexAttributes = vertexAttributes
		val vertexSize = vertexAttributes.vertexSize
		val positionOffset = vertexAttributes.getOffsetByUsage(VertexAttributeUsage.POSITION) ?: return false
		val c = vertexAttributes.getAttributeByUsage(VertexAttributeUsage.POSITION)!!.numComponents

		for (i in batch.drawCalls.lastIndex downTo 0) {
			val drawCall = batch.drawCalls[i]
			if (drawCall.count != 0) {
				indices.position = drawCall.offset
				if (drawCall.mode == Gl20.TRIANGLES) {
					for (j in 0 until drawCall.count step 3) {
						vertexComponents.position = indices.get() * vertexSize + positionOffset
						v0.set(vertexComponents.get(), vertexComponents.get(), if (c >= 3) vertexComponents.get() else 0f)
						vertexComponents.position = indices.get() * vertexSize + positionOffset
						v1.set(vertexComponents.get(), vertexComponents.get(), if (c >= 3) vertexComponents.get() else 0f)
						vertexComponents.position = indices.get() * vertexSize + positionOffset
						v2.set(vertexComponents.get(), vertexComponents.get(), if (c >= 3) vertexComponents.get() else 0f)
						if (localRay.intersectsTriangle(v0, v1, v2, intersection)) {
							return true
						}
					}
				} else {
					// TODO #149: Implement intersections for TRIANGLE_STRIP and TRIANGLE_FAN
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
	val texture: TextureRo?
	val blendMode: BlendMode
	val premultipliedAlpha: Boolean
	val mode: Int
	val count: Int
	val offset: Int
}

class DrawElementsCall private constructor() : Clearable, DrawElementsCallRo {

	override var texture: TextureRo? = null
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
