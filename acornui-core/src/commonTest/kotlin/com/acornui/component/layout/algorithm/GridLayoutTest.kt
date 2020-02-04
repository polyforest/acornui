package com.acornui.component.layout.algorithm

import com.acornui.component.layout.LayoutElement
import com.acornui.test.assertListEquals
import kotlin.test.Test


class GridLayoutTest {

	@Test fun cellWalkWithRowSpan() {
		val layout = GridLayout()
		val style = GridLayoutStyle()
		style.columns = listOf(GridColumn(), GridColumn(), GridColumn(), GridColumn())

		val list = createSpacers(19)
		list[1].layoutData {
			rowSpan = 2
		}
		list[4].layoutData {
			rowSpan = 4
		}
		list[5].layoutData {
			rowSpan = 2
		}
		list[12].layoutData {
			rowSpan = 3
		}
		list[17].layoutData {
			rowSpan = 3
		}

		/**
		 * 0  1  2  3
		 * 4  1  5  6
		 * 4  7  5  8
		 * 4  9  10 11
		 * 4  12 13 14
		 * 15 12 16 17
		 * 18       17
		 *          17
		 */

		val positions = ArrayList<Pair<Int, Int>>()
		layout.cellWalk(list, style) {
			_, rowIndex, colIndex ->
			positions.add(Pair(rowIndex, colIndex))
		}

		assertListEquals(
				arrayOf(
						Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3),
						Pair(1, 0),             Pair(1, 2), Pair(1, 3),
						Pair(2, 1),                         Pair(2, 3),
						            Pair(3, 1), Pair(3, 2), Pair(3, 3),
						            Pair(4, 1), Pair(4, 2), Pair(4, 3),
						Pair(5, 0),             Pair(5, 2), Pair(5, 3),
						Pair(6, 0)
				), positions)
	}

	@Test fun cellWalkWithRowAndColSpan() {
		val layout = GridLayout()
		val style = GridLayoutStyle()
		style.columns = listOf(GridColumn(), GridColumn(), GridColumn(), GridColumn())

		val list = createSpacers(8)
		list[1].layoutData {
			rowSpan = 2
			colSpan = 3
		}
		list[3].layoutData {
			rowSpan = 4
			colSpan = 2
		}
		list[4].layoutData {
			rowSpan = 4
		}
		list[5].layoutData {
			rowSpan = 2
			colSpan = 3
		}

		/**
		 * 0  1  1  1
		 * 2  1  1  1
		 * 3  3  4  5  5  5
		 * 3  3  4  5  5  5
		 * 3  3  4  6
		 * 3  3  4  7
		 */

		val positions = ArrayList<Pair<Int, Int>>()
		layout.cellWalk(list, style) {
			_, rowIndex, colIndex ->
			positions.add(Pair(rowIndex, colIndex))
		}


		assertListEquals(
				arrayOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(2, 0), Pair(2, 2), Pair(2, 3), Pair(4, 3), Pair(5, 3)), positions)
	}

	private fun createSpacers(n: Int): MutableList<LayoutElement> {
		val list = ArrayList<LayoutElement>()
		for (i in 0 until n) {
			list.add(spacer("[Spacer $i]"))
		}
		return list
	}

	fun spacer(name: String): LayoutElement {
		return DummySpacer(name)
	}

	fun LayoutElement.layoutData(init: GridLayoutData.()->Unit): GridLayoutData {
		val g = GridLayoutData()
		g.init()
		layoutData = (g)
		return g
	}

}