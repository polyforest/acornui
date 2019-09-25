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

package com.acornui.headless

import com.acornui.async.delay
import com.acornui.async.launch
import com.acornui.component.ComponentInit
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentImpl
import com.acornui.component.stage
import com.acornui.di.Owned
import com.acornui.graphic.exit
import com.acornui.math.Easing
import com.acornui.test.assertClose
import com.acornui.test.runTest
import com.acornui.time.start
import com.acornui.time.timer
import com.acornui.tween.tweenX
import kotlin.test.Test
import kotlin.time.seconds

class HeadlessTest {

	@Test
	fun start() = runTest {
		headlessApplication {
			exit()
		}
	}

	@Test
	fun addToStage() = runTest {
		headlessApplication {
			stage.addElement(testComponent())
			exit()
		}
	}

	@Test
	fun tweenX() = runTest {
		headlessApplication {
			testComponent {
				tweenX(4f, Easing.linear, 100f).start()
				launch {
					delay(1.seconds)
					assertClose(25f, x, margin =  5f)
					delay(1.seconds)
					assertClose(50f, x, margin =  5f)
					delay(1.seconds)
					assertClose(75f, x, margin =  5f)
					delay(1.seconds)
					assertClose(100f, x, margin =  5f)
				}
			}

			timer(5.seconds) {
				exit()
			}
		}
	}
}

private class TestComponent(owner: Owned) : UiComponentImpl(owner)

private inline fun Owned.testComponent(init: ComponentInit<UiComponent> = {}): UiComponent = TestComponent(this).apply(init)