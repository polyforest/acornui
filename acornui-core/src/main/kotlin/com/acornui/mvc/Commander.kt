package com.acornui.mvc

import com.acornui.*

/**
 * A commander provides utility to listening to the command dispatcher, and allows for all callbacks to be
 * unregistered via the commander's dispose method.
 */
class Commander(owner: Owner) : DisposableBase(owner), ManagedDisposable {

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
	fun onCommandInvoked(callback: (command: Command) -> Unit): Disposable =
		own(CommandDispatcher.commandInvoked.listen(callback))

}

fun Owner.commander(): Commander = Commander(this)