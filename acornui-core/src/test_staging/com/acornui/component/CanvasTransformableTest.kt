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
import com.acornui.math.vec2
import com.acornui.math.vec3
import com.acornui.test.runMainTest
import com.acornui.test.assertClose
import kotlin.test.Test

class CanvasTransformableTest {

	@Test
	fun localToCanvas() = runMainTest {
		val config = AppConfig(window = WindowConfig(initialWidth = 600.0, initialHeight = 400.0))
		headlessApplication(config) {
			val c: UiComponent
			val d: UiComponent
			+container {
				c = +container {
					size(200.0, 100.0)
					position(50.0, 75.0 )

					d = +spacer(200.0, 100.0) {
						setScaling(2.0, 2.0)
						position(120.0, 170.0 )
					}
				}
			}
			validate()
			assertClose(vec2(50.0, 75.0), c.localToCanvas(vec3(0.0, 0.0, 0.0)).toVec2())
			assertClose(Rectangle(50.0, 75.0, width = 200.0, height = 100.0), c.localToCanvas(c.bounds.toRectangle()))
			assertClose(Rectangle(120.0 + 50.0, 170.0 + 75.0, width = 400.0, height = 200.0), d.localToCanvas(d.bounds.toRectangle()))

			exit()
		}
	}
}