package com.acornui.core.nav

import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import org.junit.Before
import org.junit.Test
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.test.assertEquals

class NavNodeTest {
	@Before fun before() {
		encodeUriComponent2 = {
			str ->
			URLEncoder.encode(str, "UTF-8")
		}
		decodeUriComponent2 = {
			str ->
			URLDecoder.decode(str, "UTF-8")
		}
	}

	@Test fun fromStr() {
		val n = NavNode.fromStr("foo?val1=1&val2=2")
		assertEquals("foo", n.name)
		assertEquals("1", n.params["val1"])
		assertEquals("2", n.params["val2"])
		assertEquals(2, n.params.size)
	}
}