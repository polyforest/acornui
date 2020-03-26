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

import com.acornui.Disposable
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.gl.core.CachedGl20
import com.acornui.graphic.ColorRo
import com.acornui.graphic.Window
import com.acornui.math.Matrix4Ro
import com.acornui.math.Rectangle
import com.acornui.observe.Observable
import com.acornui.properties.afterChange
import com.acornui.signal.Signal1
import kotlin.properties.ReadWriteProperty

/**
 * A render filter wraps the drawing of a component.
 */
interface RenderFilter : Observable {

	/**
	 *
	 * @param region The local region being rendered. This may be mutated to represent the new local region rendered.
	 * For example `BlurFilter` will expand region by its blurX and blurY values.
	 * `region.inflate(blurY, blurX, blurY, blurX)`
	 */
	fun region(region: Rectangle) {}

	/**
	 * Updates the global vertices for any decorated components used in this filter's rendering.
	 * @param transform The global transform of the filtered container.
	 * @param tint The global color tint of the filtered container.
	 */
	fun updateGlobalVertices(transform: Matrix4Ro, tint: ColorRo) {}

	/**
	 * Renders the [inner] block to any framebuffers using this filter and returns the expanded draw region this filter
	 * covers.
	 *
	 * The camera will be set to an orthographic projection with the model transformation set the inverse of the
	 * filtered container's global model transform.
	 *
	 * @param inner A method that draws the contents this filter decorates.
	 */
	fun renderLocal(inner: () -> Unit) {}

	/**
	 * Renders to the screen.
	 *
	 * @param inner A method that draws the contents this filter decorates.
	 */
	fun render(inner: () -> Unit)

}

/**
 * The base class for render filters.
 */
abstract class RenderFilterBase(owner: Context) : ContextImpl(owner), RenderFilter, Disposable {

	private val _changed = Signal1<Observable>()
	override val changed = _changed.asRo()

	protected val gl by CachedGl20
	protected val window by Window

	protected val scaleX: Float
		get() = window.scaleX

	protected val scaleY: Float
		get() = window.scaleY

	var enabled: Boolean by bindable(true)

	/**
	 * When the property has changed, [changed] will be dispatched.
	 */
	protected fun <T> bindable(initial: T, inner: (T) -> Unit = {}): ReadWriteProperty<Any?, T> = afterChange(initial) {
		inner(it)
		notifyChanged()
	}

	/**
	 * When the property has changed, [changed] will be dispatched.
	 * Additionally, [inner] will be invoked immediately with the initial value.
	 */
	protected fun <T> bindableAndCall(initial: T, inner: (T) -> Unit): ReadWriteProperty<Any?, T> {
		return afterChange(initial) {
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

	override fun dispose() {
		super.dispose()
		_changed.dispose()
	}
}

