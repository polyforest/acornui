/*
 * Copyright 2017 Nicholas Bilyk
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

import com.acornui.core.Disposable
import com.acornui.math.MathUtils
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlin.math.round
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

interface ScrollModelRo {

	/**
	 * Dispatched when the value property has changed.
	 */
	val changed: Signal<(ScrollModelRo) -> Unit>

	/**
	 * The undecorated value.
	 * When this has changed, the [changed] signal is dispatched.
	 *
	 * @see [value]
	 */
	val rawValue: Float

	/**
	 * The decorated value. Implementations may have clamping or snapping applied. Use [rawValue] to set and retrieve an
	 * unbounded value.
	 */
	val value: Float
		get() = rawValue
}

interface ScrollModel : ScrollModelRo {

	override var rawValue: Float

	override var value: Float
		get() = rawValue
		set(value) {
			if (value == rawValue) return
			rawValue = value
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
	fun clamp(value: Float): Float {
		return MathUtils.clamp(value, min, max)
	}

	/**
	 * Returns the given value, snapped to the nearest [snap] interval, starting from [min].
	 */
	fun snap(value: Float): Float {
		if (snap <= 0) return value
		var v = value - min
		v /= snap
		v = round(v)
		v *= snap
		return v + min
	}

	/**
	 * The decorated value. This may have clamping or snapping applied. Use [rawValue] to set and retrieve an
	 * unbounded value.
	 */
	override val value: Float
		get() {
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

	override var value: Float
		get() = clamp(snap(rawValue))
		set(value) {
			val newValue = clamp(value)
			if (newValue == this.rawValue) return
			rawValue = newValue
		}
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
	override val changed: Signal<(ClampedScrollModel) -> Unit>
		get() = _changed

	private fun bindable(initial: Float): ReadWriteProperty<Any?, Float> {
		return Delegates.observable(initial, {
			meta, old, new ->
			if (new.isNaN())
				throw Exception("Cannot set scroll model to NaN")
			if (old != new) _changed.dispatch(this)}
		)
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