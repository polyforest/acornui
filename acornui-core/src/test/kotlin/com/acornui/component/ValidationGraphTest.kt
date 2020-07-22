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

import com.acornui.assertionsEnabled
import com.acornui.component.validation.ValidationGraph
import com.acornui.component.validation.containsAllFlags
import com.acornui.component.validation.containsAnyFlags
import com.acornui.component.validation.validationGraph
import kotlin.math.log2
import kotlin.test.*

class ValidationGraphTest {

	private val ONE: Int = 1 shl 0
	private val TWO: Int = 1 shl 1
	private val THREE: Int = 1 shl 2
	private val FOUR: Int = 1 shl 3
	private val FIVE: Int = 1 shl 4
	private val SIX: Int = 1 shl 5
	private val SEVEN: Int = 1 shl 6
	private val EIGHT: Int = 1 shl 7
	private val NINE: Int = 1 shl 8

	private lateinit var n: ValidationGraph

	@BeforeTest fun before() {
		assertionsEnabled = true
		n = validationGraph(::toFlagString) {
			addNode(ONE) {}
			addNode(TWO, ONE) {}
			addNode(THREE, TWO) {}
			addNode(FOUR, THREE) {}
			addNode(FIVE, TWO) {}
			addNode(SIX, FIVE) {}
			addNode(SEVEN, TWO) {}
		}
	}

	@Test fun invalidate() {
		n.validate()

		val f = n.invalidate(FIVE)
		assertEquals(FIVE or SIX, f)

		n.assertIsValid(ONE, TWO, THREE, FOUR, SEVEN)
		n.assertIsNotValid(FIVE, SIX)

		n.validate()

		val f2 = n.invalidate(TWO)
		assertEquals(TWO or THREE or FOUR or FIVE or SIX or SEVEN, f2)

		n.assertIsValid(ONE)
		n.assertIsNotValid(TWO, THREE, FOUR, FIVE, SIX, SEVEN)
	}

	@Test fun validate() {
		val f = n.validate(SEVEN or THREE)
		assertEquals(ONE or TWO or THREE or SEVEN, f)

		n.assertIsValid(ONE, TWO, THREE, SEVEN)
		n.assertIsNotValid(FIVE, SIX, FOUR)

		val iF = n.invalidate(FOUR)
		assertEquals(0, iF)

		// No change
		n.assertIsValid(ONE, TWO, THREE, SEVEN)
		n.assertIsNotValid(FIVE, SIX, FOUR)

		val iF2 = n.invalidate(THREE)
		assertEquals(THREE, iF2)

		n.assertIsValid(ONE, TWO, SEVEN)
		n.assertIsNotValid(THREE, FOUR, FIVE, SIX)

		val f2 = n.validate(THREE)
		assertEquals(THREE, f2)

		n.assertIsValid(ONE, TWO, THREE, SEVEN)
		n.assertIsNotValid(FOUR, FIVE, SIX)

		val f3 = n.validate()
		assertEquals(FOUR or FIVE or SIX, f3)

		n.assertIsValid(ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN)

	}

	@Test fun dependents() {
		n.addNode(EIGHT, dependencies = FOUR, dependents = FIVE) {}

		n.validate(FIVE)

		n.assertIsValid(FIVE, EIGHT, FOUR, TWO)

		n.invalidate(FIVE)
		n.assertIsValid(TWO, FOUR, EIGHT)
		n.assertIsNotValid(FIVE)
		n.invalidate(FOUR)
		n.assertIsValid(TWO)
		n.assertIsNotValid(FOUR, FIVE, EIGHT)

		n.validate(EIGHT)
		n.assertIsValid(TWO, FOUR, EIGHT)
		n.assertIsNotValid(FIVE)

		n.validate(FIVE)
		n.assertIsValid(TWO, FOUR, FIVE, EIGHT)

		n.invalidate(EIGHT)
		n.assertIsNotValid(FIVE, EIGHT)
		n.assertIsValid(TWO, FOUR)
	}

	@Test fun dependencyAssertion() {
		assertFailsWith(IllegalArgumentException::class) {
			n.addNode(EIGHT, NINE) {}
		}
	}

	@Test fun dependentAssertion() {
		assertFailsWith(IllegalArgumentException::class) {
			n.addNode(EIGHT, 0, NINE) {}
		}
	}

	@Test fun powerOfTwoAssertion() {
		assertFailsWith(IllegalArgumentException::class) {
			n.addNode(3) {}
		}
	}

	@Test fun dependenciesCanBeValidated() {
		val t = validationGraph(::toFlagString) {
			addNode(ONE) { validate(TWO) }
			addNode(TWO, 0, ONE) {}
		}
		t.validate()
	}

	@Test fun containsAllFlags() {
		assertTrue(0b0101 containsAllFlags 0b100)
		assertTrue(0b0101 containsAllFlags 0b101)
		assertFalse(0b0101 containsAllFlags 0b011)
		assertFalse(0b0101 containsAllFlags 0b10)
		assertTrue(0b0101 containsAllFlags 0b101)
		assertFalse(0b0101 containsAllFlags 0b111)
	}

	@Test fun containsAnyFlags() {
		assertTrue(0b0101 containsAnyFlags 0b100)
		assertFalse(0b0101 containsAnyFlags 0b10)
		assertTrue(0b0101 containsAnyFlags 0b110)
		assertTrue(0b0101 containsAnyFlags 0b101)
		assertTrue(0b0101 containsAnyFlags 0b111)
	}

	@Test fun removeNode() {
		n.invalidate()
		assertFalse(n.isValid(TWO))
		assertTrue(n.removeNode(TWO))
		assertTrue(n.isValid(TWO))
		assertEquals(0, n.validate(TWO))
	}

	private fun ValidationGraph.assertIsValid(vararg flags: Int) {
		for (flag in flags) {
			assertEquals(true, isValid(flag), "flag ${flag.toFlagString()} is not valid")
		}
	}

	private fun ValidationGraph.assertIsNotValid(vararg flags: Int) {
		for (flag in flags) {
			assertEquals(false, isValid(flag), "flag ${flag.toFlagString()} is valid")
		}
	}

	private fun toFlagString(v: Int): String {
		return log2(v.toDouble()).toInt().toString()
	}

}