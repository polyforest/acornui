/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.input

import com.acornui.Disposable
import com.acornui.DisposableBase
import com.acornui.ManagedDisposable
import com.acornui.component.ComponentInit
import com.acornui.component.Stage
import com.acornui.component.UiComponent
import com.acornui.component.stage
import com.acornui.own
import com.acornui.signal.once
import com.acornui.signal.signal
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.MouseEvent
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Provides information to buttons about the state of mouse and touch.
 * @param host The target to watch.
 */
class MouseOrTouchState(private val host: UiComponent) : DisposableBase(), ManagedDisposable {

	private val stage: Stage
		get() = host.stage


	/**
	 * Dispatched when [isOver] has changed.
	 */
	val isOverChanged = signal<Unit>()

	/**
	 * Dispatched when [isDown] has changed.
	 */
	val isDownChanged = signal<Unit>()

	/**
	 * True if either the touch or mouse is over the [host].
	 */
	var isOver: Boolean = false
		private set(value) {
			if (value == field) return
			field = value
			isOverChanged.dispatch(Unit)
		}

	/**
	 * If the mouse or touch is pressed on the target, isDown becomes true, and when the mouse or touch is released
	 * (potentially outside of the target), isDown becomes false.
	 */
	var isDown: Boolean = false
		private set(value) {
			if (value == field) return
			field = value
			isDownChanged.dispatch(Unit)
		}

	private val mouseEnteredHandler = { event: MouseEvent ->
		isOver = true
	}

	private val mouseExitedHandler = { event: MouseEvent ->
		isOver = false
	}

	private var stageMouseUpSub: Disposable? = null
	private val mousePressedHandler = { event: MouseEvent ->
		if (!isDown && event.button == WhichButton.LEFT) {
			isDown = true
			stageMouseUpSub = stage.mouseReleased.once(stageMouseUpHandler)
		}
	}

	private var stageTouchEndSub: Disposable? = null

	private val touchStartedHandler = { event: TouchEvent ->
		if (!isDown) {
			isDown = true
			stageTouchEndSub = stage.touchEnded.once(stageTouchEndHandler)
		}
	}

	private val stageMouseUpHandler = { event: MouseEvent ->
		if (event.button == WhichButton.LEFT) {
			isDown = false
		}
	}

	private val stageTouchEndHandler = { event: TouchEvent ->
		isDown = false
	}

	init {
		host.ownThis()
		own(host.mouseEntered.listen(mouseEnteredHandler))
		own(host.mouseExited.listen(mouseExitedHandler))
		own(host.mousePressed.listen(mousePressedHandler))
		own(host.touchStarted.listen(touchStartedHandler))
	}

	override fun dispose() {
		super.dispose()
		stageMouseUpSub?.dispose()
		stageMouseUpSub = null
		stageTouchEndSub?.dispose()
		stageTouchEndSub = null
	}
}

/**
 * Creates and initializes a [MouseOrTouchState] object.
 */
inline fun UiComponent.mouseOrTouchState(init: ComponentInit<MouseOrTouchState> = {}): MouseOrTouchState {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MouseOrTouchState(this).apply(init)
}