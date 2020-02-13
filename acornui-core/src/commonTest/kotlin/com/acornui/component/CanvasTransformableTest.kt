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

package com.acornui.component

import com.acornui.AppConfig
import com.acornui.WindowConfig
import com.acornui.component.layout.spacer
import com.acornui.graphic.exit
import com.acornui.headless.headlessApplication
import com.acornui.math.Rectangle
import com.acornui.math.Vector2
import com.acornui.math.Vector3
import com.acornui.runMainTest
import com.acornui.test.assertClose
import kotlin.test.Test

class CanvasTransformableTest {

	@Test
	fun localToCanvas() = runMainTest {
		val config = AppConfig(window = WindowConfig(initialWidth = 600f, initialHeight = 400f))
		headlessApplication(config) {
			val c: UiComponentRo
			val d: UiComponentRo
			+container {
				c = +container {
					setSize(200f, 100f)
					moveTo(50f, 75f)

					d = +spacer(200f, 100f) {
						setScaling(2f, 2f)
						moveTo(120f, 170f)
					}
				}
			}
			validate()
			println("Canvas transformable test running")
			assertClose(Vector2(50f, 75f), c.localToCanvas(Vector3(0f, 0f, 0f)).toVec2())
			assertClose(Rectangle(50f, 75f, width = 200f, height = 100f), c.localToCanvas(c.bounds.toRectangle()))
			assertClose(Rectangle(120f + 50f, 170f + 75f, width = 400f, height = 200f), d.localToCanvas(d.bounds.toRectangle()))

			exit()
		}
	}
}