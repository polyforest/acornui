package com.acornui.component.layout.algorithm

import com.acornui.component.layout.LayoutElement
import com.acornui.math.Bounds
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowLayoutTest {

	@Test
	fun getLines() {

		val flowLayout = FlowLayout()

		val elements = arrayListOf(
				spacer("one", 100f, 50f),
				spacer("two", 100f, 50f),
				spacer("three", 100f, 50f),
				spacer("four", 100f, 50f),
				spacer("five", 100f, 50f)
		)
		val bounds = Bounds()
		flowLayout.layout(250f, null, elements, bounds)

		assertEquals(3, flowLayout.lines.size)
		assertEquals(0, flowLayout.lines[0].startIndex)
		assertEquals(2, flowLayout.lines[0].endIndex)
		assertEquals(2, flowLayout.lines[1].startIndex)
		assertEquals(4, flowLayout.lines[1].endIndex)
		assertEquals(4, flowLayout.lines[2].startIndex)
		assertEquals(5, flowLayout.lines[2].endIndex)
	}

	@Test
	fun getElementInsertionIndex() {
		val flowLayout = FlowLayout()
		val elements = arrayListOf(
				spacer("one", 40f, 10f),
				spacer("two", 90f, 50f),
				spacer("three", 30f, 50f),
				spacer("four", 60f, 30f),
				spacer("five", 90f, 20f),
				spacer("six", 70f, 70f),
				spacer("seven", 20f, 30f),
				spacer("eight", 30f, 10f),
				spacer("nine", 80f, 50f)
		)

		val bounds = Bounds()
		val style = flowLayout.style
		style.horizontalGap = 10f
		style.verticalGap = 20f
		flowLayout.layout(250f, null, elements, bounds)

		// [40,10], [90,50], [30,50], [60, 30]      Line: width: 200, height: 50, y: 0
		// [90,20], [70, 70], [20, 30], [30, 10]    Line: width: 240, height: 70, y: 70
		// [80,50] 									Line: width: 80,  height: 50, y: 160

		assertEquals(0, flowLayout.getElementInsertionIndex(0f, 0f, elements, style))
		assertEquals(0, flowLayout.getElementInsertionIndex(-1f, 0f, elements, style))
		assertEquals(0, flowLayout.getElementInsertionIndex(100f, -1f, elements, style))
		assertEquals(1, flowLayout.getElementInsertionIndex(40f, 0f, elements, style))
		assertEquals(2, flowLayout.getElementInsertionIndex(179f, 0f, elements, style))

		assertEquals(4, flowLayout.getElementInsertionIndex(-1f, 70f, elements, style))


		assertEquals(8, flowLayout.getElementInsertionIndex(-1f, 160f, elements, style))
		assertEquals(elements.lastIndex, flowLayout.getElementInsertionIndex(79f, 209f, elements, style))
		assertEquals(elements.size, flowLayout.getElementInsertionIndex(0f, 210f, elements, style))
		assertEquals(elements.size, flowLayout.getElementInsertionIndex(80f, 209f, elements, style))
	}

	fun spacer(name: String, width: Float, height: Float): LayoutElement {
		return DummySpacer(name, width, height)
	}

}
