package com.acornui.test

import com.acornui.input.MouseState
import com.acornui.input.WhichButton
import com.acornui.input.interaction.TouchRo
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

object MockMouseState : MouseState {
	override val touchModeChanged: Signal<() -> Unit> = emptySignal()
	override val touchMode: Boolean = false
	override val overCanvasChanged: Signal<(Boolean) -> Unit> = emptySignal()
	override val overCanvas: Boolean = false
	override val canvasX: Float = 0f
	override val canvasY: Float = 0f
	override val touches: List<TouchRo> = emptyList()

	override fun mouseIsDown(button: WhichButton): Boolean {
		return false
	}

	override fun dispose() {
	}
}