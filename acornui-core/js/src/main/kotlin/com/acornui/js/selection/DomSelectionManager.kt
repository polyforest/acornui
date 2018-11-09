package com.acornui.js.selection

import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionRange
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import com.acornui.signal.Signal2
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.browser.document

class DomSelectionManager(
		private val root: HTMLElement
) : SelectionManager {

	private val _selectionChanged = Signal2<List<SelectionRange>, List<SelectionRange>>()
	override val selectionChanged: Signal<(List<SelectionRange>, List<SelectionRange>) -> Unit>
		get() = _selectionChanged

	override var selection: List<SelectionRange> = listOf()

	private val selectionChangedHandler = {
		event: Event ->
//		_selectionChanged.dispatch()
	}

	init {
		document.addEventListener("selectionchange", selectionChangedHandler)
	}

	override fun dispose() {
		document.removeEventListener("selectionchange", selectionChangedHandler)
		_selectionChanged.dispose()
	}
}