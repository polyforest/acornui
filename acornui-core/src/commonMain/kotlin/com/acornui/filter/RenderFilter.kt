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
import com.acornui.math.*
import com.acornui.observe.Observable
import com.acornui.reflect.observable
import com.acornui.signal.Signal1
import kotlin.properties.ReadWriteProperty

/**
 * A render filter wraps the drawing of a component.
 */
interface RenderFilter : Observable {

	/**
	 * The buffer to add to the region to which this filter draws.
	 * This should be in canvas coordinates.
	 */
	val drawPadding: PadRo
		get() = Pad.EMPTY_PAD

	/**
	 * Renders the [inner] block using this filter.
	 * @param region The canvas coordinates of the region (after padding) rendered.
	 */
	fun render(region: RectangleRo, inner: ()->Unit)

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

