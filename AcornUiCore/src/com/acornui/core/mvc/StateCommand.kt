/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.core.mvc

import com.acornui.collection.poll
import com.acornui.collection.pop
import com.acornui.component.UiComponentRo
import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.input.interaction.redo
import com.acornui.core.input.interaction.undo
import com.acornui.logging.Log

open class StateCommandHistory(
		private val commandDispatcher: CommandDispatcher
) : Disposable {

	protected var maxCommandHistory = 100000
	protected val _commandHistory: ArrayList<StateCommand> = ArrayList()
	val commandHistory: List<StateCommand>
		get() = _commandHistory

	protected var commandCursor: Int = 0
	protected var isDispatching = false

	private val commandInvokedHandler: (Command) -> Unit = {
		if (!isDispatching) {
			if (it is StateCommand) {
				add(it)
			}
		}
	}

	init {
		commandDispatcher.commandInvoked.add(commandInvokedHandler)
	}

	fun lastCommand(): StateCommand? {
		if (commandCursor == 0) return null
		return _commandHistory[commandCursor - 1]
	}

	fun nextCommand(): StateCommand? {
		if (commandCursor > _commandHistory.lastIndex) return null
		return _commandHistory[commandCursor]
	}

	protected fun add(command: StateCommand) {
		if (commandCursor != _commandHistory.size) {
			// If we have undone commands and are now invoking a new command,
			// then remove the undone commands from the history.
			while (_commandHistory.size > commandCursor) {
				_commandHistory.pop()
			}
		}
		_commandHistory.add(command)
		if (_commandHistory.size > maxCommandHistory) {
			_commandHistory.poll()
		} else {
			commandCursor++
		}
	}

	fun undoCommandGroup() {
		val currentGroup = lastCommand()?.group
		do {
			undoCommand()
			val nextGroup = lastCommand()?.group
		} while (nextGroup != null && nextGroup == currentGroup)
	}

	fun redoCommandGroup() {
		val currentGroup = nextCommand()?.group
		do {
			redoCommand()
			val nextGroup = nextCommand()?.group
		} while (nextGroup != null && nextGroup == currentGroup)
	}

	fun undoCommand(): Command? {
		if (isDispatching) {
			Log.warn("Cannot invoke a command within a command handler.")
			return null
		}
		if (commandCursor == 0) return null
		val command = _commandHistory[commandCursor - 1]
		val reversed = command.reverse()
		commandCursor--
		_invoke(reversed)
		return reversed
	}

	fun redoCommand(): StateCommand? {
		if (isDispatching) {
			Log.warn("Cannot invoke a command within a command handler.")
			return null
		}
		if (commandCursor == _commandHistory.size) return null
		val command = _commandHistory[commandCursor]
		commandCursor++
		_invoke(command)
		return command
	}

	private fun _invoke(command: Command) {
		isDispatching = true
		commandDispatcher.invokeCommand(command)
		isDispatching = false
	}

	fun clearHistory() {
		_commandHistory.clear()
		commandCursor = 0
	}

	override fun dispose() {
		commandDispatcher.commandInvoked.remove(commandInvokedHandler)
	}

	companion object : DKey<StateCommandHistory> {
		override fun factory(injector: Injector): StateCommandHistory? {
			return StateCommandHistory(injector.inject(CommandDispatcher))
		}
	}
}

fun Scoped.stateCommandHistory(): StateCommandHistory = inject(StateCommandHistory)


/**
 * A Command that changes application state and can be undone.
 */
interface StateCommand : Command {

	override val type: CommandType<out StateCommand>

	/**
	 * If set, undo/redo actions will continue until the group no longer matches.
	 */
	val group: CommandGroup?

	/**
	 * Creates the command that represents the undoing of this command.
	 */
	fun reverse(): Command
}

class CommandGroup

fun UiComponentRo.enableUndoRedo() {
	undo().add {
		stateCommandHistory().undoCommandGroup()
	}
	redo().add {
		stateCommandHistory().redoCommandGroup()
	}
}