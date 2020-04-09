package com.acornui.component

import com.acornui.component.scroll.scrollArea
import com.acornui.graphic.exit
import com.acornui.headless.headlessApplication
import com.acornui.runMainTest
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
			a.position(0f, 0f)
			assertEquals(0f, b.transformGlobal.translationY)
			a.position(0f, 50f)
			assertEquals(50f, b.transformGlobal.translationY)
			exit()
		}
	}
}