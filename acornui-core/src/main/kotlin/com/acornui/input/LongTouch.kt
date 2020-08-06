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

import com.acornui.component.WithNode
import com.acornui.dom.handle
import com.acornui.signal.*
import com.acornui.time.schedule
import kotlinx.browser.window
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.MouseEvent
import kotlin.time.seconds

val longTouchInterval = 0.6.seconds

/**
 * Dispatched when the user has touched down on this element for [longTouchInterval] duration.
 */
val WithNode.longTouched
	get() = object : Signal<TouchEvent> {

		override fun listen(isOnce: Boolean, handler: (TouchEvent) -> Unit): SignalSubscription {
			val win = window.asWithEventTarget()
			return buildSubscription(isOnce, handler) {
				+touchStarted.listen { e ->
					val timer = schedule(longTouchInterval) {
						win.contextMenu.listen(EventOptions(isCapture = true, isOnce = true, isPassive = false)) {
							it.preventDefault()
							it.handle()
						}
						invoke(e)
					}
					win.touchEnded.once {
						timer.dispose()
					}
				}
			}
		}
	}

/**
 * Dispatched when the user has moused down on this element for [longTouchInterval] duration.
 */
val WithNode.longPressed
	get() = object : Signal<MouseEvent> {

		override fun listen(isOnce: Boolean, handler: (MouseEvent) -> Unit): SignalSubscription {
			val win = window.asWithEventTarget()
			return buildSubscription(isOnce, handler) {
				+mousePressed.listen { e ->
					val timer = schedule(longTouchInterval) {
						win.clicked.listen(EventOptions(isCapture = true, isOnce = true, isPassive = false)) {
							it.preventDefault()
							it.handle()
						}
						invoke(e)
					}
					win.mouseReleased.once {
						timer.dispose()
					}
				}
			}
		}
	}

