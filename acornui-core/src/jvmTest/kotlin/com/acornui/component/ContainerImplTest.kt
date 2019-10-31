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

package com.acornui.component

import com.acornui.collection.addAll
import com.acornui.component.layout.spacer
import com.acornui.headless.HeadlessInjector
import com.acornui.headless.MockComponent
import com.acornui.test.assertListEquals
import org.junit.Test

import org.junit.Assert.*

class ContainerImplTest {

	@Test
	fun addChild() {
		object : ContainerImpl(HeadlessInjector.owner) {
			init {
				val a = addChild(MockComponent())
				val b = addChild(MockComponent())
				val c = addChild(MockComponent())
				assertListEquals(listOf(a, b, c), children)
				assertEquals(this, a.parent)
				assertEquals(this, b.parent)
				assertEquals(this, c.parent)
			}
		}
	}

	@Test
	fun addOptionalChild() {
		object : ContainerImpl(HeadlessInjector.owner) {
			init {
				val a = addChild(MockComponent())
				addOptionalChild(null)
				val c = addChild(MockComponent())
				assertListEquals(listOf(a, c), children)
			}
		}
	}

	@Test
	fun removeChild() {
		object : ContainerImpl(HeadlessInjector.owner) {
			init {
				val a = addChild(MockComponent("a"))
				val b = addChild(MockComponent("b"))
				val c = addChild(MockComponent("c"))
				assertListEquals(listOf(a, b, c), children)
				removeChild(b)
				assertListEquals(listOf(a, c), children)
				assertNull(b.parent)
			}
		}
	}

	@Test
	fun clearChildren() {
		object : ContainerImpl(HeadlessInjector.owner) {
			init {
				val a = addChild(MockComponent("a"))
				val b = addChild(MockComponent("b"))
				val c = addChild(MockComponent("c"))
				assertListEquals(listOf(a, b, c), children)
				clearChildren(dispose = false)
				assertListEquals(listOf(), children)
				assertNull(a.parent)
				assertNull(b.parent)
				assertNull(c.parent)
				_children.addAll(a, b, c)
				assertListEquals(listOf(a, b, c), children)
				clearChildren(dispose = true)
				assertTrue(a.isDisposed)
				assertTrue(b.isDisposed)
				assertTrue(c.isDisposed)
			}
		}
	}

	@Test
	fun activate() {
		object : ContainerImpl(HeadlessInjector.owner) {
			init {
				val a = addChild(MockComponent("a"))
				val b = addChild(MockComponent("b"))
				val c = addChild(MockComponent("c"))

				activate()
				assertTrue(a.isActive)
				assertTrue(b.isActive)
				assertTrue(c.isActive)
				deactivate()
				assertFalse(a.isActive)
				assertFalse(b.isActive)
				assertFalse(c.isActive)
			}
		}
	}

	@Test
	fun invalidate() {
		object : ContainerImpl(HeadlessInjector.owner) {
			init {
				val a = addChild(MockComponent("a"))
				val b = addChild(MockComponent("b"))
				val c = addChild(MockComponent("c"))

				update()
				// Bubbling flags should bubble.
				assertEquals(0, invalidFlags)
				assertEquals(0, a.invalidFlags)
				a.invalidate(bubblingFlags)
				assertTrue(invalidFlags.containsFlag(bubblingFlags))
				update()

				assertEquals(0, a.invalidFlags)

				// Cascading flags should cascade.
				invalidate(cascadingFlags)
				assertTrue(a.invalidFlags.containsFlag(cascadingFlags))
				assertTrue(b.invalidFlags.containsFlag(cascadingFlags))
				assertTrue(c.invalidFlags.containsFlag(cascadingFlags))

				// Non-cascading flags shouldn't cascade.
				update()
				invalidate(ValidationFlags.HIERARCHY_ASCENDING)
				assertEquals(0, a.invalidFlags)
				assertEquals(0, b.invalidFlags)
				assertEquals(0, c.invalidFlags)
			}
		}
	}

