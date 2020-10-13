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

package com.acornui

import com.acornui.dom.add
import com.acornui.dom.divElement
import com.acornui.test.assertListEquals
import com.acornui.test.initMockDom
import org.w3c.dom.asList
import kotlin.test.BeforeTest
import kotlin.test.Test

class HtmlUtilsTest {

    @BeforeTest
    fun setup() {
        initMockDom()
    }

    @Test
    fun insertNode() {
        val div1 = divElement()
        val div2 = divElement()
        div1.add(0, div2)
        assertListEquals(listOf(div2), div1.childNodes.asList())
        val div3 = divElement()
        div1.add(0, div3)
        assertListEquals(listOf(div3, div2), div1.childNodes.asList())
        val div4 = divElement()
        div1.add(2, div4)
        assertListEquals(listOf(div3, div2, div4), div1.childNodes.asList())
        val div5 = divElement()
        div1.add(1, div5)
        assertListEquals(listOf(div3, div5, div2, div4), div1.childNodes.asList())
        val div6 = divElement()
        div1.add(0, div6)
        assertListEquals(listOf(div6, div3, div5, div2, div4), div1.childNodes.asList())
    }
}