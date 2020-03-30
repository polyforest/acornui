/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.component.style

import com.acornui.collection.AlwaysFilter
import com.acornui.component.ComponentInit
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.test.Test
import kotlin.test.assertEquals

class CascadingStyleCalculatorTest {

	@Test
	fun calculate() {

		val tagA = styleTag()
		val tagB = styleTag()
		val tagC = styleTag()

		// Stylable `a` is the root.
		// b : a
		// c : a

		val a = object : Stylable {
			override val styleTags = arrayListOf(tagA)
			override val styleRules = arrayListOf<StyleRule<*>>()

			override val styleParent: Stylable? = null

			override fun invalidateStyles() {}

			init {
				simpleStyle(withAncestor(tagA), -1f) {
					bar = "AForA_bar"
				}
				simpleStyle(withAncestor(tagC)) {
					bar = "InheritanceFail"
				}
				simpleStyle(withAncestor(tagB)) {
					bar = "PriorityFail"
					foo = "PriorityFail"
				}
				simpleStyle(withAncestor(tagB)) {
					bar = "AForB_bar"
				}
				// This higher priority should override the explicit value foo on b
				simpleStyle(withAncestor(tagB), priority = 1f) {
					foo = "AForB_foo"
				}
			}
		}

		val stylableB = object : Stylable {
			override val styleTags = arrayListOf(tagB)
			override val styleRules = arrayListOf<StyleRule<*>>()

			override val styleParent: Stylable? = a

			override fun invalidateStyles() {}

			init {
				simpleStyle(withAncestor(tagB), -1f) {
					bar = "PriorityFail"
					baz = "BForB_bar"
				}
			}
		}

		val c = object : Stylable {
			override val styleTags = arrayListOf(tagC)
			override val styleRules = arrayListOf<StyleRule<*>>()

			override val styleParent = a

			override fun invalidateStyles() {}

			init {
				simpleStyle(withAncestor(tagC)) {
					bar = "CForC_bar"
				}
			}
		}

		val styleC = SimpleStyle()
		CascadingStyleCalculator.calculate(c, styleC)
		assertEquals("CForC_bar", styleC.bar)
		assertEquals("baz", styleC.baz)
		assertEquals("foo", styleC.foo)

		val styleB = SimpleStyle()
		CascadingStyleCalculator.calculate(stylableB, styleB)
		assertEquals("AForB_bar", styleB.bar)
		assertEquals("BForB_bar", styleB.baz)
		assertEquals("AForB_foo", styleB.foo)

		val styleA = SimpleStyle()
		CascadingStyleCalculator.calculate(a, styleA)
		assertEquals("AForA_bar", styleA.bar)
		assertEquals("baz", styleA.baz)
		assertEquals("foo", styleA.foo)
	}

}

private fun <T : StyleRo> Stylable.filterRules(type: StyleType<T>, out: MutableList<StyleRule<T>>) {
	out.clear()
	@Suppress("UNCHECKED_CAST")
	(styleRules as Iterable<StyleRule<T>>).filterTo(out, { it.style.type == type })
}

private class SimpleStyle : StyleBase() {
	override val type = Companion

	var foo by prop("foo")
	var bar by prop("bar")
	var baz by prop("baz")

	companion object : StyleType<SimpleStyle>
}

private fun Stylable.simpleStyle(filter: StyleFilter = AlwaysFilter, priority: Float = 0f, init: ComponentInit<SimpleStyle>) {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val style = SimpleStyle().apply(init)
	styleRules.add(StyleRule(style, filter, priority))
}