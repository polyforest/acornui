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

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.CameraRo
import com.acornui.core.graphic.OrthographicCamera
import com.acornui.core.graphic.Window
import com.acornui.core.graphic.centerCamera
import com.acornui.function.as2
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool

interface RenderContextRo : CanvasTransformableRo {

	/**
	 * This render context's parent, if there is one.
	 */
	val parentContext: RenderContextRo?

	/**
	 * The clipping region, in canvas coordinates.
	 * This is used to early-out on rendering if the render contents is outside of this region.
	 * This is not done automatically; it is the responsibility of the component.
	 */
	val clipRegion: MinMaxRo

	/**
	 * The color multiplier for vertices.
	 */
	val colorTint: ColorRo

	companion object : DKey<RenderContextRo> {

		override fun factory(injector: Injector): RenderContextRo? = OrthographicRenderContext(injector)
	}
}

/**
 * Returns true if the camera and viewport match.
 */
fun RenderContextRo.cameraEquals(renderContext: RenderContextRo): Boolean {
	return viewProjectionTransform == renderContext.viewProjectionTransform && canvasTransform == renderContext.canvasTransform
}

/**
 * The orthographic render context is a render context that sets its view projection to an orthographic projection with
 * the viewport set to the window.
 * This is the default render context used by the Stage or as the parent context for any components not on the display
 * graph.
 */
class OrthographicRenderContext(override val injector: Injector) : Scoped, RenderContextRo, Disposable {

	override val parentContext: RenderContextRo? = null

	private val camera = OrthographicCamera()
	private val window = inject(Window)

	private var _clipRegion = MinMax()
	private var _canvasTransform = Rectangle()

	private var isValid = false

	init {
		window.sizeChanged.add(::invalidate.as2)
	}

	private fun invalidate() {
		isValid = false
	}

	override val modelTransform: Matrix4Ro = Matrix4.IDENTITY
	override val modelTransformInv: Matrix4Ro = Matrix4.IDENTITY

	override val viewProjectionTransformInv: Matrix4Ro
		get() {
			validate()
			return camera.combinedInv
		}

	override val viewProjectionTransform: Matrix4Ro
		get() {
			validate()
			return camera.combined
		}

	override val viewTransform: Matrix4Ro
		get() {
			validate()
			return camera.view
		}

	override val projectionTransform: Matrix4Ro
		get() {
			validate()
			return camera.projection
		}

	override val clipRegion: MinMaxRo
		get() {
			validate()
			return _clipRegion
		}

	override val canvasTransform: RectangleRo
		get() {
			validate()
			return _canvasTransform
		}

	override val colorTint: ColorRo = Color.WHITE

	private fun validate() {
		if (isValid) return
		isValid = true
		val window = window
		val w = window.width
		val h = window.height
		window.centerCamera(camera)
		_clipRegion.set(0f, 0f, w, h)
		_canvasTransform.set(0f, 0f, w, h)
	}

	override fun dispose() {
		window.sizeChanged.remove(::invalidate.as2)
	}
}

/**
 * RenderContext is the canonical render context for components.  It is hierarchical for its model and color
 * transformations, allows for a custom camera to be set, and has properties for external overrides.
 */
class RenderContext() : RenderContextRo, Clearable {

	constructor(initialParentContext: RenderContextRo?) : this() {
		_parentContext = initialParentContext
	}

	private var _parentContext: RenderContextRo? = null
	override var parentContext: RenderContextRo
		get() = _parentContext!!
		set(value) {
			_parentContext = value
		}

	/**
	 * If set, [viewTransform], [projectionTransform], [viewProjectionTransform], and [viewProjectionTransformInv]
	 * matrices will be calculated based on this camera.
	 */
	var cameraOverride: CameraRo? = null

	/**
	 * If set, [canvasTransform] will use this explicit value.
	 */
	var canvasTransformOverride: RectangleRo? = null

	/**
	 * The clip region, in local coordinates.
	 * If set, this will transform to canvas coordinates and [clipRegion] will be the intersection of the
	 * parent clip region and this region.
	 */
	var clipRegionLocal: MinMaxRo? = null

	/**
	 * If set, the [clipRegion] value won't be calculated as the intersection of [clipRegionLocal] and the parent
	 * context's clip region; it will be this explicit value.
	 */
	var clipRegionOverride: MinMaxRo? = null

