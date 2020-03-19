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

package com.acornui

import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.time.FrameDriverRo

/**
 * An object with a lifecycle. Create, Dispose, Activate, Deactivate
 *
 * @author nbilyk
 */
interface LifecycleRo {

	/**
	 * Dispatched when this object has been activated.
	 */
	val activated: Signal<(self: LifecycleRo) -> Unit>

	/**
	 * Dispatched when this object has been deactivated.
	 */
	val deactivated: Signal<(self: LifecycleRo) -> Unit>

	/**
	 * Dispatched when this object has been disposed.
	 */
	val disposed: Signal<(self: LifecycleRo) -> Unit>

	/**
	 * Returns true if this object is currently active.
	 */
	val isActive: Boolean

	/**
	 * Returns true if this object has been disposed.
	 */
	val isDisposed: Boolean
}

interface Lifecycle : LifecycleRo, Disposable {

	/**
	 * Invoke when this object is to become active.
	 */
	fun activate()

	/**
	 * Invoke when this object is no longer active.
	 */
	fun deactivate()
}

abstract class LifecycleBase : Lifecycle {

	private val _activated = Signal1<Lifecycle>()
	override val activated = _activated.asRo()
	private val _deactivated = Signal1<Lifecycle>()
	override val deactivated = _deactivated.asRo()
	private val _disposed = Signal1<Lifecycle>()
	override val disposed = _disposed.asRo()

	private var _isDisposed: Boolean = false
	private var _isDisposing: Boolean = false
	private var _isActive: Boolean = false

	override val isActive: Boolean
		get() = _isActive

	override val isDisposed: Boolean
		get() = _isDisposed

	final override fun activate() {
		if (_isDisposed)
			throw DisposedException()
		if (_isActive)
			throw IllegalStateException("Already active")
		_isActive = true
		onActivated()
		_activated.dispatch(this)
	}

	protected open fun onActivated() {}

	final override fun deactivate() {
		if (_isDisposed) throw DisposedException()
		if (!_isActive) throw IllegalStateException("Not active")
		_isActive = false
		onDeactivated()
		_deactivated.dispatch(this)
	}

	protected open fun onDeactivated() {
	}

	override fun dispose() {
		if (_isDisposed)
			throw DisposedException()
		if (_isDisposing) return
		_isDisposing = true
		if (isActive) {
			deactivate()
		}
		_disposed.dispatch(this)
		_isDisposed = true
		_disposed.dispose()
		_activated.dispose()
		_deactivated.dispose()
		_isDisposing = false
	}
}

interface Updatable {

	/**
	 * The frame driver this Updatable instance will use when [start] is called.
	 */
	val frameDriver: FrameDriverRo

	/**
	 * Updates this object.
	 * @param dT The number of seconds since the last update. This will be at most [AppConfig.frameTime].
	 */
	fun update(dT: Float)
}

/**
 * Removes this instance's [Updatable.update] from the frame driver.
 * @return Returns `this`
 */
fun <T : Updatable> T.stop(): T {
	frameDriver.remove(::update)
	return this
}

/**
 * Adds this instance's [Updatable.update] to the frame driver.
 * @return Returns `this`
 */
fun <T : Updatable> T.start(): T {
	frameDriver.add(::update)
	return this
}

/**
 * Returns true if this updatable instance is currently being driven.
 * @see start
 */
val Updatable.isDriven: Boolean
	get() = frameDriver.contains(::update)