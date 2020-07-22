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

package com.acornui.focus

import com.acornui.component.Stage
import com.acornui.component.StageImpl
import com.acornui.component.UiComponent
import com.acornui.component.container
import com.acornui.component.layout.spacer
import com.acornui.di.Context
import com.acornui.graphic.exit
import com.acornui.headless.headlessApplication
import com.acornui.input.InteractivityManager
import com.acornui.test.runMainTest
import kotlin.test.Test

class FocusManagerImplTest {

	@Test
	fun testFocusOrder1() = focusManagerTest {
		val a = +spacer {
			focusEnabled = true
		}
		val b = +spacer {
			focusEnabled = true
		}
		val c = +spacer {
			focusEnabled = true
		}
		+spacer {
			focusEnabled = false
		}

		assertListEquals(listOf(a, b, c), focusables)
	}

	@Test
	fun testFocusOrderWithFocusOrder() = focusManagerTest {
		val a = +spacer {
			focusEnabled = true
			focusOrder = 1.0
			componentId = "a"
		}
		val b = +spacer {
			focusEnabled = true
			focusOrder = 0.0
			componentId = "b"
		}
		val c = +spacer {
			focusEnabled = true
			focusOrder = 2.0
			componentId = "c"
		}
		+spacer {
			focusEnabled = false
		}

		assertListEquals(listOf(b, a, c), focusables)
	}

	@Test
	fun testFocusOrderWithFocusContainer() = focusManagerTest {
		val a = +spacer {
			focusEnabled = true
			componentId = "a"
		}
		val b = +spacer {
			focusEnabled = true
			componentId = "b"
		}
		val a1: UiComponent
		val b1: UiComponent
		+container {
			a1 = +spacer {
				focusEnabled = true
				componentId = "a1"
			}
			b1 = +spacer {
				focusEnabled = true
				componentId = "b1"
			}
		}
		val c = +spacer {
			focusEnabled = true
			focusOrder = 2.0
			componentId = "c"
		}
		+spacer {
			focusEnabled = false
		}

		assertListEquals(listOf(a, b, a1, b1, c), focusables)
	}

	@Test
	fun testFocusOrderWithFocusContainerAndOrder() = focusManagerTest {
		val a = +spacer {
			focusEnabled = true
			componentId = "a"
		}
		val b = +spacer {
			focusEnabled = true
			componentId = "b"
		}
		val a1: UiComponent
		val b1: UiComponent
		+container {
			a1 = +spacer {
				focusEnabled = true
				componentId = "a1"
				focusOrder = -1.0
			}
			b1 = +spacer {
				focusEnabled = true
				componentId = "b1"
				focusOrder = -2.0
			}
		}

		val a2: UiComponent
		val b2: UiComponent
		+container {
			componentId = "container2"
			isFocusContainer = true
			a2 = +spacer {
				focusEnabled = true
				componentId = "a2"
				focusOrder = -1.0
			}
			b2 = +spacer {
				focusEnabled = true
				componentId = "b2"
				focusOrder = -2.0
			}
		}

		val c = +spacer {
			focusEnabled = true
			focusOrder = 2.0
			componentId = "c"
		}
		+spacer {
			focusEnabled = false
		}

		assertListEquals(listOf(b1, a1, a, b, b2, a2, c), focusables)
	}

	@Test
	fun containerBeforeChildren() = focusManagerTest {
		val a = +spacer {
			focusEnabled = true
			componentId = "a"
		}
		val b = +spacer {
			focusEnabled = true
			componentId = "b"
		}
		val a1: UiComponent
		val b1: UiComponent
		val c = +container {
			focusEnabled = true
			isFocusContainer = true

			a1 = +spacer {
				focusEnabled = true
				componentId = "a1"
				focusOrder = -1.0
			}
			b1 = +spacer {
				focusEnabled = true
				componentId = "b1"
				focusOrder = -2.0
			}
		}

		val d_a = spacer {
			focusEnabled = true
			componentId = "d_a"
		}
		val d_b = spacer {
			focusEnabled = true
			componentId = "d_b"
			focusOrder = -1.0
		}
		val d = +container {
			focusEnabled = true
			isFocusContainer = true
			+d_a
			+d_b
			componentId = "d"
		}
		assertListEquals(listOf(a, b, c, b1, a1, d, d_b, d_a), focusables)

		val d_c = d.addElement(spacer {
			componentId = "d_c"
			focusEnabled = true
		})
		assertListEquals(listOf(a, b, c, b1, a1, d, d_b, d_a, d_c), focusables)
		d.focusEnabled = false
		assertListEquals(listOf(a, b, c, b1, a1, d_b, d_a, d_c), focusables)
		d.focusEnabled = true
		assertListEquals(listOf(a, b, c, b1, a1, d, d_b, d_a, d_c), focusables)
//		-d
//		assertListEquals(listOf(a, b, c, b1, a1), focusables)
//		+d
//		assertListEquals(listOf(a, b, c, b1, a1, d, d_b, d_a, d_c), focusables)
	}
}

private val Context.focusables: List<Focusable>
	get() = inject(FocusManager).focusables

private fun focusManagerTest(onReady: Stage.() -> Unit) = runMainTest {
	headlessApplication {
		val focusManager = FocusManagerImpl(inject(InteractivityManager))
		(this as StageImpl).dependencies += FocusManager to focusManager
		focusManager.init(this)
		onReady()
		exit()
	}
}