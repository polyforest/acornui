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

package com.acornui.input.interaction

import com.acornui.Disposable
import com.acornui.component.*
import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleType
import com.acornui.di.ContextImpl
import com.acornui.function.as1
import com.acornui.input.*
import com.acornui.time.nowMs
import com.acornui.time.timer
import kotlin.time.Duration
import kotlin.time.seconds


fun UiComponent.enableDownRepeat() {
//	return createOrReuseAttachment(DownRepeat) { DownRepeat(this) }
}

/**
 * Sets this component to dispatch a mouse down event repeatedly after holding down on the target.
 * @param repeatDelay The number of seconds after holding down the target to start repeat dispatching.
 * @param repeatInterval Once the repeat dispatching begins, subsequent events are dispatched at this interval (in
 * seconds).
 */
fun UiComponent.enableDownRepeat(repeatDelay: Duration, repeatInterval: Duration) {
//	return createOrReuseAttachment(DownRepeat) {
//		val dR = DownRepeat(this)
//		dR.style.repeatDelay = repeatDelay
//		dR.style.repeatInterval = repeatInterval
//		dR
//	}
}

fun UiComponent.disableDownRepeat() {
//	removeAttachment<DownRepeat>(DownRepeat)?.dispose()
}

class DownRepeatStyle : ObservableBase() {
	override val type = Companion

	var repeatDelay by prop(0.24.seconds)
	var repeatInterval by prop(0.02.seconds)

	companion object : StyleType<DownRepeatStyle>
}