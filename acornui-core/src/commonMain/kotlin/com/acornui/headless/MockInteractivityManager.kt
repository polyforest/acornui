package com.acornui.headless

import com.acornui.component.UiComponentRo
import com.acornui.input.*
import com.acornui.signal.StoppableSignal
import com.acornui.signal.StoppableSignalImpl

object MockInteractivityManager : InteractivityManager {

	private lateinit var root: UiComponentRo
	override fun init(root: UiComponentRo) {
		this.root = root
	}

	override fun activeElement(value: UiComponentRo?) {
	}

	override val activeElement: UiComponentRo
		get() = root

	override fun <T : InteractionEventRo> getSignal(host: UiComponentRo, type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T> {
		return StoppableSignalImpl()
	}

	override fun dispatch(canvasX: Float, canvasY: Float, event: InteractionEvent, useCapture: Boolean, useBubble: Boolean) {
	}

	override fun dispatch(event: InteractionEvent, target: UiComponentRo, useCapture: Boolean, useBubble: Boolean) {
	}


}
