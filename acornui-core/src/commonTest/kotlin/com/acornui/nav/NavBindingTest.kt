package com.acornui.nav

import com.acornui.ChildRo
import com.acornui.ParentRo
import com.acornui.di.Injector
import com.acornui.di.InjectorImpl
import kotlin.test.Test
import kotlin.test.assertEquals

class NavBindingTest {

	private val mockInjector = InjectorImpl(null, listOf())

	private val navMan: NavigationManager
		get() = mockInjector.inject(NavigationManager)

	@Test fun pathStr() {
		val b = NavBindingImpl(mockBindable(0), "")

		b.navigate("../test/foo/bar")
		val p = navMan.path()
		assertEquals(3, p.size)
		assertEquals("test", p[0].name)
		assertEquals("foo", p[1].name)
		assertEquals("bar", p[2].name)
	}

	private fun mockBindable(depth: Int): NavBindable {
		return MockNavBindable(mockInjector, depth)
	}

	@Test fun pathStrWithParams() {
		val b = NavBindingImpl(mockBindable(0), "")
		b.navigate("/test?a=0&b=1&c=2/foo/bar?d=3&e=4")

		val p = navMan.path()
		assertEquals(3, p.size)
		assertEquals("test", p[0].name)
		assertEquals("0", p[0].params["a"])
		assertEquals("1", p[0].params["b"])
		assertEquals("2", p[0].params["c"])
		assertEquals("foo", p[1].name)
		assertEquals("bar", p[2].name)
		assertEquals("3", p[2].params["d"])
		assertEquals("4", p[2].params["e"])
		assertEquals(2, p[2].params.size)

	}

	@Test fun dotDotRelative() {
		val b = NavBindingImpl(mockBindable(0), "")
		b.navigate("/test?a=0&b=1&c=2/foo/bar?d=3&e=4")

		b.navigate("../..")

		assertEquals("test?a=0&b=1&c=2", navMan.pathToString())
		b.navigate("/foo")
		assertEquals("foo", navMan.pathToString())

	}

}

private class MockNavBindable(override val injector: Injector, private val depth: Int) : ParentRo<NavBindable>,  NavBindable {

	override val parent: ParentRo<out ChildRo>?
		get() {
			if (depth == 0) return null else return MockNavBindable(injector, depth - 1)
		}

	override val children: List<NavBindable>
		get() = throw UnsupportedOperationException()

}