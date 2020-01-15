/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.texturepacker

import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.gl.core.TexturePixelFormat
import com.acornui.gl.core.TexturePixelType
import com.acornui.graphic.TextureAtlasData
import com.acornui.serialization.jsonParse
import com.acornui.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @author nbilyk
 */
class AcornTexturePackerTest {

	@Test fun settings() {
		// language=JSON
		val settings = jsonParse(TexturePackerSettingsData.serializer(),"""
		{
			"alphaThreshold": 3,
			"filterMag": "LINEAR",
			"filterMin": "NEAREST_MIPMAP_LINEAR",
			"pixelType": "UNSIGNED_SHORT_4_4_4_4",
			"pixelFormat": "LUMINANCE",
			"compressionQuality": 0.8,
			"compressionExtension": "jpg",
			"maxDirectoryDepth": 3,
  			"stripWhitespace": false,
			"algorithmSettings": {
				"allowRotation": false,
				"paddingX": 3,
				"paddingY": 4,
				"pageMaxHeight": 512,
				"pageMaxWidth": 256,
				"addWhitePixel": true
			}
		}
		""")

		assertEquals(TexturePackerSettingsData(
				alphaThreshold = 3f,
				filterMag = TextureMagFilter.LINEAR,
				filterMin = TextureMinFilter.NEAREST_MIPMAP_LINEAR,
				pixelType = TexturePixelType.UNSIGNED_SHORT_4_4_4_4,
				pixelFormat = TexturePixelFormat.LUMINANCE,
				compressionQuality = 0.8f,
				compressionExtension = "jpg",
				maxDirectoryDepth = 3,
				stripWhitespace = false,
				algorithmSettings = PackerAlgorithmSettingsData(
						allowRotation = false,
						pageMaxHeight = 512,
						pageMaxWidth = 256,
						paddingX = 3,
						paddingY = 4,
						addWhitePixel = true
				)

		), settings)
	}

	@Test fun testPackBasic() = runTest {
		val packTest1 = File("build/processedResources/jvm/test/packTest1")
		val out = File("build/processedResources/jvm/test/packTest1_out")
		out.deleteRecursively()

		packAssets(packTest1, out)
		assertTrue(out.resolve("packTest1.json").exists())
		assertTrue(out.resolve("packTest10.png").exists())
		assertTrue(out.resolve("packTest11.png").exists())

		val atlasData = jsonParse(TextureAtlasData.serializer(), out.resolve("packTest1.json").readText())
		assertNotNull(atlasData.findRegion("part11.png"))
	}

}
