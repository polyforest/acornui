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

import com.acornui.di.Context
import com.acornui.headless.HeadlessDependencies
import com.acornui.test.assertListEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ItemRendererTest {

	@BeforeTest
	fun setup() {
		TestItemRenderer.clear()
	}

	@Test
	fun recycle() {
		HeadlessDependencies.owner.apply {
			// Do a loop to ensure that the pools are unique to the container.
			for (i in 0..3) {
				container<TestItemRenderer> {
					var expectedConstructed = TestItemRenderer.constructedCount
					val factory: ElementContainer<TestItemRenderer>.() -> TestItemRenderer = { TestItemRenderer(this) }
					recycleItemRenderers(listOf("first", "second", "third"), factory = factory)
					assertListEquals(listOf("first", "second", "third"), elements.map { it.data })

					recycleItemRenderers(listOf("first", "second", "third"), factory = factory)
					assertListEquals(listOf("first", "second", "third"), elements.map { it.data })
					expectedConstructed += 3
					assertEquals(expectedConstructed, TestItemRenderer.constructedCount)

					recycleItemRenderers(listOf("first", "second", "third", "fourth"), factory = factory)
					assertListEquals(listOf("first", "second", "third", "fourth"), elements.map { it.data })
					assertEquals(++expectedConstructed, TestItemRenderer.constructedCount)

					// Removed item should return to a pool.
					recycleItemRenderers(listOf("first", "third", "fourth"), factory = factory)
					assertListEquals(listOf("first", "third", "fourth"), elements.map { it.data })
					assertEquals(expectedConstructed, TestItemRenderer.constructedCount)

					// Added item should first draw from pool.
					recycleItemRenderers(listOf("first", "second", "third", "fourth"), factory = factory)
					assertListEquals(listOf("first", "second", "third", "fourth"), elements.map { it.data })
					assertEquals(expectedConstructed, TestItemRenderer.constructedCount)

					assertNull(elements.firstOrNull { it.owner != this })
				}
			}
		}

	}
}

private class TestItemRenderer(owner: Context) : UiComponentImpl(owner), ItemRenderer<String> {

	override var data: String? = null

	init {
		++constructedCount
	}

	companion object {
		var constructedCount: Int = 0

		fun clear() {
			constructedCount = 0
		}
	}
}
