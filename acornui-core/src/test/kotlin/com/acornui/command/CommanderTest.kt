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

import com.acornui.di.ContextImpl
import com.acornui.obj.Boxed
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CommanderTest {

	lateinit var commander: CommanderImpl

	@BeforeTest
	private fun before() {
		commander = CommanderImpl(ContextImpl())
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
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))

		assertEquals(4, d.value)
		commander.undoCommand()
		commander.undoCommand()

		assertEquals(2, d.value)
		commander.redoCommand()
		commander.redoCommand()

		assertEquals(4, d.value)
	}

	@Test
	fun addAfterUndoShouldClearHistoryAfterCursor() {
		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))

		commander.undoCommand()
		commander.undoCommand()
		assertEquals(2, d.value)
		commander.add(Increment(d, 1))
		assertEquals(3, d.value)
		commander.redoCommand()
		commander.redoCommand()
		assertEquals(3, d.value)
	}

	@Test
	fun undoRedoWithGroups() {
		val d = Boxed(0)
		val groupA = CommandGroup()
		commander.add(Increment(d, 1), groupA)
		commander.add(Increment(d, 1), groupA)
		val groupB = CommandGroup()
		commander.add(Increment(d, 1), groupB)
		commander.add(Increment(d, 1), groupB)
		commander.add(Increment(d, 1), groupB)
		commander.add(Increment(d, 1)) // No group
		val groupC = CommandGroup()
		commander.add(Increment(d, 1), groupC)
		commander.add(Increment(d, 1), groupC)

		assertEquals(8, d.value)
		commander.undoCommandGroup()

		assertEquals(6, d.value)
		commander.undoCommandGroup()

		assertEquals(5, d.value)
		commander.undoCommandGroup()

		assertEquals(2, d.value)
		commander.undoCommandGroup()

		assertEquals(0, d.value)
		commander.undoCommandGroup()

		assertEquals(0, d.value)
		commander.redoCommandGroup()

		assertEquals(2, d.value)
		commander.redoCommandGroup()

		assertEquals(5, d.value)
		commander.redoCommandGroup()

		assertEquals(6, d.value)
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
		commander.undoCommand()

		assertEquals(0, i)
		commander.redoCommand()

		assertEquals(1, i)
		commander.redoCommand()

		assertEquals(1, i)
	}

	@Test
	fun maxHistory() {
		commander.maxCommandHistory = 0

		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.undoCommand() // Nothing to undo, no history
		commander.undoCommand()
		assertEquals(2, d.value)

		commander.maxCommandHistory = 2
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		assertEquals(5, d.value)
		commander.undoCommand() // 4
		commander.undoCommand() // 3
		commander.undoCommand() // 3 - Only two should be undone; history size exceeded.
		assertEquals(3, d.value)

		commander.maxCommandHistory = 0
	}

	@Test
	fun onCommand() {
		val ctx = ContextImpl()
		ctx.dependencies += Commander to commander

		class Foo : Command {
			override fun invoke() {}
		}
		class Bar : Command {
			override fun invoke() {}
		}
		var fooC = 0
		ctx.onCommand<Foo> {
			fooC++
		}
		var barC = 0
		ctx.onCommand<Bar> {
			barC++
		}
		ctx.command(Foo())
		ctx.command(Foo())
		ctx.command(Bar())
		ctx.command(Foo())
		ctx.command(Bar())
		assertEquals(3, fooC)
		assertEquals(2, barC)
	}
}

private class Increment(val receiver: Boxed<Int>, val delta: Int) : Command {

	override fun invoke() {
		receiver.value = receiver.value + delta
	}

	override fun createUndo() = Increment(receiver, -delta)
}