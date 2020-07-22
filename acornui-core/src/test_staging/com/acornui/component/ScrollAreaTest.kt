package com.acornui.component

import com.acornui.component.scroll.scrollArea
import com.acornui.graphic.exit
import com.acornui.headless.headlessApplication
import com.acornui.test.runMainTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ScrollAreaTest {

	@Test
	fun globalTransform() = runMainTest {
		headlessApplication {
			val b: UiComponent
			val a = +scrollArea {
				b = +UiComponentImpl(this)
			}
			a.position(0.0, 0.0)
			assertEquals(0.0, b.transformGlobal.translationY)
			a.position(0.0, 50.0)
			assertEquals(50.0, b.transformGlobal.translationY)
			exit()
		}
	}
}