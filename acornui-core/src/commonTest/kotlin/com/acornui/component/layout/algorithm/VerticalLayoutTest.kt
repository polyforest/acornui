/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.component.layout.algorithm

import com.acornui.component.layout.Spacer
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.mock.MockInjector
import kotlin.test.Test
import kotlin.test.assertEquals

class VerticalLayoutTest {

	private val owner = MockInjector.owner

	@Test fun basic() {
		val layout = VerticalLayout()
		val style = layout.style
		val gap = 7f
		style.gap = gap
		val list = arrayListOf(Spacer(owner, 10f, 5f), Spacer(owner, 12f, 3f), Spacer(owner, 28f, 29f))
		val size = Bounds()
		layout.layout(null, null, list, size)
		assertEquals(0f, list[0].y)
		assertEquals(5f + gap, list[1].y)
		assertEquals(5f + 3f + gap * 2, list[2].y)
		assertEquals(Bounds(28f, 5f + 3f + 29f + gap * 2, baseline = 5f), size)
	}

	@Test fun padding() {
		val layout = VerticalLayout()
		val style = layout.style
		val gap = 7f
		style.gap = gap
		val padding = Pad(7f, 8f, 9f, 6f)
		style.padding = padding
		val list = arrayListOf(Spacer(owner, 10f, 5f), Spacer(owner, 12f, 3f), Spacer(owner, 28f, 29f))
		val size = Bounds()
		layout.layout(null, null, list, size)
		assertEquals(0f + 7f, list[0].y)
		assertEquals(5f + gap + 7f, list[1].y)
		assertEquals(5f + 3f + gap * 2 + 7f, list[2].y)
		assertEquals(Bounds(28f + 6f + 8f, 5f + 3f + 29f + gap * 2 + 7f + 9f, baseline = 12f), size)
	}

//	@Test fun percentWidth() {
//		val layout = VerticalLayout()
//		val style = layout.style
//		val gap = 7f
//		style.gap = gap
//		val padding = Pad(7f, 8f, 9f, 6f)
//		style.padding = padding
//		val list = arrayListOf(Spacer(owner, 10f, 5f), Spacer(owner, 12f, 3f), Spacer(owner, 28f, 29f))
//		list[0].layoutData = (VerticalLayoutData().apply { widthPercent = 0.5f })
//		list[1].layoutData = (VerticalLayoutData().apply { widthPercent = 0.75f })
//		val size = Bounds()
//		layout.layout(100f, null, list, size)
//		assertEquals((100f - 6f - 8f) * 0.5f, list[0].width)
//		assertEquals(0f + 7f, list[0].y)
//		assertEquals((100f - 6f - 8f) * 0.75f, list[1].width)
//		assertEquals(5f + gap + 7f, list[1].y)
//		assertEquals(28f, list[2].width)
//		assertEquals(5f + 3f + gap * 2 + 7f, list[2].y)
//		assertEquals(Bounds(100f, 5f + 3f + 29f + gap * 2 + 7f + 9f, baseline = 12f), size)
//	}
//
//	/**
//	 * Percent based heights should contract to fit the available space.
//	 */
//	@Test fun percentHeight() {
//		val layout = VerticalLayout()
//		val style = layout.style
//		val gap = 7f
//		style.gap = gap
//		val padding = Pad(7f, 8f, 9f, 6f)
//		style.padding = padding
//		val list = arrayListOf(Spacer(owner, 10f, 5f), Spacer(owner, 12f, 3f), Spacer(owner, 28f, 29f))
//		list[0].layoutData = (VerticalLayoutData().apply { heightPercent = 0.5f })
//		list[1].layoutData = (VerticalLayoutData().apply { heightPercent = 0.75f })
//		val size = Bounds()
//		layout.layout(150f, 100f, list, size)
//		assertEquals(100f - 7f - 9f, list[0].height + list[1].height + list[2].height + gap * 2f)
//		assertEquals(Bounds(150f, 100f, baseline = 23.4f), size)
//	}
//
//	/**
//	 * Percent based heights shouldn't expand to fill the available space.
//	 */
//	@Test fun percentHeight2() {
//		val layout = VerticalLayout()
//		val style = layout.style
//		val gap = 3f
//		style.gap = gap
//		val padding = Pad(7f, 8f, 9f, 6f)
//		style.padding = padding
//		val list = arrayListOf(Spacer(owner, 10f, 5f), Spacer(owner, 12f, 3f), Spacer(owner, 28f, 29f))
//		list[0].layoutData = (VerticalLayoutData().apply { heightPercent = 0.25f })
//		list[1].layoutData = (VerticalLayoutData().apply { heightPercent = 0.25f })
//		val size = Bounds()
//		layout.layout(150f, 100f, list, size)
//		val h = 100f - 7f - 9f
//		assertEquals(h * 0.5f + 29f, list[0].height + list[1].height + list[2].height)
//		assertEquals(Bounds(150f, 93f, baseline = 28f), size)
//	}
}