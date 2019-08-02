package com.acornui.test

import com.acornui.browser.Location
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.Window
import com.acornui.signal.Cancel
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

object MockWindow : Window {

	override val closeRequested: Signal<(Cancel) -> Unit> = emptySignal()
	override val isActiveChanged: Signal<(Boolean) -> Unit> = emptySignal()
	override val isActive: Boolean = false
	override val isVisibleChanged: Signal<(Boolean) -> Unit> = emptySignal()
	override val isVisible: Boolean = false
	override val sizeChanged: Signal<(Float, Float) -> Unit> = emptySignal()
	override val scaleChanged: Signal<(Float, Float) -> Unit> = emptySignal()
	override val width: Float = 0f
	override val height: Float = 0f
	override val framebufferWidth: Int = 0
	override val framebufferHeight: Int = 0
	override val scaleX: Float = 1f
	override val scaleY: Float = 1f

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
		return false
	}

	override fun requestClose() {
	}

	override fun alert(message: String) {
	}

	override val fullScreenChanged: Signal<() -> Unit> = emptySignal()
	override val fullScreenEnabled: Boolean = false
	override var fullScreen: Boolean = false
	override val location: Location = MockLocation

	override fun dispose() {
	}
}