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

package com.acornui.time

import com.acornui.di.Context
import com.acornui.recycle.Clearable
import com.acornui.signal.Signal
import com.acornui.signal.Signal1

interface FrameDriverRo : Signal<FrameCallback> {

	companion object : Context.Key<FrameDriverRo>
}

interface FrameDriver : FrameDriverRo, Clearable {

	/**
	 * Dispatches the frame driver.
	 */
	fun dispatch(dT: Float)

	companion object : Context.Key<FrameDriver> {
		override val extends: Context.Key<*>?
			get() = FrameDriverRo
	}
}

/**
 * The frame driver is a signal that is invoked every frame before any applications are updated and rendered.
 * @author nbilyk
 */
class FrameDriverImpl : FrameDriver {

	private val signal = Signal1<Float>()

	override val isDispatching: Boolean
		get() = signal.isDispatching

	override fun isNotEmpty(): Boolean = signal.isNotEmpty()
	override fun isEmpty(): Boolean = signal.isNotEmpty()
	override fun add(handler: FrameCallback, isOnce: Boolean) = signal.add(handler, isOnce)
	override fun remove(handler: FrameCallback) = signal.remove(handler)
	override fun contains(handler: FrameCallback): Boolean = signal.contains(handler)
	override fun addBinding(callback: () -> Unit) = signal.addBinding(callback)
	override fun removeBinding(callback: () -> Unit) = signal.removeBinding(callback)

	override fun dispatch(dT: Float) = signal.dispatch(dT)

	override fun clear() = signal.clear()
}

typealias FrameCallback = (dT: Float) -> Unit