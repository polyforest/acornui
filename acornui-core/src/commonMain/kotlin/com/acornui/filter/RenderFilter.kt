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
import com.acornui.di.Owned
import com.acornui.di.OwnedImpl
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.math.RectangleRo
import com.acornui.observe.Observable
import com.acornui.reflect.observable
import com.acornui.signal.Signal1
import kotlin.properties.ReadWriteProperty

/**
 * A render filter wraps the drawing of a component.
 */
interface RenderFilter : Observable {

	/**
	 * Updates the world vertices for any decorated components used in this filter's rendering.
	 * @param region The inner region (before transformed padding) rendered. This will be in canvas coordinates.
	 * @param transform The world transform of the filtered container. This should be used to transform padding,
	 * offsets, or any other coordinates in local space.
	 * @param tint The world color tint of the filtered container.
	 * @return Returns the expanded draw region in canvas coordinates.
	 */
	fun updateWorldVertices(region: RectangleRo, transform: Matrix4Ro, tint: ColorRo): RectangleRo = region

	/**
	 * Renders the [inner] block using this filter.

	 */
	fun render(inner: () -> Unit)

}

/**
 * The base class for render filters.
 */
abstract class RenderFilterBase(owner: Owned) : OwnedImpl(owner), RenderFilter, Disposable {

	private val _changed = Signal1<Observable>()
	override val changed = _changed.asRo()

	protected val glState by GlState
	protected val gl by Gl20

	var enabled: Boolean by bindable(true)

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

	override fun dispose() {
		super.dispose()
		_changed.dispose()
	}
}

