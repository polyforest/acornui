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

import com.acornui.component.ComponentInit
import com.acornui.logging.Log
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.test.Test
import kotlin.test.assertEquals

class HierarchyDiagramTest {

	@Test
	fun toDiagram() {
		val diagram = n("root") {
			+n("a") {
				+n("a.1") {
					+n("a.1.a")
					+n("a.1.b")
				}
				+n("a.2")
				+n("a.3")
			}
			+n("b") {
				+n("b.1")
				+n("b.2")
			}
			+n("c") {
				+n("c.1")
				+n("c.2")
			}
		}.toDiagram { str }

		Log.debug(diagram)
		assertEquals("""
			root
			├──a
			│  ├──a.1
			│  │  ├──a.1.a
			│  │  └──a.1.b
			│  ├──a.2
			│  └──a.3
			├──b
			│  ├──b.1
			│  └──b.2
			└──c
			   ├──c.1
			   └──c.2
		""".trimIndent(), diagram)
	}

	private data class N(val str: String): Node {
		override val children = ArrayList<N>()

		operator fun N.unaryPlus() {
			this@N.children += this
		}
	}

	private inline fun n(str: String, init: ComponentInit<N> = {}): N {
//		contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
		return N(str).apply(init)
	}
}
