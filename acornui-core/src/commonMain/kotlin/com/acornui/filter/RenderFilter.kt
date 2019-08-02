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

package com.acornui.filter

import com.acornui.component.RenderContextRo
import com.acornui.component.layout.Sizable
import com.acornui.Disposable
import com.acornui.Renderable
import com.acornui.di.Owned
import com.acornui.di.OwnedImpl
import com.acornui.gl.core.GlState
import com.acornui.math.*
import com.acornui.observe.Observable
import com.acornui.reflect.observable
import com.acornui.signal.Signal1
import kotlin.properties.ReadWriteProperty

/**
 * A render filter wraps the drawing of a component.
 */
interface RenderFilter : Renderable, Observable {

	/**
	 * The contents this render filter should wrap.
	 */
	var contents: Renderable?

	/**
	 * Marks any bitmap caches (if there are any) as invalid and need to be redrawn.
	 */
	fun invalidateBitmapCache()

}

/**
 * The base class for render filters.
 */
abstract class RenderFilterBase(owner: Owned) : OwnedImpl(owner), RenderFilter, Sizable, Disposable {

	private val _changed = Signal1<Observable>()
	override val changed = _changed.asRo()

	protected val glState by GlState

	protected var bitmapCacheIsValid = false
		private set

	var enabled: Boolean by bindable(true)

	/**
	 * True if this filter should be skipped.
	 */
	protected open val shouldSkipFilter: Boolean
		get() = !enabled

	override var contents: Renderable? = null
		set(value) {
			if (value === this) throw Exception("Cannot set contents to self.")
			field = value
			invalidateBitmapCache()
		}

	override fun invalidateBitmapCache() {
		bitmapCacheIsValid = false
	}

	/**
	 * The padding this filter should inflate the [bounds] in order to calculate the [drawRegion].
	 */
	open val drawPadding: PadRo = Pad.EMPTY_PAD

	override val bounds: BoundsRo
		get() = contents?.bounds ?: Bounds.EMPTY_BOUNDS

	private val _drawRegion = MinMax()

	/**
	 * @see Renderable.drawRegion
	 */
	override val drawRegion: MinMaxRo
		get() = _drawRegion.set(contents?.drawRegion).inflate(drawPadding)

	/**
	 * Configures a padding object to represent the shift needed to draw a rasterized representation of the contents
	 * in the contents coordinate space.
	 */
	protected fun setDrawPadding(padding: Pad) {
		val drawRegion = drawRegion
		val bounds = bounds
		padding.set(drawRegion.yMin, bounds.width - drawRegion.xMax, bounds.height - drawRegion.yMax, drawRegion.xMin)
	}

	/**
	 * When the property has changed, [changed] will be dispatched.
	 */
	protected fun <T> bindable(initial: T, inner: (T) -> Unit = {}): ReadWriteProperty<Any?, T> = observable(initial) {
		inner(it)
		notifyChanged()
	}

	/**
	 * When the property has changed, [changed] will be dispatched.
	 * Additionally, [inner] will be invoked immediately with the initial value.
	 */
	protected fun <T> bindableAndCall(initial: T, inner: (T) -> Unit): ReadWriteProperty<Any?, T> {
		return observable(initial) {
			inner(it)
			notifyChanged()
		}.also { inner(initial) }
	}

	/**
	 * Dispatches the [changed] signal.
	 */
	protected fun notifyChanged() {
		_changed.dispatch(this)
	}

	final override val explicitWidth: Float?
		get() = contents?.explicitWidth

	final override val explicitHeight: Float?
		get() = contents?.explicitHeight

	final override fun setSize(width: Float?, height: Float?) {
		contents?.setSize(width, height)
	}

	final override fun render(renderContext: RenderContextRo) {
		if (shouldSkipFilter) contents?.render(renderContext)
		else draw(renderContext)
		bitmapCacheIsValid = true
	}

	/**
	 * Renders this filter.
	 * This will only be called if [shouldSkipFilter] is false.
	 */
	protected abstract fun draw(renderContext: RenderContextRo)

	override fun dispose() {
		super.dispose()
		contents = null
		_changed.dispose()
	}
}

