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

package com.acornui.mvc

import com.acornui.signal.unmanagedSignal


fun invokeCommand(command: Command) {
	CommandDispatcher.invokeCommand(command)
}

object CommandDispatcher {

	var keepHistory: Boolean = false

	val commandInvoked = unmanagedSignal<Command>()

	private val _history = ArrayList<Command>()
	val history: List<Command>
		get() = _history

	fun invokeCommand(command: Command) {
		if (keepHistory) _history.add(command)
		commandInvoked.dispatch(command)
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
