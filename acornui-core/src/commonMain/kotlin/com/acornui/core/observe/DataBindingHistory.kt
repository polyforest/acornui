package com.acornui.core.observe

import com.acornui.collection.poll
import com.acornui.collection.pop
import com.acornui.component.UiComponentRo
import com.acornui.core.Disposable
import com.acornui.core.di.own
import com.acornui.core.input.interaction.UndoInteractionRo
import com.acornui.core.input.interaction.redo
import com.acornui.core.input.interaction.undo
import com.acornui.core.time.delayedCallback
import com.acornui.function.as2
import com.acornui.recycle.Clearable

class DataBindingHistory<T>(
		private val host: UiComponentRo,
		private val dataBinding: DataBinding<T>
) : Clearable, Disposable {

	var maxHistory = 200
	private val _history = ArrayList<T>()
	val history: List<T> = _history

	private var cursor: Int = -1
	private var isDispatching = false

	private val delayedPush = host.delayedCallback(1f, ::pushState)

	init {
		host.own(this)
		host.undo().add(this::undoHandler)
		host.redo().add(this::redoHandler)

		dataBinding.changed.add(::dataBindingChangedHandler.as2)
		pushState()
	}

	private fun dataBindingChangedHandler() {
		if (!isDispatching)
			delayedPush()
	}

	private fun undoHandler(undoInteraction: UndoInteractionRo) {
		if (undoInteraction.handled) return
		undoInteraction.handled = true
		undo()
	}

	private fun redoHandler(redoInteraction: UndoInteractionRo) {
		if (redoInteraction.handled) return
		redoInteraction.handled = true
		redo()
	}

	private fun redo() {
		if (isDispatching)
			throw IllegalStateException("Cannot redo while dispatching.")
		if (cursor >= _history.lastIndex) return
		isDispatching = true
		val nextState = _history[++cursor]
		dataBinding.value = nextState
		isDispatching = false
	}

	private fun undo() {
		if (isDispatching)
			throw IllegalStateException("Cannot undo while dispatching.")
		if (cursor == 0) return
		isDispatching = true
		val previousState = _history[--cursor]
		dataBinding.value = previousState
		isDispatching = false
	}

	private fun pushState() {
		if (cursor != history.lastIndex) {
			// If we have undone states and are now pushing a new state, then remove the undone states from the history.
			while (_history.lastIndex > cursor)
				_history.pop()
		}
		_history.add(dataBinding.value)
		if (_history.size > maxHistory) _history.poll() else cursor++
	}

	override fun clear() {
		_history.clear()
		cursor = -1
		pushState()
		delayedPush.dispose()
	}

	override fun dispose() {
		cursor = -1
		host.undo().remove(this::undoHandler)
		host.redo().remove(this::redoHandler)
		dataBinding.changed.remove(::dataBindingChangedHandler.as2)
		delayedPush.dispose()
	}
}

fun <T> UiComponentRo.applyDataBindingUndoRedo(dataBinding: DataBinding<T>): DataBindingHistory<T> {
	return DataBindingHistory(this, dataBinding)
}