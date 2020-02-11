package com.acornui.component

import com.acornui.graphic.exit
import com.acornui.headless.headlessApplication
import com.acornui.runMain
import com.acornui.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UiComponentImplTest {

	@Test
	fun globalTransform() = runTest {
		runMain {
			headlessApplication {
				val b: UiComponent
				val a = +container {
					b = +UiComponentImpl(this)
				}
				a.moveTo(0f, 0f)
				assertEquals(0f, b.transformGlobal.translationY)
				a.moveTo(0f, 50f)
				assertEquals(50f, b.transformGlobal.translationY)
				exit()
			}
		}
	}
}