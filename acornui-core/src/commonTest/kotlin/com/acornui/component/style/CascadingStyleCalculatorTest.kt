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
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal
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

		val a = object : SimpleStylable() {
			override val styleTags = arrayListOf(tagA)
			override val styleRules = arrayListOf<StyleRo>()

			override val styleParent: Stylable? = null

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

				// This lower priority should not override the explicit value foo on b
				simpleStyle(withAncestor(tagB), priority = -1f) {
					foo = "PriorityFail"
				}
			}
		}

		val stylableB = object : SimpleStylable() {
			override val styleTags = arrayListOf(tagB)
			override val styleRules = arrayListOf<StyleRo>()

			override val styleParent: Stylable? = a

			init {
				simpleStyle(withAncestor(tagB), -1f) {
					bar = "PriorityFail"
					baz = "BForB_bar"
				}
			}
		}

		val c = object : SimpleStylable() {
			override val styleTags = arrayListOf(tagC)
			override val styleRules = arrayListOf<StyleRo>()

			override val styleParent = a

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

private abstract class SimpleStylable : Stylable {

	override val stylesInvalidated: Signal<(StylableRo) -> Unit> = emptySignal()

	override fun invalidateStyles() {}
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
	style.filter = filter
	style.priority = priority
	styleRules.add(style)
}