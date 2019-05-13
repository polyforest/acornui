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
import com.acornui.core.di.Scoped


/**
 * A commander provides utility to listening to the command dispatcher, and allows for all callbacks to be
 * unregistered via the commander's dispose method.
 */
class Commander(
		private val commandDispatcher: CommandDispatcher
) : Disposable {

	private val disposables = ArrayList<Disposable>()

	/**
	 * Invokes the given callback only when the provided type matches the invoked command's type.
	 * @return An object which, when disposed, will remove the handler.
	 */
	fun <T : Command> onCommandInvoked(type: CommandType<T>, callback: (command: T) -> Unit): Disposable {
		return onCommandInvoked {
			@Suppress("UNCHECKED_CAST")
			if (it.type == type)
				callback(it as T)
		}
	}

	/**
	 * Invokes the given callback when a command has been invoked.
	 * @return An object which, when disposed, will remove the handler.
	 */
	fun onCommandInvoked(callback: (command: Command) -> Unit): Disposable {
		commandDispatcher.commandInvoked.add(callback)

		val disposable = object : Disposable {
			override fun dispose() {
				commandDispatcher.commandInvoked.remove(callback)
			}
		}
		disposables.add(disposable)
		return disposable
	}

	override fun dispose() {
		for (disposable in disposables) {
			disposable.dispose()
		}
		disposables.clear()
	}
}

fun Scoped.commander(): Commander = Commander(injector.inject(CommandDispatcher))
