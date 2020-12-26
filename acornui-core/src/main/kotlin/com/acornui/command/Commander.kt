/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.command

import com.acornui.collection.poll
import com.acornui.collection.setSize
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.frame
import com.acornui.signal.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.seconds

interface Commander {

	/**
	 * A command has been invoked.
	 */
	val commandInvoked: Signal<CommandNotification>

	/**
	 * A command has completed.
	 */
	val commandCompleted: Signal<CommandNotification>

	/**
	 * The command history stack, in order of oldest to newest.
	 * This will be at most [maxCommandHistory] items, with the oldest items removed first.
	 */
	val history: List<CommandNotification>

	/**
	 * The current index within the history towards which this commander is progressing.
	 */
	val index: Int

	/**
	 * The maximum number of commands to remember. (Allows for undo/redo)
	 */
	var maxCommandHistory: Int

	/**
	 * Enqueues a command and appends the command to the [history] stack.
	 */
	fun add(command: Command, group: CommandGroup? = null)

	fun undoCommandGroup(): List<CommandNotification>

	fun redoCommandGroup(): List<CommandNotification>

	/**
	 * If the history item at the current cursor can be undone, queues the command created with [Command.createUndo].
	 */
	fun undoCommand(): CommandNotification?

	fun redoCommand(): CommandNotification?

	/**
	 * A list of commands still pending.
	 */
	val pending: List<CommandNotification>

	/**
	 * True if this commander is currently invoking a command.
	 */
	val isBusy: Boolean

	companion object : Context.Key<Commander> {

		override val factory = dependencyFactory { CommanderImpl(it) }
	}
}

/**
 * Suspends the coroutine until all pending commands have been completed.
 */
suspend fun Commander.await() {
	if (!isBusy) return
	return suspendCancellableCoroutine { cont ->
		var sub: SignalSubscription? = null
		sub = commandCompleted.listen {
			if (pending.isEmpty()) {
				sub!!.dispose()
				cont.resume(Unit)
			}
		}
		cont.invokeOnCancellation {
			sub.dispose()
		}

	}
}

/**
 * A command group indicates which commands should be grouped together in undo/redo actions.
 */
class CommandGroup

/**
 * A description of a command invocation. This is information the [Commander] provides publicly for history and events.
 */
interface CommandNotification {

	/**
	 * The type of command.
	 */
	val type: KClass<out Command>

	val group: CommandGroup?

	fun isInstanceOf(type: KClass<out Command>): Boolean

}

class CommanderImpl(owner: Context) : ContextImpl(owner), Commander {

	override var index: Int = 0
		private set

	/**
	 * The maximum number of commands to remember. (Allows for undo/redo)
	 */
	override var maxCommandHistory = 10000

	override val commandInvoked = signal<CommandNotification>()

	override val commandCompleted = signal<CommandCompletedEvent>()

	class CommandCompletedEvent(notification: CommandNotification, val error: Throwable?) :
		CommandNotification by notification

	private val historyMutable = ArrayList<CommandAndGroup>()
	override val history: List<CommandNotification> = historyMutable

	private val pendingMutable = ArrayList<CommandAndGroup>()
	override val pending: List<CommandNotification> = pendingMutable

	/**
	 * Returns a signal for [commandCompleted] filtered to only commands of the given type.
	 * This is the same as `commandCompleted.filtered { it.type == type }`
	 */
	fun commandCompleted(type: KClass<out Command>): Signal<CommandCompletedEvent> =
		commandCompleted.filtered { it.isInstanceOf(type) }

	override fun add(command: Command, group: CommandGroup?) {
		// If we have undone commands and are now invoking a new command,
		// then remove the undone commands from the history.
		historyMutable.setSize(index)

		val c = CommandAndGroup(command, group)
		historyMutable.add(c)

		// Remove oldest history elements until our history size is at most maxCommandHistory.
		while (historyMutable.size > maxCommandHistory)
			historyMutable.poll()

		index = historyMutable.size
		invoke(c)
	}

