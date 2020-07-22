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

import com.acornui.graphic.Color
import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlin.test.Test
import kotlin.test.assertEquals

class LinearGradientTest {

    @Test fun toCssString() {
        assertEquals("linear-gradient(to bottom, rgba(255, 0, 0, 1) 0px)", LinearGradient(GradientDirection.BOTTOM, ColorStop(Color.RED, dp = 0.0)).toCssString())
    }

    @Test fun serialize() {
        val g = LinearGradient(GradientDirection.ANGLE, Color.RED, Color.BLACK, Color.BLUE)
        val json = jsonStringify(LinearGradient.serializer(), g)
        assertEquals(g, jsonParse(LinearGradient.serializer(), json))
    }
}