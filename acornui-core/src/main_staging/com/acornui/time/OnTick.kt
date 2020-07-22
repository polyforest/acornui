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

import com.acornui.*
import com.acornui.component.UiComponent

private class OnTick(
		private val component: UiComponent,
		private val callback: (tickTime: Double) -> Unit
) : Updatable, Disposable {

	override val frameDriver: FrameDriver = component.inject(frameDriverKey)

	private val componentActivatedHandler: (LifecycleRo) -> Unit = {
		start()
	}

	private val componentDeactivatedHandler: (LifecycleRo) -> Unit = {
		stop()
	}

	private val componentDisposedHandler: (LifecycleRo) -> Unit = {
		dispose()
	}

	init {
		component.activated.add(componentActivatedHandler)
		component.deactivated.add(componentDeactivatedHandler)
		component.disposed.add(componentDisposedHandler)

		if (component.isActive) {
			start()
		}
	}

	override fun update(dT: Double) {
		callback(dT)
	}

	override fun dispose() {
		stop()
		component.activated.remove(componentActivatedHandler)
		component.deactivated.remove(componentDeactivatedHandler)
		component.disposed.remove(componentDisposedHandler)
	}
}

/**
 * Invokes a callback every frame this receiver is active.
 * When the receiver component is disposed, this handle will also be disposed.
 *
 * @return An instance that can be disposed to stop watching ticks.
 */
fun UiComponent.onTick(callback: (tickTime: Double) -> Unit): Disposable {
	return OnTick(this, callback)
}
