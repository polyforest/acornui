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

import com.acornui.test.runApplicationTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplyCssTest {

	@Test fun singleProperty() = runApplicationTest { resolve, reject ->
		val d = div()
		d.applyCss("color: blue")
		assertEquals("blue", d.style.getPropertyValue("color"))
		resolve()
	}

	@Test fun singleProperty2() = runApplicationTest { resolve, reject ->
		val d = div()
		d.applyCss("align-self: center;")
		assertEquals("center", d.style.getPropertyValue("align-self"))
		resolve()
	}

	@Test fun multiProperty() = runApplicationTest { resolve, reject ->
		val d = div()
		d.applyCss("""
			color: blue;
			background: red;
			""")
		assertEquals("blue", d.style.getPropertyValue("color"))
		assertEquals("red", d.style.getPropertyValue("background"))
		resolve()
	}

	@Test fun variableProperty() = runApplicationTest { resolve, reject ->
		val d = div()
		d.applyCss("""
			--color: blue;
			--test234: red;
			--test_234: white;
			""")
		assertEquals("blue", d.style.getPropertyValue("--color"))
		assertEquals("red", d.style.getPropertyValue("--test234"))
		assertEquals("white", d.style.getPropertyValue("--test_234"))
		resolve()
	}

	@Test fun dashedProperty() = runApplicationTest { resolve, reject ->
		val d = div()
		d.applyCss("""
			align-self: center;
			""")
		assertEquals("center", d.style.getPropertyValue("align-self"))
		resolve()
	}
}