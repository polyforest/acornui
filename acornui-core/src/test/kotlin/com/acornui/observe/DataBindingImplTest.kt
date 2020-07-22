package com.acornui.observe

import com.acornui.DisposableBase
import com.acornui.ManagedDisposable
import kotlin.test.Test
import kotlin.test.assertEquals

class DataBindingImplTest {

	@Test
	fun mirror() {
		val owner = object : DisposableBase() {}
		val dataBindingA = DataBindingImpl(TestData("foo", 3))
		val dataBindingB = DataBindingImpl(TestData("bar", 4))
		owner.mirror(dataBindingA, dataBindingB)
		assertEquals(TestData("foo", 3), dataBindingB.value)
		assertEquals(TestData("foo", 3), dataBindingA.value)
		dataBindingA.value = TestData("baz", 5)
		assertEquals(TestData("baz", 5), dataBindingB.value)
		assertEquals(TestData("baz", 5), dataBindingA.value)
	}
}

private data class TestData(val a: String, val b: Int)