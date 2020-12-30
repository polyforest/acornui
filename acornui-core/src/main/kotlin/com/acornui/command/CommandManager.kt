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

import com.acornui.ExperimentalAcorn
import com.acornui.collection.*
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.observe.Observable
import com.acornui.recycle.Clearable
import com.acornui.signal.signal

@ExperimentalAcorn
interface CommandManager : Observable, Clearable {

	/**
	 * A LIFO list of commands added via [add].
	 * When a command is undone (via [undo] or [undoCommandGroup]), the command is popped from this stack
	 * and pushed on the [redoStack].
	 */
	val undoStack: List<CommandCall>

	/**
	 * A LIFO list of commands undone via [undo] or [undoCommandGroup].
	 * When a command is redone (via [redo] or [redoCommandGroup]), the command is popped from this stack
	 * and pushed back on the [undoStack].
	 */
	val redoStack: List<CommandCall>

	/**
	 * The maximum number of commands to remember. (Allows for undo/redo)
	 * If this is set to lower than the current history, the history will not be truncated until after the next [add].
	 */
	var maxUndoHistory: Int

	/**
	 * Enqueues a command and appends the command to the [undoStack].
	 */
	fun add(command: Command, group: CommandGroup? = null)

	/**
	 * Pops the last item off the [undoStack], and adds it to the [redoStack].
	 */
	fun undo(): CommandCall?

	/**
	 * Pops the last item off the [redoStack], and adds it to the [undoStack].
	 */
	fun redo(): CommandCall?

	/**
	 * Undoes the last commands with the same [CommandGroup].
	 * If the last [CommandCall.group] is null, undoes one command.
	 * @return Returns the list of commands that were undone, in the order that [Command.undo] was called.
	 */
	fun undoCommandGroup(): List<CommandCall>

	/**
	 * Redoes the last undone commands with the same [CommandGroup].
	 * If the last [CommandCall.group] is null, redoes one command.
	 * @return Returns the list of commands that were redone, in the order that [Command.execute] was called.
	 */
	fun redoCommandGroup(): List<CommandCall>

	companion object : Context.Key<CommandManager> {

		override val factory = dependencyFactory { CommandManagerImpl(it) }
	}
}

@ExperimentalAcorn
class CommandCall(val command: Command, val group: CommandGroup?)

@ExperimentalAcorn
class CommandManagerImpl(owner: Context) : ContextImpl(owner), CommandManager {

	override val changed = signal<Observable>()

	private val undoStackMutable = ArrayList<CommandCall>()
	override val undoStack: List<CommandCall> = undoStackMutable

	private val redoStackMutable = ArrayList<CommandCall>()
	override val redoStack: List<CommandCall> = redoStackMutable

	override var maxUndoHistory: Int = 10000

	override fun add(command: Command, group: CommandGroup?) {
		redoStackMutable.clear()
		undoStackMutable.add(CommandCall(command, group))

		// Remove oldest history elements until our history size is at most maxCommandHistory.
		undoStackMutable.keepLast(maxUndoHistory)
		command.execute()
		changed.dispatch(this)
	}

	override fun undo(): CommandCall? {
		val c = undoStackMutable.popOrNull() ?: return null
		redoStackMutable.add(c)
		c.command.undo()
		changed.dispatch(this)
		return c
	}

	override fun redo(): CommandCall? {
		val c = redoStackMutable.popOrNull() ?: return null
		undoStackMutable.add(c)
		c.command.execute()
		changed.dispatch(this)
		return c
	}

	override fun undoCommandGroup(): List<CommandCall> {
		if (undoStackMutable.isEmpty()) return emptyList()
		val group = undoStackMutable.last().group
		val toInvoke = ArrayList<CommandCall>()
		while (true) {
			val prev = undoStackMutable.lastOrNull() ?: break
			if (prev.group != group) break
			undoStackMutable.pop()
			redoStackMutable.add(prev)
			toInvoke.add(prev)
			if (group == null) break
		}
		toInvoke.forEach { it.command.undo() }
		changed.dispatch(this)
		return toInvoke
	}

	override fun redoCommandGroup(): List<CommandCall> {
		if (redoStackMutable.isEmpty()) return emptyList()
		val currentGroup = redoStackMutable.last().group
		val toInvoke = ArrayList<CommandCall>()
		while (true) {
			val prev = redoStackMutable.lastOrNull() ?: break
			if (prev.group != currentGroup) break
			redoStackMutable.pop()
			undoStackMutable.add(prev)
			toInvoke.add(prev)
			if (currentGroup == null) break
		}
		toInvoke.forEach { it.command.execute() }
		changed.dispatch(this)
		return toInvoke
	}

	override fun clear() {
		undoStackMutable.clear()
		redoStackMutable.clear()
		changed.dispatch(this)
	}

}

@ExperimentalAcorn
val Context.commandManager: CommandManager
	get() = inject(CommandManager)

/**
 * Adds the given command to the injected [CommandManager]'s queue.
 */
@ExperimentalAcorn
fun Context.command(command: Command) {
	commandManager.add(command)
}

/**
 * Creates and adds an anonymous command out of the given invocation and undo blocks.
 */
@ExperimentalAcorn
fun Context.command(invoke: () -> Unit, undo: () -> Unit) {
	commandManager.add(createCommand(invoke, undo))
}