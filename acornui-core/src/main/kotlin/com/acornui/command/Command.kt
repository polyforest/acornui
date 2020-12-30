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

@ExperimentalAcorn
interface Command {

	/**
	 * Executes this command.
	 * This should only be invoked from the command manager.
	 */
	fun execute()

	/**
	 * Undoes this command.
	 * This should only be invoked from the command manager.
	 */
	fun undo()

}

@ExperimentalAcorn
class CommandGroup

@ExperimentalAcorn
object NoopCommand : Command {
	override fun execute() {}
	override fun undo() {}
}

/**
 * Creates an anonymous command out of the given invocation and undo blocks.
 */
@ExperimentalAcorn
fun createCommand(invoke: () -> Unit, undo: () -> Unit): Command = object : Command {
	override fun execute() = invoke()
	override fun undo() = undo()
}