package com.acornui.graphic

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorTest {

	@BeforeTest fun before() {
	}

	@Test fun toHsl() {
		assertEquals(Hsl(0.0, 0.0, 0.0), Color.BLACK.toHsl())
		assertEquals(Hsl(0.0, 0.0, 1.0), Color.WHITE.toHsl())
		assertEquals(Hsl(0.0, 1.0, 0.5), Color.RED.toHsl())
		assertEquals(Hsl(120.0, 1.0, 0.5), Color.GREEN.toHsl())
		assertEquals(Hsl(240.0, 1.0, 0.5), Color.BLUE.toHsl())
		assertEquals(Hsl(60.0, 1.0, 0.5), Color.YELLOW.toHsl())
		assertEquals(Hsl(180.0, 1.0, 0.5), Color.CYAN.toHsl())
		assertEquals(Hsl(300.0, 1.0, 0.5), Color.MAGENTA.toHsl())
		assertEquals(Hsl(0.0, 0.0, 0.75), Color.LIGHT_GRAY.toHsl())
		assertEquals(Hsl(0.0, 0.0, 0.5), Color.GRAY.toHsl())
		assertEquals(Hsl(0.0, 1.0, 0.25), Color.MAROON.toHsl())
		assertEquals(Hsl(60.0, 1.0, 0.25), Color.OLIVE.toHsl())
	}

	@Test fun toHsv() {
		assertEquals(Hsv(0.0, 0.0, 0.0), Color.BLACK.toHsv())
		assertEquals(Hsv(0.0, 0.0, 1.0), Color.WHITE.toHsv())
		assertEquals(Hsv(0.0, 1.0, 1.0), Color.RED.toHsv())
		assertEquals(Hsv(120.0, 1.0, 1.0), Color.GREEN.toHsv())
		assertEquals(Hsv(240.0, 1.0, 1.0), Color.BLUE.toHsv())
		assertEquals(Hsv(60.0, 1.0, 1.0), Color.YELLOW.toHsv())
		assertEquals(Hsv(180.0, 1.0, 1.0), Color.CYAN.toHsv())
		assertEquals(Hsv(300.0, 1.0, 1.0), Color.MAGENTA.toHsv())
		assertEquals(Hsv(0.0, 0.0, 0.75), Color.LIGHT_GRAY.toHsv())
		assertEquals(Hsv(0.0, 0.0, 0.5), Color.GRAY.toHsv())
		assertEquals(Hsv(0.0, 1.0, 0.5), Color.MAROON.toHsv())
		assertEquals(Hsv(60.0, 1.0, 0.5), Color.OLIVE.toHsv())
	}

	@Test fun set888() {
		val c = Color.from888(14502206)
		assertEquals("dd493e", c.toRgbString())
	}

	@Test fun set8888() {
		val c = Color(4294967295L)
		assertEquals("ffffffff", c.toRgbaString())
	}

	@Test fun equalsTest() {
		assertEquals(Color(1.0, 0.2, 0.3, 0.4), Color(1.0, 0.2, 0.3, 0.4))
		assertEquals(Color(), Color())
	}

	@Test fun toColorOrNull() {
		assertEquals(null, "asdf".toColorOrNull())
		assertEquals(Color(1.0, 1.0, 1.0, 1.0), "FFFFFFFF".toColorOrNull())
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
	 * Color values support > 1.0 and < 0.0, but the hex value should be clamped.
	 */
	@Test fun toRgbStringOutOfRange() {
		assertEquals("ffffffff", Color(2.0, 3.0, 4.0, 5.0).toRgbaString())
		assertEquals("ffffff", Color(2.0, 3.0, 4.0, 5.0).toRgbString())
		assertEquals("00ff00ff", Color(0.0, 3.0, 0.0, 5.0).toRgbaString())
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
private data class ColorWrapper(val c: Color)

