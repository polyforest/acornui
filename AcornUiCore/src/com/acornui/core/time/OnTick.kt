package com.acornui.core.time

import com.acornui.component.UiComponent
import com.acornui.core.Disposable
import com.acornui.core.UpdatableChildBase
import com.acornui.core.LifecycleRo
import com.acornui.core.di.inject

private class OnTick(private val component: UiComponent, private val callback: (stepTime: Float) -> Unit) : UpdatableChildBase(), Disposable {

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

	override fun update(stepTime: Float) {
		callback(stepTime)
	}

	override fun dispose() {
		remove()
		component.activated.remove(componentActivatedHandler)
		component.deactivated.remove(componentDeactivatedHandler)
		component.disposed.remove(componentDisposedHandler)
	}
}

/**
 * While the receiver component is activated, every frame will invoke the callback.
 *
 * @return An instance that can be disposed to stop watching frames.
 */
fun UiComponent.onTick(callback: (stepTime: Float) -> Unit): Disposable {
	return OnTick(this, callback)
}