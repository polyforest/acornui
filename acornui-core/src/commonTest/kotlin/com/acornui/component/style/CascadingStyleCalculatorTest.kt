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
			override val styleRules = arrayListOf<StyleRule<*>>(
					StyleRule(
							simpleStyle {
								bar = "AForA_bar"
							},
							withAncestor(tagA),
							-1f
					),
					StyleRule(
							simpleStyle {
								bar = "InheritanceFail"
							},
							withAncestor(tagC),
							0f
					),
					StyleRule(
							simpleStyle {
								bar = "PriorityFail"
								foo = "PriorityFail"
							},
							withAncestor(tagB),
							0f
					),
					StyleRule(
							simpleStyle {
								bar = "AForB_bar"
							},
							withAncestor(tagB),
							0f
					),
					StyleRule(
							simpleStyle {
								foo = "AForB_foo"
							},
							withAncestor(tagB),
							1f // This higher priority should override the explicit value foo on b
					)
			)

			override fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) = filterRules(type, out)

			override val styleParent: Stylable? = null

			override fun invalidateStyles() {}
		}

		val stylableB = object : Stylable {
			override val styleTags = arrayListOf(tagB)
			override val styleRules = arrayListOf<StyleRule<*>>(
					StyleRule(
							simpleStyle {
								bar = "PriorityFail"
								baz = "BForB_bar"
							},
							withAncestor(tagB),
							-1f
					)
			)

			override fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) = filterRules(type, out)

			override val styleParent: Stylable? = a

			override fun invalidateStyles() {}
		}

		val c = object : Stylable {
			override val styleTags = arrayListOf(tagC)
			override val styleRules = arrayListOf<StyleRule<*>>(
					StyleRule(
							simpleStyle {
								bar = "CForC_bar"
							},
							withAncestor(tagC),
							0f
					)
			)

			override fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) = filterRules(type, out)

			override val styleParent = a

			override fun invalidateStyles() {}
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

private fun simpleStyle(inner: SimpleStyle.() -> Unit): SimpleStyle = SimpleStyle().apply(inner)