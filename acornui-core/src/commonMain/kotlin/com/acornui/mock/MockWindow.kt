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

package com.acornui.mock

import com.acornui.browser.Location
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.Window
import com.acornui.signal.Cancel
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.signal.emptySignal

class MockWindow : Window {

	private val _closeRequested = Signal1<Cancel>()
	override val closeRequested: Signal<(Cancel) -> Unit> = _closeRequested.asRo()
	override val isActiveChanged: Signal<(Boolean) -> Unit> = emptySignal()
	override val isActive: Boolean = false
	override val isVisibleChanged: Signal<(Boolean) -> Unit> = emptySignal()
	override val isVisible: Boolean = false
	override val sizeChanged: Signal<(Float, Float) -> Unit> = emptySignal()
	override val scaleChanged: Signal<(Float, Float) -> Unit> = emptySignal()
	override val width: Float = 1000f
	override val height: Float = 1000f
	override val framebufferWidth: Int = 0
	override val framebufferHeight: Int = 0
	override val scaleX: Float = 1f
	override val scaleY: Float = 1f

	private val closeCancel = Cancel()
	private var closeIsRequested = false

	override fun setSize(width: Float, height: Float) {
	}

	override var clearColor: ColorRo
		get() = Color.CLEAR
		set(value) {}

	override var continuousRendering: Boolean = false

	override fun shouldRender(clearRenderRequest: Boolean): Boolean {
		return false
	}

	override fun requestRender() {
	}

	override fun renderBegin() {
	}

	override fun renderEnd() {
	}

	override fun isCloseRequested(): Boolean {
		return closeIsRequested
	}

	override fun requestClose(force: Boolean) {
		_closeRequested.dispatch(closeCancel.reset())
		if (!closeCancel.canceled) {
			closeIsRequested = true
		}
	}

	override fun alert(message: String) {
	}

	override val fullScreenChanged: Signal<() -> Unit> = emptySignal()
	override val fullScreenEnabled: Boolean = false
	override var fullScreen: Boolean = false
	override val location: Location = MockLocation

	override fun dispose() {
		_closeRequested.dispose()
	}
}