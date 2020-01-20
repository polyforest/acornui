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

package com.acornui.math

import com.acornui.test.assertClose
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierTest {

	@Test
	fun apply() {
		val b = Bezier(floatArrayOf(0.032f, 0.032f, 0.127f, 0.679f, 0.138f, 0.565f, 0.181f, 0.1f, 0.374f, 0.008f, 0.421f, 0.055f, 0.565f, 0.199f, 0.821f, 0.253f, 0.821f, 0.654f, 0.821f, 0.865f, 0.889f, 0.889f))
		val expected = floatArrayOf(0f, .122f, .295f, .4705f, .565f, .355f, .2465f, .173f, .1205f, 0.083f, 0.0575f, 0.045f, 0.0495f, 0.0805f, 0.108f, 0.1335f, 0.1585f, 0.1835f, 0.21f, 0.2395f, 0.2735f, 0.315f, 0.369f, 0.451f, 0.7515f, 0.8535f, 0.8995f, 0.9345f, 0.9665f, 1f)
		for (i in 0..29) {
			val alpha = i / 29.toFloat()
			assertClose(expected[i], b.apply(alpha), maxDifference = 0.005f)
		}
	}
}