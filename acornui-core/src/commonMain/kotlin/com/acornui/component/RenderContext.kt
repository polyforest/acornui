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

import com.acornui.Disposable
import com.acornui.RedrawRegions
import com.acornui.RedrawRegionsImpl
import com.acornui.collection.forEach2
import com.acornui.di.DKey
import com.acornui.di.Injector
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.function.as2
import com.acornui.gl.core.*
import com.acornui.graphic.*
import com.acornui.math.*
import com.acornui.recycle.Clearable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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

	/**
	 * The regions that should be redrawn in the next render.
	 */
	val redraw: RedrawRegions

	/**
	 * True if this component does drawing.
	 *
	 * Components where this is set to true will invalidate their redraw regions with the render context.
	 * Any descendents of a container with draws == true will skip their region checks; that is, if the container
	 * needs redrawing, the children need redrawing.
	 */
	val draws: Boolean

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
 * This is the default parent render context for any component without a parent, such as the Stage or components not
 * on the display graph.
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

	override val redraw: RedrawRegions = RedrawRegionsImpl()

	override var draws: Boolean = false

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

	private val allProps = ArrayList<CachedProp<*>>()

	private var _parentContext: RenderContextRo? = null
	override var parentContext: RenderContextRo
		get() = _parentContext!!
		set(value) {
			if (value === this)
				throw Exception("Cannot set parentContext to self.")
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
	override val modelTransform: Matrix4Ro by prop {
		modelTransformOverride ?: _modelTransform.set(parentContext.modelTransform).mul(modelTransformLocal)
	}

	private val _modelTransformInv by lazy { Matrix4() }
	override val modelTransformInv: Matrix4Ro by prop {
		_modelTransformInv.set(modelTransform).inv()
	}

	override val viewProjectionTransform: Matrix4Ro by prop {
		cameraOverride?.combined ?: parentContext.viewProjectionTransform
	}

	override val viewProjectionTransformInv: Matrix4Ro by prop {
		cameraOverride?.combinedInv ?: parentContext.viewProjectionTransformInv
	}

	override val viewTransform: Matrix4Ro by prop {
		cameraOverride?.view ?: parentContext.viewTransform
	}

	override val projectionTransform: Matrix4Ro by prop {
		cameraOverride?.projection ?: parentContext.projectionTransform
	}


	override val canvasTransform: RectangleRo by prop {
		canvasTransformOverride ?: parentContext.canvasTransform
	}

	private val _clipRegionIntersection: MinMax = MinMax()
	override val clipRegion: MinMaxRo by prop {
		clipRegionOverride ?: if (clipRegionLocal == null) parentContext.clipRegion
		else localToCanvas(_clipRegionIntersection.set(clipRegionLocal!!)).intersection(parentContext.clipRegion)
	}

	private val _colorTint = Color()
	override val colorTint: ColorRo by prop {
		colorTintOverride ?: _colorTint.set(parentContext.colorTint).mul(colorTintLocal).clamp()
	}

	var redrawOverride: RedrawRegions? = null

	override val redraw: RedrawRegions by prop {
		redrawOverride ?: parentContext.redraw ?: RedrawRegions.ALWAYS
	}

	var drawsSelf = false
	override val draws: Boolean by prop {
		drawsSelf || parentContext.draws
	}

	fun invalidate() {
		allProps.forEach2 { it.clear() }
	}

	override fun clear() {
		_parentContext = null
		cameraOverride = null
		canvasTransformOverride = null
		clipRegionLocal = null
		colorTintLocal.set(Color.WHITE)
		colorTintOverride = null
		modelTransformLocal.idt()
		modelTransformOverride = null
		redrawOverride = null
		invalidate()
	}

	private fun <T> prop(calculator: () -> T) = CachedProp(calculator)

	private class CachedProp<T>(
			val calculator: () -> T
	) : ReadOnlyProperty<RenderContext, T>, Clearable {

		private var cachedIsSet = false
		private var cached: T? = null

		override fun getValue(thisRef: RenderContext, property: KProperty<*>): T {
			if (!cachedIsSet) {
				cachedIsSet = true
				cached = calculator()
			}
			@Suppress("UNCHECKED_CAST")
			return cached as T
		}

		override fun clear() {
			cachedIsSet = false
			cached = null
		}

		operator fun provideDelegate(
				thisRef: RenderContext,
				prop: KProperty<*>
		): CachedProp<T> {
			thisRef.allProps.add(this)
			return this
		}
	}
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

	override val redraw: RedrawRegions = RedrawRegions.ALWAYS

	override var draws: Boolean = false

	override fun clear() {
		modelTransform.idt()
		colorTint.set(Color.WHITE)
	}
}

/**
 * Sets the camera uniforms using the given [renderContext].
 */
fun Uniforms.setCamera(renderContext: RenderContextRo, useModel: Boolean = false) {
	if (useModel) setCamera(renderContext.viewProjectionTransform, renderContext.viewTransform, renderContext.modelTransform)
	else setCamera(renderContext.viewProjectionTransform, renderContext.viewTransform, Matrix4.IDENTITY)
}

/**
 * Sets the camera uniforms using the given [renderContext], calls [inner], then sets the camera back to what it
 * previously was.
 */
fun Uniforms.useCamera(renderContext: RenderContextRo, useModel: Boolean = false, inner: () -> Unit) {
	if (useModel) useCamera(renderContext.viewProjectionTransform, renderContext.viewTransform, renderContext.modelTransform, inner)
	else useCamera(renderContext.viewProjectionTransform, renderContext.viewTransform, Matrix4.IDENTITY, inner)
}