	override fun undoCommandGroup(): List<CommandNotification> {
		val currentGroup = previousCommand()?.group
		val toInvoke = ArrayList<CommandAndGroup>()
		while (true) {
			val prev = previousCommand()
			if (prev?.group != currentGroup) break
			val undoCommand = prev?.createUndo() ?: break
			toInvoke.add(CommandAndGroup(undoCommand, prev.group))
			index--
			if (currentGroup == null) break
		}
		invoke(toInvoke)
		return toInvoke
	}

	override fun redoCommandGroup(): List<CommandNotification> {
		val currentGroup = nextCommand()?.group
		val toInvoke = ArrayList<CommandAndGroup>()
		while (true) {
			val next = nextCommand() ?: break
			if (next.group != currentGroup) break
			toInvoke.add(next)
			index++
			if (currentGroup == null) break
		}
		invoke(toInvoke)
		return toInvoke
	}

	override fun undoCommand(): CommandNotification? {
		val prev = previousCommand() ?: return null
		val undoCommand = prev.createUndo() ?: return null
		index--
		val c = CommandAndGroup(undoCommand, prev.group)
		invoke(c)
		return c
	}

	override fun redoCommand(): CommandNotification? {
		val next = nextCommand() ?: return null
		index++
		invoke(next)
		return next
	}

	private fun previousCommand(): CommandAndGroup? = historyMutable.getOrNull(index - 1)
	private fun nextCommand(): CommandAndGroup? = historyMutable.getOrNull(index)

	private fun invoke(command: CommandAndGroup) {
		pendingMutable.add(command)
		nextPending()
	}

	private fun invoke(commands: List<CommandAndGroup>) {
		pendingMutable.addAll(commands)
		nextPending()
	}

	override var isBusy = false
		private set

	/**
	 * Polls the pending queue when this dispatcher is idle.
	 */
	private fun nextPending() {
		if (isBusy || pendingMutable.isEmpty()) return
		val next = pendingMutable.poll()
		isBusy = true
		launch {
			commandInvoked.dispatch(next)
			var error: Throwable? = null
			try {
				withTimeout(next.timeout) {
					next.invoke()
				}
			} catch (e: Throwable) {
				error = e
			}
			frame.once {
				// Notify command's completion on the next frame to prevent deadlock.
				commandCompleted.dispatch(CommandCompletedEvent(next, error))
				isBusy = false
				nextPending()
			}
		}
	}

	private class CommandAndGroup(val command: Command, override val group: CommandGroup?) : Command by command,
		CommandNotification {

		override val type: KClass<out Command>
			get() = command::class

		override fun isInstanceOf(type: KClass<out Command>): Boolean =
			type.isInstance(command)
	}
}

interface Command {

	/**
	 * The timeout before this command is considered to have failed.
	 */
	val timeout: Duration
		get() = 30.seconds

	/**
	 * Executes this command.
	 * This should only be invoked from the command dispatcher.
	 */
	suspend fun invoke()

	/**
	 * If possible, returns the command that can undo this command.
	 * This method should be idempotent. That is, calling multiple times should yield the same result.
	 */
	fun createUndo(): Command? = null
}

val Context.commander: Commander
	get() = inject(Commander)

/**
 * Adds the given command to the injected [Commander]'s queue.
 */
fun Context.command(command: Command) {
	commander.add(command)
}

/**
 * Creates and adds an anonymous command out of the given invocation block.
 */
fun Context.command(invoke: suspend () -> Unit) {
	commander.add(createCommand(invoke))
}

/**
 * Creates and adds an anonymous command out of the given invocation and undo blocks.
 */
fun Context.command(invoke: suspend () -> Unit, undo: suspend () -> Unit) {
	commander.add(createCommand(invoke, undo))
}

/**
 * Creates an anonymous command out of the given invocation block.
 */
fun createCommand(invoke: suspend () -> Unit): Command = createCommand(invoke, null)

/**
 * Creates an anonymous command out of the given invocation and undo blocks.
 */
fun createCommand(invoke: suspend () -> Unit, undo: (suspend () -> Unit)?): Command {
	return object : Command {
		override suspend fun invoke() = invoke()
		override fun createUndo(): Command? {
			if (undo == null) return null
			return createCommand(undo, invoke)
		}
	}
}