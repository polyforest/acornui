package com.acornui.core.mvc

import com.acornui.signal.Signal1


fun invokeCommand(command: Command) {
	CommandDispatcher.invokeCommand(command)
}

object CommandDispatcher {

	var keepHistory: Boolean = false

	private val _commandInvoked: Signal1<Command> = Signal1()
	val commandInvoked = _commandInvoked.asRo()

	private val _history = ArrayList<Command>()
	val history: List<Command>
		get() = _history

	fun invokeCommand(command: Command) {
		if (keepHistory) _history.add(command)
		_commandInvoked.dispatch(command)
	}

}

@Suppress("unused")
interface CommandType<T : Command>

interface Command {

	/**
	 * The identifier that allows handlers to observe the desired command. Typically the command's companion object.
	 */
	val type: CommandType<out Command>

}