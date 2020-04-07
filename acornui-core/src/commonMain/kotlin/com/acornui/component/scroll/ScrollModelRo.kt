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

package com.acornui.component.scroll

import com.acornui.Disposable
import com.acornui.math.MathUtils.clamp
import com.acornui.math.MathUtils.roundToNearest
import com.acornui.observe.Observable
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

interface ScrollModelRo : Observable {

	/**
	 * Dispatched when the value property has changed.
	 */
	override val changed: Signal<(ScrollModelRo) -> Unit>

	/**
	 * The undecorated value.
	 * When this has changed, the [changed] signal is dispatched.
	 *
	 * @see [value]
	 */
	val rawValue: Float

	/**
	 * The decorated value. Implementations may for example have clamping and/or snapping applied.
	 * Use [rawValue] to set and retrieve an unbounded value.
	 */
	val value: Float
		get() = rawToDecorated(rawValue)

	/**
	 * Applies decoration to the given value. Implementations may for example apply clamping and/or snapping.
	 */
	fun rawToDecorated(rawValue: Float): Float = rawValue
}

interface ScrollModel : ScrollModelRo {

	override var rawValue: Float

	/**
	 * Getting is equivalent to `rawToDecorated(rawValue)`.
	 * Setting is the same as `rawValue = rawToDecorated(value)`
	 */
	override var value: Float
		get() = rawToDecorated(rawValue)
		set(value) {
			val newRawValue = rawToDecorated(value)
			if (newRawValue == rawValue) return
			rawValue = newRawValue
		}
}

interface ClampedScrollModelRo : ScrollModelRo {

	/**
	 * Dispatched when the min, max, or value properties have changed.
	 */
	override val changed: Signal<(ClampedScrollModelRo) -> Unit>

	/**
	 * The min value.  When changed a changed signal is dispatched.
	 */
	val min: Float

	/**
	 * The max value.  When changed a changed signal is dispatched.
	 */
	val max: Float

	/**
	 * The snapping delta. This causes the [value] to snap to the nearest interval. The
	 * interval begins at [min].
	 */
	val snap: Float

	/**
	 * Returns the given value, clamped within the min and max values.
	 * The min bound takes precedence over max.
	 */
	fun clamp(value: Float): Float = clamp(value, min, max)

	/**
	 * Returns the given value, snapped to the nearest [snap] interval, starting from [min].
	 */
	fun snap(value: Float): Float = roundToNearest(value, snap, min)

	/**
	 * Snaps and then clamps the given value.
	 */
	override fun rawToDecorated(rawValue: Float): Float {
		return clamp(snap(rawValue))
	}
}

/**
 * A mutable scroll model with clamping.
 */
interface ClampedScrollModel : ClampedScrollModelRo, ScrollModel {

	override var min: Float
	override var max: Float
	override var snap: Float
}

/**
 * A model representation of the scrolling values
 */
class ScrollModelImpl(
		value: Float = 0f,
		min: Float = 0f,
		max: Float = 0f,
		snap: Float = 0f
) : ClampedScrollModel, Disposable {

	private val _changed = Signal1<ClampedScrollModel>()
	override val changed = _changed.asRo()

	private fun bindable(initial: Float): ReadWriteProperty<Any?, Float> {
		return Delegates.observable(initial) { _, old, new ->
			check(!new.isNaN()) { "Cannot set scroll model to NaN" }
			if (old != new)
				_changed.dispatch(this)
		}
	}

	override var min by bindable(min)

	override var max by bindable(max)

	override var snap by bindable(snap)

	override var rawValue by bindable(value)

	override fun toString(): String {
		return "[ScrollModelRo value=$rawValue min=$min max=$max"
	}

	override fun dispose() {
		_changed.dispose()
	}
}
