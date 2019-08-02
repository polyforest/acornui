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

import com.acornui.component.UiComponentRo
import com.acornui.Disposable
import com.acornui.LifecycleRo
import com.acornui.UpdatableChildBase
import com.acornui.di.inject

private class OnTick(
		private val component: UiComponentRo,
		private val callback: (tickTime: Float) -> Unit
) : UpdatableChildBase(), Disposable {

	private val timeDriver = component.inject(TimeDriver)

	private val componentActivatedHandler: (LifecycleRo) -> Unit = {
		timeDriver.addChild(this)
	}

	private val componentDeactivatedHandler: (LifecycleRo) -> Unit = {
		timeDriver.removeChild(this)
	}

	private val componentDisposedHandler: (LifecycleRo) -> Unit = {
		dispose()
	}

	init {
		component.activated.add(componentActivatedHandler)
		component.deactivated.add(componentDeactivatedHandler)
		component.disposed.add(componentDisposedHandler)

		if (component.isActive) {
			timeDriver.addChild(this)
		}
	}

	override fun update(tickTime: Float) {
		callback(tickTime)
	}

	override fun dispose() {
		remove()
		component.activated.remove(componentActivatedHandler)
		component.deactivated.remove(componentDeactivatedHandler)
		component.disposed.remove(componentDisposedHandler)
	}
}

/**
 * While the receiver component is activated, every time driver tick will invoke the callback.
 *
 * @return An instance that can be disposed to stop watching ticks.
 */
fun UiComponentRo.onTick(callback: (tickTime: Float) -> Unit): Disposable {
	return OnTick(this, callback)
}
