package com.acornui.mock

import com.acornui.component.StageRo
import com.acornui.component.UiComponentRo
import com.acornui.input.InteractionEvent
import com.acornui.input.InteractionEventRo
import com.acornui.input.InteractionType
import com.acornui.input.InteractivityManager
import com.acornui.signal.StoppableSignal
import com.acornui.signal.StoppableSignalImpl

object MockInteractivityManager : InteractivityManager {

	override fun init(root: StageRo) {
	}

	override fun <T : InteractionEventRo> getSignal(host: UiComponentRo, type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T> {
		return StoppableSignalImpl()
	}

	override fun dispatch(canvasX: Float, canvasY: Float, event: InteractionEvent, useCapture: Boolean, useBubble: Boolean) {
	}

	override fun dispatch(target: UiComponentRo, event: InteractionEvent, useCapture: Boolean, useBubble: Boolean) {
	}

	override fun dispose() {
	}
}
