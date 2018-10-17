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

import org.junit.Test

import org.junit.Assert.*

class CascadingStyleCalculatorTest {

	@Test
	fun calculate() {

		val tagA = styleTag()
		val tagB = styleTag()
		val tagC = styleTag()

		val a = object : Styleable {
			override val styleTags = arrayListOf(tagA)
			override val styleRules = arrayListOf<StyleRule<*>>(
					StyleRule(
							SimpleStyle().apply {
								bar = "AForA_bar"
							},
							withAncestor(tagA),
							-1f
					),
					StyleRule(
							SimpleStyle().apply {
								bar = "InheritanceFail"
							},
							withAncestor(tagC),
							0f
					),
					StyleRule(
							SimpleStyle().apply {
								bar = "PriorityFail"
								foo = "PriorityFail"
							},
							withAncestor(tagB),
							0f
					),
					StyleRule(
							SimpleStyle().apply {
								bar = "AForB_bar"
							},
							withAncestor(tagB),
							0f
					),
					StyleRule(
							SimpleStyle().apply {
								foo = "AForB_foo"
							},
							withAncestor(tagB),
							1f
					)
			)

			override fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) = filterRules(type, out)

			override val styleParent: Styleable? = null

			override fun invalidateStyles() {}
		}

		val b = object : Styleable {
			override val styleTags = arrayListOf(tagB)
			override val styleRules = arrayListOf<StyleRule<*>>(
					StyleRule(
							SimpleStyle().apply {
								bar = "PriorityFail"
								baz = "BForB_bar"
							},
							withAncestor(tagB),
							-1f
					)
			)

			override fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) = filterRules(type, out)

			override val styleParent: Styleable? = a

			override fun invalidateStyles() {}
		}

		val c = object : Styleable {
			override val styleTags = arrayListOf(tagC)
			override val styleRules = arrayListOf<StyleRule<*>>(
					StyleRule(
							SimpleStyle().apply {
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

		val styleB = SimpleStyle()
		CascadingStyleCalculator.calculate(b, styleB)
		assertEquals("AForB_bar", styleB.bar)
		assertEquals("BForB_bar", styleB.baz)
		assertEquals("AForB_foo", styleB.foo)

		val styleA = SimpleStyle()
		CascadingStyleCalculator.calculate(a, styleA)
		assertEquals("AForA_bar", styleA.bar)
	}

}

private fun <T : StyleRo> Styleable.filterRules(type: StyleType<T>, out: MutableList<StyleRule<T>>) {
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