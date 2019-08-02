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

package com.acornui.graphic

import com.acornui.math.Vector3
import com.acornui.test.assertClose
import kotlin.test.Test

/**
 * @author nbilyk
 */
class PerspectiveCameraTest {

	@Test fun project() {
		val cam = PerspectiveCamera()
		val viewportX = 0f
		val viewportY = 0f
		val viewportW = 400f
		val viewportH = 300f
		cam.setViewport(viewportW, viewportH)
		cam.moveToLookAtRect(0f, 0f, viewportW, viewportH)

		val coords = Vector3(0f, 0f, 0f)
		cam.project(coords, viewportX, viewportY, viewportW, viewportH)
		assertClose(0f, coords.x, 0.1f)
		assertClose(0f, coords.y, 0.1f)

		coords.set(400f, 300f, 0f)
		cam.project(coords, viewportX, viewportY, viewportW, viewportH)
		assertClose(400f, coords.x, 0.1f)
		assertClose(300f, coords.y, 0.1f)

		coords.set(0.020372868f, 0.015261769f, 0.9960859f)
		//cam.project(coords)
		cam.canvasToGlobal(coords, viewportX, viewportY, viewportW, viewportH)
	}

}