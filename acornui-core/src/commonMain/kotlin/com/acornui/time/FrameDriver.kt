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

import com.acornui.Updatable
import com.acornui.recycle.Clearable
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * @author nbilyk
 */
object FrameDriver : Signal<FrameCallback>, Clearable {

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

	fun dispatch(dT: Float) {
		signal.dispatch(dT)
	}
	override fun clear() = signal.clear()
}

typealias FrameCallback = (dT: Float) -> Unit

/**
 * Removes this instance's [Updatable.update] from the frame driver.
 */
fun <T : Updatable> T.stop(): T {
	FrameDriver.remove(::update)
	return this
}

/**
 * Adds this instance's [Updatable.update] to the frame driver.
 */
fun <T : Updatable> T.start(): T {
	FrameDriver.add(::update)
	return this
}

val Updatable.isDriven: Boolean
	get() = FrameDriver.contains(::update)

/**
 * A blocking function to create a frame loop until [inner] returns false.
 * @param frameTime The desired interval between frames. This will have no effect on JS backends.
 * @see loopFrames
 */
expect suspend fun loopWhile(frameTime: Duration = (1.0 / 50.0).seconds, inner: (dT: Float) -> Boolean)

/**
 * Invokes [FrameDriver.dispatch] and [inner] on every frame until [inner] returns false.
 * @see loopWhile
 */
suspend fun loopFrames(frameTime: Duration = (1.0 / 50.0).seconds, inner: (dT: Float) -> Boolean) {
	loopWhile(frameTime) {
		FrameDriver.dispatch(it)
		inner(it)
	}
}

