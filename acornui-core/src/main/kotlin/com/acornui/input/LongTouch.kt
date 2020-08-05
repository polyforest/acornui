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
import com.acornui.signal.*
import com.acornui.time.schedule
import kotlinx.browser.window
import org.w3c.dom.TouchEvent
import kotlin.time.seconds

val longTouchInterval = 0.8.seconds

/**
 * The [focusIn] event filtered to only dispatch when the previous focus is not a descendent of this node.
 */
val WithNode.longTouch
	get() = object : Signal<TouchEvent> {

		override fun listen(isOnce: Boolean, handler: (TouchEvent) -> Unit): SignalSubscription {
			val win = window.asWithEventTarget()
			return buildSubscription(isOnce, handler) {
				+touchStarted.listen { e ->
					val timer = +schedule(longTouchInterval) {
						invoke(e)
					}
					+win.touchEnded.once {
						timer.dispose()
					}
				}
			}
		}
	}