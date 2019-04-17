package com.acornui.component

import com.acornui.assertionsEnabled
import com.acornui.string.toRadix
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

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

	@Before fun before() {
		assertionsEnabled = true
		n = validationGraph {
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
		assertFailsWith(Exception::class) {
			n.addNode(EIGHT, NINE) {}
		}
	}

	@Test fun powerOfTwoAssertion() {
		assertFailsWith(IllegalArgumentException::class) {
			n.addNode(3) {}
		}
	}

	@Test fun textComponentsBug() {
		val validation = validationGraph {
			ValidationFlags.apply {
				addNode(STYLES) {}
				addNode(PROPERTIES, STYLES) {}
				addNode(SIZE_CONSTRAINTS, PROPERTIES) {}
				addNode(LAYOUT, PROPERTIES or SIZE_CONSTRAINTS) {}
				addNode(TRANSFORM) {}
				addNode(CONCATENATED_TRANSFORM, TRANSFORM) {}
				addNode(INTERACTIVITY_MODE) {}
				addNode(HIERARCHY_ASCENDING, PROPERTIES) {}
				addNode(HIERARCHY_DESCENDING, PROPERTIES) {}
			}
		}

		validation.addNode(1 shl 16, 0, ValidationFlags.STYLES or ValidationFlags.LAYOUT) {}

		validation.validate()

		validation.invalidate(1 shl 16)
		assertFalse(validation.isValid(ValidationFlags.STYLES))
	}

	@Test fun dependenciesCanBeValidated() {
		val t = validationGraph {
			addNode(ONE) { validate(TWO) }
			addNode(TWO, 0, ONE) {}
		}
		t.validate()
	}

	private fun ValidationGraph.assertIsValid(vararg flags: Int) {
		for (flag in flags) {
			assertEquals(true, isValid(flag), "flag ${flag.toRadix(2)} is not valid")
		}
	}

	private fun ValidationGraph.assertIsNotValid(vararg flags: Int) {
		for (flag in flags) {
			assertEquals(false, isValid(flag), "flag ${flag.toRadix(2)} is valid")
		}
	}

}