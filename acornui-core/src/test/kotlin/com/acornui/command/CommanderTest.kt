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
import com.acornui.di.ContextImpl
import com.acornui.obj.Boxed
import kotlin.test.*

@ExperimentalAcorn
class CommanderTest {

	lateinit var commander: CommandManagerImpl

	@BeforeTest
	private fun before() {
		commander = CommandManagerImpl(ContextImpl())
	}

	@AfterTest
	private fun after() {
		commander.dispose()
	}

	@Test
	fun addInvokesCommand() {
		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))

		assertEquals(4, d.value)
	}

	@Test
	fun undoRedo() {
		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 3))
		commander.add(Increment(d, 5))
		commander.add(Increment(d, 7))

		assertEquals(1 + 3 + 5 + 7, d.value)
		commander.undo()
		commander.undo()

		assertEquals(1 + 3, d.value)
		commander.redo()

		assertEquals(1 + 3 + 5, d.value)
		commander.redo()
		assertEquals(1 + 3 + 5 + 7, d.value)
		commander.redo()
		assertEquals(1 + 3 + 5 + 7, d.value)
	}

	@Test
	fun addAfterUndoShouldClearRedo() {
		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 3))
		commander.add(Increment(d, 5))
		commander.add(Increment(d, 7))

		commander.undo()
		commander.undo()
		assertEquals(1 + 3, d.value)
		commander.add(Increment(d, 8))
		assertEquals(1 + 3 + 8, d.value)
		commander.redo()
		commander.redo()
		assertEquals(1 + 3 + 8, d.value)
	}

	@Test
	fun undoRedoWithGroups() {
		val d = Boxed(0)
		val groupA = CommandGroup()
		commander.add(Increment(d, 1), groupA)
		commander.add(Increment(d, 3), groupA)
		val groupB = CommandGroup()
		commander.add(Increment(d, 5), groupB)
		commander.add(Increment(d, 7), groupB)
		commander.add(Increment(d, 9), groupB)
		commander.add(Increment(d, 11)) // No group
		val groupC = CommandGroup()
		commander.add(Increment(d, 13), groupC)
		commander.add(Increment(d, 15), groupC)

		assertEquals(1 + 3 + 5 + 7 + 9 + 11 + 13 + 15, d.value)

		commander.undoCommandGroup()
		assertEquals(1 + 3 + 5 + 7 + 9 + 11, d.value)

		commander.undoCommandGroup()
		assertEquals(1 + 3 + 5 + 7 + 9, d.value)

		commander.undoCommandGroup()
		assertEquals(1 + 3, d.value)

		commander.undoCommandGroup()
		assertEquals(0, d.value)

		commander.undoCommandGroup()
		assertEquals(0, d.value)

		commander.redoCommandGroup()
		assertEquals(1 + 3, d.value)

		commander.redoCommandGroup()
		assertEquals(1 + 3 + 5 + 7 + 9, d.value)

		commander.redoCommandGroup()
		assertEquals(1 + 3 + 5 + 7 + 9 + 11, d.value)

		commander.redoCommandGroup()
		assertEquals(1 + 3 + 5 + 7 + 9 + 11 + 13 + 15, d.value)

		commander.redoCommandGroup()
		assertEquals(1 + 3 + 5 + 7 + 9 + 11 + 13 + 15, d.value)
	}

	@Test
	fun anonymousCommand() {
		var i = 0
		commander.add(createCommand(invoke = {
			i++
		}, undo = {
			i--
		}))

		assertEquals(1, i)
		commander.undo()

		assertEquals(0, i)
		commander.redo()

		assertEquals(1, i)
		commander.redo()

		assertEquals(1, i)
	}

	@Test
	fun maxHistory() {
		commander.maxUndoHistory = 0

		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.undo() // Nothing to undo, no history
		commander.undo()
		assertEquals(2, d.value)

		commander.maxUndoHistory = 2
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		assertEquals(5, d.value)
		commander.undo() // 4
		commander.undo() // 3
		commander.undo() // 3 - Only two should be undone; history size exceeded.
		assertEquals(3, d.value)

		commander.maxUndoHistory = 0
	}

	@Test
	fun clear() {
		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.undo()
		commander.undo()
		commander.clear()
		assertTrue(commander.undoStack.isEmpty())
		assertTrue(commander.redoStack.isEmpty())
	}
}

@ExperimentalAcorn
private class Increment(val receiver: Boxed<Int>, val delta: Int) : Command {

	override fun execute() {
		receiver.value += delta
	}

	override fun undo() {
		receiver.value -= delta
	}
}