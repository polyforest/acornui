package com.acornui.graphic

import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.BeforeTest

class ColorTest {

	@BeforeTest fun before() {
	}

	@Test fun toHsl() {
		assertEquals(Hsl(0f, 0f, 0f), Color.BLACK.toHsl(Hsl()))
		assertEquals(Hsl(0f, 0f, 1f), Color.WHITE.toHsl(Hsl()))
		assertEquals(Hsl(0f, 1f, 0.5f), Color.RED.toHsl(Hsl()))
		assertEquals(Hsl(120f, 1f, 0.5f), Color.GREEN.toHsl(Hsl()))
		assertEquals(Hsl(240f, 1f, 0.5f), Color.BLUE.toHsl(Hsl()))
		assertEquals(Hsl(60f, 1f, 0.5f), Color.YELLOW.toHsl(Hsl()))
		assertEquals(Hsl(180f, 1f, 0.5f), Color.CYAN.toHsl(Hsl()))
		assertEquals(Hsl(300f, 1f, 0.5f), Color.MAGENTA.toHsl(Hsl()))
		assertEquals(Hsl(0f, 0f, 0.75f), Color.LIGHT_GRAY.toHsl(Hsl()))
		assertEquals(Hsl(0f, 0f, 0.5f), Color.GRAY.toHsl(Hsl()))
		assertEquals(Hsl(0f, 1f, 0.25f), Color.MAROON.toHsl(Hsl()))
		assertEquals(Hsl(60f, 1f, 0.25f), Color.OLIVE.toHsl(Hsl()))
	}

	@Test fun toHsv() {
		assertEquals(Hsv(0f, 0f, 0f), Color.BLACK.toHsv(Hsv()))
		assertEquals(Hsv(0f, 0f, 1f), Color.WHITE.toHsv(Hsv()))
		assertEquals(Hsv(0f, 1f, 1f), Color.RED.toHsv(Hsv()))
		assertEquals(Hsv(120f, 1f, 1f), Color.GREEN.toHsv(Hsv()))
		assertEquals(Hsv(240f, 1f, 1f), Color.BLUE.toHsv(Hsv()))
		assertEquals(Hsv(60f, 1f, 1f), Color.YELLOW.toHsv(Hsv()))
		assertEquals(Hsv(180f, 1f, 1f), Color.CYAN.toHsv(Hsv()))
		assertEquals(Hsv(300f, 1f, 1f), Color.MAGENTA.toHsv(Hsv()))
		assertEquals(Hsv(0f, 0f, 0.75f), Color.LIGHT_GRAY.toHsv(Hsv()))
		assertEquals(Hsv(0f, 0f, 0.5f), Color.GRAY.toHsv(Hsv()))
		assertEquals(Hsv(0f, 1f, 0.5f), Color.MAROON.toHsv(Hsv()))
		assertEquals(Hsv(60f, 1f, 0.5f), Color.OLIVE.toHsv(Hsv()))
	}

	@Test fun set888() {
		val c = Color()
		c.set888(14502206)
		assertEquals("dd493e", c.toRgbString())
	}

	@Test fun set8888() {
		val c = Color()
		c.set8888(4294967295L)
		assertEquals("ffffffff", c.toRgbaString())
	}

	@Test fun equals() {
		assertEquals(Color(1f, 0.2f, 0.3f, 0.4f), Color(1f, 0.2f, 0.3f, 0.4f))
		assertEquals(Color(), Color())
	}
}

class HSLTest {
	@Test fun toRgb() {
		assertEquals(Color.BLACK, Hsl(0f, 0f, 0f).toRgb(Color()))
		assertEquals(Color.WHITE, Hsl(0f, 0f, 1f).toRgb(Color()))
		assertEquals(Color.RED, Hsl(0f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.GREEN, Hsl(120f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.BLUE, Hsl(240f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.YELLOW, Hsl(60f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.CYAN, Hsl(180f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.MAGENTA, Hsl(300f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.LIGHT_GRAY, Hsl(0f, 0f, 0.75f).toRgb(Color()))
		assertEquals(Color.GRAY, Hsl(0f, 0f, 0.5f).toRgb(Color()))
		assertEquals(Color.MAROON, Hsl(0f, 1f, 0.25f).toRgb(Color()))
		assertEquals(Color.OLIVE, Hsl(60f, 1f, 0.25f).toRgb(Color()))
	}
}

class HSVTest {
	@Test fun toRgb() {
		assertEquals(Color.BLACK, Hsv(0f, 0f, 0f).toRgb(Color()))
		assertEquals(Color.WHITE, Hsv(0f, 0f, 1f).toRgb(Color()))
		assertEquals(Color.RED, Hsv(0f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.GREEN, Hsv(120f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.BLUE, Hsv(240f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.YELLOW, Hsv(60f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.CYAN, Hsv(180f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.MAGENTA, Hsv(300f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.LIGHT_GRAY, Hsv(0f, 0f, 0.75f).toRgb(Color()))
		assertEquals(Color.GRAY, Hsv(0f, 0f, 0.5f).toRgb(Color()))
		assertEquals(Color.MAROON, Hsv(0f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.OLIVE, Hsv(60f, 1f, 0.5f).toRgb(Color()))
	}
}