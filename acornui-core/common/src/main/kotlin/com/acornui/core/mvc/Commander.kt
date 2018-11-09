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