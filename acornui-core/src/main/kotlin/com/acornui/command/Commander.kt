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
import com.acornui.collection.keepFirst
import com.acornui.collection.keepLast
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.own
import com.acornui.signal.Signal
import com.acornui.signal.SignalSubscription
import com.acornui.signal.filtered
import com.acornui.signal.signal
import kotlin.reflect.KClass

interface Commander {

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
	 * If this is set to lower than the current history, the history will not be truncated until after the next [add].
	 */
	var maxCommandHistory: Int

	/**
	 * Enqueues a command and appends the command to the [history] stack.
	 */
	fun add(command: Command, group: CommandGroup? = null)

	/**
	 * Undoes last commands with the same [CommandGroup]. (Or one command with no command group)
	 */
	fun undoCommandGroup(): List<CommandNotification>

	/**
	 * Redoes the last undone commands with the same [CommandGroup]. (Or one command with no command group)
	 * Note that if a new command has been added after undo, redo history is removed.
	 */
	fun redoCommandGroup(): List<CommandNotification>

	/**
	 * If the history item at the current cursor can be undone, queues the command created with [Command.createUndo].
	 */
	fun undoCommand(): CommandNotification?

	/**
	 * Redoes the last undone command.
	 * Note that if a new command has been added after undo, redo history is removed.
	 */
	fun redoCommand(): CommandNotification?

	companion object : Context.Key<Commander> {

		override val factory = dependencyFactory { CommanderImpl(it) }
	}
}

/**
 * Listens for a command to be completed of the given command type.
 * @return Returns a subscription that may be paused or disposed. Will be disposed automatically when this context is
 * disposed.
 */
inline fun <reified T : Command> Context.onCommand(noinline handler: (CommandNotification) -> Unit): SignalSubscription =
	own(commander.commandCompleted.filtered { it.isInstanceOf(T::class) }.listen(handler))

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

	override var maxCommandHistory = 10000

	override val commandCompleted = signal<CommandNotification>()

	private val historyMutable = ArrayList<CommandAndGroup>()
	override val history: List<CommandNotification> = historyMutable

	private val pending = ArrayList<CommandAndGroup>()

	override fun add(command: Command, group: CommandGroup?) {
		// If we have undone commands and are now invoking a new command,
		// then remove the undone commands from the history.
		historyMutable.keepFirst(index)

		val c = CommandAndGroup(command, group)
		historyMutable.add(c)

		// Remove oldest history elements until our history size is at most maxCommandHistory.
		historyMutable.keepLast(maxCommandHistory)

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
		pending.add(command)
		nextPending()
	}

	private fun invoke(commands: List<CommandAndGroup>) {
		pending.addAll(commands)
		nextPending()
	}

	private var isBusy = false

	/**
	 * Polls the pending queue when this dispatcher is idle.
	 */
	private fun nextPending() {
		if (isBusy || pending.isEmpty()) return
		val next = pending.poll()
		isBusy = true
		next.invoke()
		commandCompleted.dispatch(next)
		isBusy = false
		nextPending()
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
	 * Executes this command.
	 * This should only be invoked from the commander.
	 */
	fun invoke()

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
fun Context.command(command: Command, group: CommandGroup? = null) {
	commander.add(command, group)
}

/**
 * Creates and adds an anonymous command out of the given invocation block.
 */
fun Context.command(invoke: () -> Unit, group: CommandGroup? = null) {
	commander.add(createCommand(invoke), group)
}

/**
 * Creates and adds an anonymous command out of the given invocation and undo blocks.
 */
fun Context.command(invoke: () -> Unit, undo: () -> Unit, group: CommandGroup? = null) {
	commander.add(createCommand(invoke, undo), group)
}

/**
 * Creates an anonymous command out of the given invocation block.
 */
fun createCommand(invoke: () -> Unit): Command = createCommand(invoke, null)

/**
 * Creates an anonymous command out of the given invocation and undo blocks.
 */
fun createCommand(invoke: () -> Unit, undo: (() -> Unit)?): Command {
	return object : Command {
		override fun invoke() = invoke()
		override fun createUndo(): Command? {
			if (undo == null) return null
			return createCommand(undo, invoke)
		}
	}
}