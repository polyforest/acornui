package com.acornui.test

import com.acornui.input.KeyInput
import com.acornui.input.interaction.CharInteractionRo
import com.acornui.input.interaction.KeyInteractionRo
import com.acornui.input.interaction.KeyLocation
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

object MockKeyState : KeyInput {
	override fun keyIsDown(keyCode: Int, location: KeyLocation): Boolean {
		return false
	}

	override val keyDown: Signal<(KeyInteractionRo) -> Unit> = emptySignal()
	override val keyUp: Signal<(KeyInteractionRo) -> Unit> = emptySignal()
	override val char: Signal<(CharInteractionRo) -> Unit> = emptySignal()

	override fun dispose() {
	}
}