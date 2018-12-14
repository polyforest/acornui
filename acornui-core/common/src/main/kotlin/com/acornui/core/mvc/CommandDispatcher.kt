package com.acornui.core.mvc

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.signal.Signal
import com.acornui.signal.Signal1


/**
 * The command dispatcher is simply a scoped signal for dispatching and listening to Commands.
 *
 * @see Commander Use a Commander object to more easily listen for and dispatch commands without memory leaks.
 */
interface CommandDispatcher {

	/**
	 * Dispatched when a command has been invoked, or redone.
	 */
	val commandInvoked: Signal<(Command) -> Unit>

	/**
	 * Invokes the given command.
	 */
	fun invokeCommand(command: Command)

	val history: List<Command>

	val keepHistory: Boolean

	companion object : DKey<CommandDispatcher> {
		override fun factory(injector: Injector): CommandDispatcher? = CommandDispatcherImpl()
	}
}

fun Scoped.invokeCommand(command: Command) {
	inject(CommandDispatcher).invokeCommand(command)
}

open class CommandDispatcherImpl(
		override val keepHistory: Boolean = false
) : CommandDispatcher, Disposable {

	private val _commandInvoked: Signal1<Command> = Signal1()
	override val commandInvoked: Signal<(Command) -> Unit>
		get() = _commandInvoked

	private val _history = ArrayList<Command>()
	override val history: List<Command>
		get() = _history

	override fun invokeCommand(command: Command) {
		if (keepHistory) _history.add(command)
		_commandInvoked.dispatch(command)
	}

	override fun dispose() {
		_commandInvoked.dispose()
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