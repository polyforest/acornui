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

import com.acornui.async.delay
import com.acornui.async.withTimeout
import com.acornui.di.ContextImpl
import com.acornui.obj.Boxed
import com.acornui.test.assertLessThan
import com.acornui.test.runTest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime
import kotlin.time.seconds

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
	fun addInvokesCommand() = runTest {
		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.await()
		assertEquals(4, d.value)
	}

	@Test
	fun undoRedo() = runTest(timeout = 2.seconds) {
		val d = Boxed(0)
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.add(Increment(d, 1))
		commander.await()
		assertEquals(4, d.value)
		commander.undoCommand()
		commander.undoCommand()
		commander.await()
		assertEquals(2, d.value)
		commander.redoCommand()
		commander.redoCommand()
		commander.await()
		assertEquals(4, d.value)
	}

	@Test
	fun undoRedoWithGroups() = runTest(timeout = 2.seconds) {
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
		commander.await()
		assertEquals(8, d.value)
		commander.undoCommandGroup()
		commander.await()
		assertEquals(6, d.value)
		commander.undoCommandGroup()
		commander.await()
		assertEquals(5, d.value)
		commander.undoCommandGroup()
		commander.await()
		assertEquals(2, d.value)
		commander.undoCommandGroup()
		commander.await()
		assertEquals(0, d.value)
		commander.undoCommandGroup()
		commander.await()
		assertEquals(0, d.value)
		commander.redoCommandGroup()
		commander.await()
		assertEquals(2, d.value)
		commander.redoCommandGroup()
		commander.await()
		assertEquals(5, d.value)
		commander.redoCommandGroup()
		commander.await()
		assertEquals(6, d.value)
	}

	@Test
	fun awaitCanBeCancelled() = runTest {
		commander.add(createCommand {
			while (isActive) {
				delay(0.2.seconds)
			}
		})
		val time = measureTime {
			try {
				withTimeout(0.5.seconds) {
					commander.await()
				}
			} catch (ignore: TimeoutCancellationException) {}
		}
		assertLessThan(1.seconds, time)
	}

	@Test
	fun anonymousCommand() = runTest {
		var i = 0
		commander.add(createCommand(invoke = {
			i++
		}, undo = {
			i--
		}))
		commander.await()
		assertEquals(1, i)
		commander.undoCommand()
		commander.await()
		assertEquals(0, i)
		commander.redoCommand()
		commander.await()
		assertEquals(1, i)
		commander.redoCommand()
		commander.await()
		assertEquals(1, i)
	}
}

private class Increment(val receiver: Boxed<Int>, val delta: Int) : Command {

	override suspend fun invoke() {
		receiver.value = receiver.value + delta
	}

	override fun createUndo() = Increment(receiver, -delta)
}