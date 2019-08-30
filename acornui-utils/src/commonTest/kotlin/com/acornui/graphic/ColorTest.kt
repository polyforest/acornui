package com.acornui.graphic

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

	@Test fun equalsTest() {
		assertEquals(Color(1f, 0.2f, 0.3f, 0.4f), Color(1f, 0.2f, 0.3f, 0.4f))
		assertEquals(Color(), Color())
	}

	@Test fun toColorOrNull() {
		assertEquals(null, "asdf".toColorOrNull())
		assertEquals(Color(1f, 1f, 1f, 1f), "FFFFFFFF".toColorOrNull())
		assertEquals(Color(0x334455FF), "334455".toColorOrNull())
		assertEquals(Color(0x334455FF), "0x334455".toColorOrNull())
		assertEquals(Color(0x334455FF), "#334455".toColorOrNull())
		assertEquals(Color(0x33445533), "#33445533".toColorOrNull())
	}

	@Test fun toRgbString() {
		assertEquals("ffffffff", Color.WHITE.toRgbaString())
		assertEquals("ffffff", Color.WHITE.toRgbString())
		assertEquals("0000ffff", Color.BLUE.toRgbaString())
		assertEquals("0000ff", Color.BLUE.toRgbString())
		assertEquals("ff0000ff", Color.RED.toRgbaString())
		assertEquals("ff0000", Color.RED.toRgbString())
		assertEquals("00ff00ff", Color.GREEN.toRgbaString())
		assertEquals("00ff00", Color.GREEN.toRgbString())
		assertEquals("12345678", Color(0x12345678).toRgbaString())
		assertEquals("123456", Color(0x12345678).toRgbString())
	}

	/**
	 * Color values support > 1f and < 0f, but the hex value should be clamped.
	 */
	@Test fun toRgbStringOutOfRange() {
		assertEquals("ffffffff", Color(2f, 3f, 4f, 5f).toRgbaString())
		assertEquals("ffffff", Color(2f, 3f, 4f, 5f).toRgbString())
		assertEquals("00ff00ff", Color(0f, 3f, 0f, 5f).toRgbaString())
	}

	@Test fun serialize() {
		val c = Color(0x4c669933)
		val json = jsonStringify(Color.serializer(), c)
		assertEquals("\"#4c669933\"", json)
		assertEquals(c, jsonParse(Color.serializer(), json))

		val cW = ColorWrapper(Color.RED)
		val json2 = jsonStringify(ColorWrapper.serializer(), cW)
		assertEquals(cW, jsonParse(ColorWrapper.serializer(), json2))
	}
}

@Serializable
private data class ColorWrapper(val c: ColorRo)