	/**
	 * The calculated [colorTint] value will be this value multiplied by the parent context's color tint, unless
	 * [colorTintOverride] is set.
	 */
	var colorTintLocal: Color = Color.WHITE.copy()

	/**
	 * If set, the [colorTint] value won't be calculated as the multiplication of [colorTintLocal] and the parent
	 * context's color tint; it will be this explicit value.
	 */
	var colorTintOverride: ColorRo? = null

	/**
	 * The calculated [modelTransform] value will be this value multiplied by the parent context's model transform,
	 * unless [modelTransformOverride] is set.
	 */
	val modelTransformLocal: Matrix4 = Matrix4()

	/**
	 * If set, the [modelTransform] value won't be calculated as the multiplication of [modelTransformLocal] and the
	 * parent context's model transform; it will be this explicit value.
	 */
	var modelTransformOverride: Matrix4Ro? = null

	private val _modelTransform = Matrix4()
	override val modelTransform: Matrix4Ro
		get() = modelTransformOverride ?: _modelTransform.set(parentContext.modelTransform).mul(modelTransformLocal)

	private val _concatenatedTransformInv = Matrix4()
	override val modelTransformInv: Matrix4Ro
		get() = _concatenatedTransformInv.set(modelTransform).inv()

	override val viewProjectionTransform: Matrix4Ro
		get() = cameraOverride?.combined ?: parentContext.viewProjectionTransform

	override val viewProjectionTransformInv: Matrix4Ro
		get() = cameraOverride?.combinedInv ?: parentContext.viewProjectionTransformInv

	override val viewTransform: Matrix4Ro
		get() = cameraOverride?.view ?: parentContext.viewTransform

	override val projectionTransform: Matrix4Ro
		get() = cameraOverride?.projection ?: parentContext.projectionTransform

	override val canvasTransform: RectangleRo
		get() {
			return canvasTransformOverride ?: parentContext.canvasTransform
		}

	private val _clipRegionIntersection: MinMax = MinMax()
	override val clipRegion: MinMaxRo
		get() {
			return clipRegionOverride ?: if (clipRegionLocal == null) parentContext.clipRegion
			else localToCanvas(_clipRegionIntersection.set(clipRegionLocal!!)).intersection(parentContext.clipRegion)
		}

	private val _colorTint = Color()
	override val colorTint: ColorRo
		get() = colorTintOverride ?: _colorTint.set(parentContext.colorTint).mul(colorTintLocal).clamp()

	override fun clear() {
		_parentContext = null
		cameraOverride = null
		canvasTransformOverride = null
		clipRegionLocal = null
		colorTintLocal.set(Color.WHITE)
		colorTintOverride = null
		modelTransformLocal.idt()
		modelTransformOverride = null
	}
}

@PublishedApi
internal val renderContextPool = ClearableObjectPool { RenderContext() }

inline fun RenderContextRo.childRenderContext(inner: RenderContext.() -> Unit) {
	val c = renderContextPool.obtain()
	c.parentContext = this
	c.inner()
	renderContextPool.free(c)
}

/**
 * IdtProjectionContext is a render context where the model/view/projection transformations are the identity matrix.
 */
class IdtProjectionContext : RenderContextRo, Clearable {

	override val parentContext: RenderContextRo? = null

	override val modelTransform = Matrix4()

	private val _modelTransformInv = Matrix4()
	override val modelTransformInv: Matrix4Ro
		get() = _modelTransformInv.set(modelTransform).inv()

	override val viewProjectionTransform: Matrix4Ro = Matrix4.IDENTITY

	override val viewProjectionTransformInv: Matrix4Ro = Matrix4.IDENTITY

	override val viewTransform: Matrix4Ro = Matrix4.IDENTITY

	override val projectionTransform: Matrix4Ro = Matrix4.IDENTITY

	override val canvasTransform: RectangleRo = Rectangle(-1f, -1f, 2f, 2f)

	override val clipRegion: MinMaxRo = MinMax()

	override val colorTint = Color.WHITE.copy()

	override fun clear() {
		modelTransform.idt()
		colorTint.set(Color.WHITE)
	}
}
