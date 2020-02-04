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

package com.acornui.headless

import com.acornui.WindowConfig
import com.acornui.browser.Location
import com.acornui.graphic.Window
import com.acornui.signal.*

/**
 * HeadlessWindow is used for testing and headless applications.  It mocks everything except for close/closing logic.
 */
class HeadlessWindow(config: WindowConfig) : Window {

	private val _closeRequested = Signal1<Cancel>()
	override val closeRequested: Signal<(Cancel) -> Unit> = _closeRequested.asRo()
	private val _isActiveChanged = Signal1<Boolean>()
	override val isActiveChanged = _isActiveChanged.asRo()
	override var isActive: Boolean = false
		set(value) {
			field = value
			_isActiveChanged.dispatch(value)
		}

	private val _isVisibleChanged = Signal1<Boolean>()
	override val isVisibleChanged = _isVisibleChanged.asRo()
	override var isVisible: Boolean = false
		set(value) {
			field = value
			_isVisibleChanged.dispatch(value)
		}

	private val _sizeChanged = Signal2<Float, Float>()
	override val sizeChanged = _sizeChanged.asRo()

	private val _scaleChanged = Signal2<Float, Float>()
	override val scaleChanged = _scaleChanged.asRo()

	override val refresh: Signal<() -> Unit> = emptySignal()

	override var width: Float = config.initialWidth
		private set

	override var height: Float = config.initialHeight
		private set

	override val framebufferWidth: Int = config.initialWidth.toInt()
	override val framebufferHeight: Int = config.initialHeight.toInt()

	override var scaleX: Float = 1f
		private set

	override var scaleY: Float = 1f
		private set

	private val closeCancel = Cancel()
	private var closeIsRequested = false

	override fun setSize(width: Float, height: Float) {
		this.width = width
		this.height = height
		_sizeChanged.dispatch(width, height)
	}

	fun setScale(scaleX: Float, scaleY: Float) {
		this.scaleX = scaleX
		this.scaleY = scaleY
		_scaleChanged.dispatch(scaleX, scaleY)
		_sizeChanged.dispatch(width, height)
	}

	private var renderRequested = false
	override var continuousRendering: Boolean = false

	override fun shouldRender(clearRenderRequest: Boolean): Boolean {
		val shouldRender = continuousRendering || renderRequested
		if (clearRenderRequest && renderRequested) renderRequested = false
		return shouldRender
	}

	override fun requestRender() {
		renderRequested = true
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
		_isActiveChanged.dispose()
		_sizeChanged.dispose()
		_scaleChanged.dispose()
		_isVisibleChanged.dispose()
	}
}