package com.acornui.nav

import com.acornui.ChildRo
import com.acornui.ParentRo
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import kotlin.test.Test
import kotlin.test.assertEquals

class NavBindingTest {

	private val context = ContextImpl(null, dependencies = listOf(NavigationManager to NavigationManagerImpl(ContextImpl())))

	private val navMan: NavigationManager = context.inject(NavigationManager)

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
		return MockNavBindable(context, depth)
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

private class MockNavBindable(owner: Context, private val depth: Int) : ContextImpl(owner), ParentRo<NavBindable>, NavBindable {

	override val parent: ParentRo<ChildRo>?
		get() {
			return if (depth == 0) null else MockNavBindable(this, depth - 1)
		}

	override val children: List<NavBindable>
		get() = throw UnsupportedOperationException()

}