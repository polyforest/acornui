/*
 * Copyright 2014 Nicholas Bilyk
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

package com.acornui.collection

import com.acornui.recycle.Clearable
import com.acornui.recycle.limitedPool
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author nbilyk
 */
class LimitedPoolTest {

	@Test fun testObtain() {
		val pool = limitedPool(16, { ClearableTest() })
		val item1 = pool.obtain()
		val item2 = pool.obtain()
		val item3 = pool.obtain()
		assertEquals(0, item1.useCount)
		assertEquals(0, item2.useCount)
		assertEquals(0, item3.useCount)
		pool.free(item1)
		pool.free(item2)
		pool.free(item3)
		val item4 = pool.obtain()
		val item5 = pool.obtain()
		val item6 = pool.obtain()
		assertEquals(1, item4.useCount)
		assertEquals(1, item5.useCount)
		assertEquals(1, item6.useCount)
	}

	@Test fun testObtainPastCapacity() {
		val pool = limitedPool(4, { ClearableTest() })
		val item1 = pool.obtain()
		val item2 = pool.obtain()
		val item3 = pool.obtain()
		val item4 = pool.obtain()
		val item5 = pool.obtain()
		val item6 = pool.obtain()
		assertEquals(1, item1.useCount) // 1 and 2 should be re-used, despite being 'active'.
		assertEquals(1, item2.useCount)
		assertEquals(0, item3.useCount)
		assertEquals(0, item4.useCount)
		assertEquals(1, item5.useCount)
		assertEquals(1, item6.useCount)
	}

	private class ClearableTest : Clearable {

		var useCount: Int = 0

		override fun clear() {
			useCount++
		}
	}
}