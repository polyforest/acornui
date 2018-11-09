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

package com.acornui.component.layout.algorithm

import com.acornui.component.ComponentInit
import com.acornui.component.layout.LayoutData
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

open class BasicLayoutData : LayoutData {

	protected val _changed = Signal0()
	override val changed: Signal<() -> Unit>
		get() = _changed

	protected fun <T> bindable(initial: T): ReadWriteProperty<Any?, T> {
		return Delegates.observable(initial) {
			meta, old, new ->
			if (old != new) _changed.dispatch()
		}
	}

	/**
	 * The preferred width of this component.
	 */
	var height: Float? by bindable(null)

	/**
	 * The preferred height of this component.
	 */
	var width: Float? by bindable(null)

	/**
	 * If set, this object will be set to a percent-based width.
	 */
	var widthPercent: Float? by bindable(null)

	/**
	 * If set, this object will be set to a percent-based height.
	 */
	var heightPercent: Float? by bindable(null)

	/**
	 * Sets width and height to 100%
	 */
	fun fill() {
		widthPercent = 1f
		heightPercent = 1f
	}

	open fun getPreferredWidth(availableWidth: Float?): Float? {
		return if (widthPercent == null || availableWidth == null) width else widthPercent!! * availableWidth
	}

	open fun getPreferredHeight(availableHeight: Float?): Float? {
		return if (heightPercent == null || availableHeight == null) height else heightPercent!! * availableHeight
	}
}

fun basicLayoutData(init: ComponentInit<BasicLayoutData> = {}): BasicLayoutData {
	val b = BasicLayoutData()
	b.init()
	return b
}