	@Test
	fun update() {
		object : ContainerImpl(HeadlessInjector.owner) {
			init {
				var updateCountA = 0
				addChild(object : MockComponent("a") {
					override fun update() {
						super.update()
						updateCountA++
					}
				})
				var updateCountB = 0
				addChild(object : MockComponent("b") {
					override fun update() {
						super.update()
						updateCountB++
					}
				})
				var updateCountC = 0
				addChild(object : MockComponent("c") {
					override fun update() {
						super.update()
						updateCountC++
					}
				})

				update()
				assertEquals(updateCountA, 1)
				assertEquals(updateCountB, 1)
				assertEquals(updateCountC, 1)
				update()
				assertEquals(updateCountA, 2)
				assertEquals(updateCountB, 2)
				assertEquals(updateCountC, 2)
			}
		}
	}

	@Test
	fun drawTest() {
		object : ContainerImpl(HeadlessInjector.owner) {
			init {
				var drawCountA = 0
				addChild(object : MockComponent("a") {
					override fun draw() {
						super.draw()
						drawCountA++
					}
				})
				var drawCountB = 0
				addChild(object : MockComponent("b") {
					override fun draw() {
						super.draw()
						drawCountB++
					}
				})
				var drawCountC = 0
				addChild(object : MockComponent("c") {
					override fun draw() {
						super.draw()
						drawCountC++
					}
				})

				render()
				assertEquals(drawCountA, 1)
				assertEquals(drawCountB, 1)
				assertEquals(drawCountC, 1)
				render()
				assertEquals(drawCountA, 2)
				assertEquals(drawCountB, 2)
				assertEquals(drawCountC, 2)
			}
		}
	}

	@Test
	fun getChildrenUnderPoint() {
	}

	@Test
	fun createSlot() {
	}

	@Test
	fun isValidatingLayout() {
	}

	@Test
	fun onChildInvalidated() {
		object : ContainerImpl(HeadlessInjector.owner) {

			private var childInvalidatedCount: Int = 0

			init {
				val a = addChild(MockComponent("a"))
				update()
				a.invalidate(-1)
				assertEquals(1, childInvalidatedCount)

				a.invalidate(-1)
				assertEquals(1, childInvalidatedCount)

				update()
				a.invalidate(-1)
				assertEquals(2, childInvalidatedCount)

				// Removal should remove the handler.
				removeChild(a)
				update()
				a.invalidate(-1)
				assertEquals(2, childInvalidatedCount)
			}

			override fun onChildInvalidated(child: UiComponent, flagsInvalidated: Int) {
				super.onChildInvalidated(child, flagsInvalidated)
				childInvalidatedCount++
			}
		}
	}

	@Test
	fun childDisposedHandler() {

		object : ContainerImpl(HeadlessInjector.owner) {

			private var childDisposedCount: Int = 0

			init {
				val a = addChild(MockComponent("a"))
				val b = addChild(MockComponent("b"))
				val c = addChild(MockComponent("c"))
				update()
				a.dispose()
				assertEquals(1, childDisposedCount)
				b.dispose()
				assertEquals(2, childDisposedCount)

				// Removal should remove the handler.
				removeChild(c)
				c.dispose()
				assertEquals(2, childDisposedCount)
			}

			override fun onChildDisposed(child: UiComponent) {
				super.onChildDisposed(child)
				childDisposedCount++
			}
		}
	}

	@Test
	fun dispose() {
		object : ContainerImpl(HeadlessInjector.owner) {

			init {
				val a = addChild(spacer())
				val b = addChild(spacer())
				val c = spacer()

				dispose()
				assertTrue(a.isDisposed)
				assertTrue(b.isDisposed)
				assertTrue(c.isDisposed)
				assertTrue(isDisposed)
			}

		}
	}
}