/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.core.focus

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.core.Disposable
import com.acornui.core.di.inject
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

class FocusAttachment(private val target: Focusable) : Disposable {

	private val focusManager = target.inject(FocusManager)

	private val _focused = Signal0()
	val focused = _focused.asRo()

	private val _blurred = Signal0()
	val blurred = _blurred.asRo()

	init {
		focusManager.focusedChanged.add(this::focusChangedHandler)
	}

	private fun focusChangedHandler(old: Focusable?, new: Focusable?) {
		if (old == target) _blurred.dispatch()
		if (new == target) _focused.dispatch()
	}

	override fun dispose() {
		focusManager.focusedChanged.remove(this::focusChangedHandler)
		_focused.dispose()
		_blurred.dispose()
	}

	companion object
}

fun UiComponentRo.focusAttachment(): FocusAttachment {
	return createOrReuseAttachment(FocusAttachment, { FocusAttachment(this) })
}

fun UiComponentRo.focused(): Signal<() -> Unit> {
	return focusAttachment().focused
}

fun UiComponentRo.blurred(): Signal<() -> Unit> {
	return focusAttachment().blurred
}