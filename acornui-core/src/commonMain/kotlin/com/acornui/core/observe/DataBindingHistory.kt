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
import com.acornui.signal.Signal0

class DataBindingHistory<T>(
		private val host: UiComponentRo,
		private val dataBinding: DataBinding<T>
) : Clearable, Disposable {

	private val _changed = Signal0()

	/**
	 * Dispatched when either the [cursor] or the [history] has changed.
	 * Unlike component changed signals, this will be dispatched on programmatic changes as well,
	 * not just user interactions.
	 */
	val changed = _changed.asRo()

	var maxHistory = 200

	private val _history = ArrayList<T>()

	/**
	 * The state history for the target databinding.
	 */
	val history: List<T> = _history

	/**
	 * The current history index.
	 */
	var cursor: Int = -1
		private set

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

	val hasRedo: Boolean
		get() = cursor < _history.lastIndex

	/**
	 * If [hasRedo] is true, the cursor will be decremented and the [dataBinding] state will be
	 * set to the next historic value.
	 */
	fun redo() {
		if (isDispatching)
			throw IllegalStateException("Cannot redo while dispatching.")
		if (cursor >= _history.lastIndex) return
		isDispatching = true
		val nextState = _history[++cursor]
		dataBinding.value = nextState
		isDispatching = false
		_changed.dispatch()
	}

	val hasUndo: Boolean
		get() = cursor > 0

	/**
	 * If [hasUndo] is true, the cursor will be decremented and the [dataBinding] state will be
	 * set to the previous historic value.
	 */
	fun undo() {
		if (isDispatching)
			throw IllegalStateException("Cannot undo while dispatching.")
		if (cursor == 0) return
		isDispatching = true
		val previousState = _history[--cursor]
		dataBinding.value = previousState
		isDispatching = false
		_changed.dispatch()
	}

	private fun pushState() {
		if (cursor != history.lastIndex) {
			// If we have undone states and are now pushing a new state, then remove the undone states from the history.
			while (_history.lastIndex > cursor)
				_history.pop()
		}
		_history.add(dataBinding.value)
		if (_history.size > maxHistory) _history.poll() else cursor++
		_changed.dispatch()
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
		_changed.dispose()
	}
}

fun <T> UiComponentRo.applyDataBindingUndoRedo(dataBinding: DataBinding<T>): DataBindingHistory<T> {
	return DataBindingHistory(this, dataBinding)
}