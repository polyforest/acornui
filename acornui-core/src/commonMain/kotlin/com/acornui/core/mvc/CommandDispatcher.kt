/*
 * Copyright 2019 Poly Forest, LLC
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
	override val commandInvoked = _commandInvoked.asRo()

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
