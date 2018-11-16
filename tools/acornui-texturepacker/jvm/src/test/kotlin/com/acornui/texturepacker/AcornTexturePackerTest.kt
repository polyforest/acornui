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

// TODO - MP: Can't find assets directory, need to fix, commenting out for now.
//package com.acornui.texturepacker
//
//import com.acornui.async.launch
//import com.acornui.core.asset.AssetManager
//import com.acornui.core.di.inject
//import com.acornui.gl.core.TextureMagFilter
//import com.acornui.gl.core.TextureMinFilter
//import com.acornui.gl.core.TexturePixelFormat
//import com.acornui.gl.core.TexturePixelType
//import com.acornui.core.io.file.Files
//import com.acornui.jvm.JvmHeadlessApplication
//import com.acornui.serialization.JsonSerializer
//import com.acornui.texturepacker.jvm.writer.JvmTextureAtlasWriter
//import kotlin.test.Test
//import java.io.File
//import kotlin.test.assertEquals
//
///**
// * @author nbilyk
// */
//class AcornTexturePackerTest {
//
//	@Test fun settings() {
//		val json = JsonSerializer
//		// language=JSON
//		val settings = json.read("""
//		{
//			"alphaThreshold": 3,
//			"filterMag": "LINEAR",
//			"filterMin": "NEAREST_MIPMAP_LINEAR",
//			"pixelType": "UNSIGNED_SHORT_4_4_4_4",
//			"pixelFormat": "LUMINANCE",
//			"compressionQuality": 0.8,
//			"compressionExtension": "jpg",
//			"maxDirectoryDepth": 3,
//  			"stripWhitespace": false,
//			"algorithmSettings": {
//				"allowRotation": false,
//				"paddingX": 3,
//				"paddingY": 4,
//				"pageMaxHeight": 512,
//				"pageMaxWidth": 256,
//				"addWhitePixel": true
//			}
//		}
//		""", TexturePackerSettingsSerializer)
//
//		assertEquals(TexturePackerSettingsData(
//				alphaThreshold = 3f,
//				filterMag = TextureMagFilter.LINEAR,
//				filterMin = TextureMinFilter.NEAREST_MIPMAP_LINEAR,
//				pixelType = TexturePixelType.UNSIGNED_SHORT_4_4_4_4,
//				pixelFormat = TexturePixelFormat.LUMINANCE,
//				compressionQuality = 0.8f,
//				compressionExtension = "jpg",
//				maxDirectoryDepth = 3,
//				stripWhitespace = false,
//				algorithmSettings = PackerAlgorithmSettingsData(
//						allowRotation = false,
//						pageMaxHeight = 512,
//						pageMaxWidth = 256,
//						paddingX = 3,
//						paddingY = 4,
//						addWhitePixel = true
//				)
//
//		), settings)
//	}
//
//	@Test fun testPackBasic() {
//		File("../build/test/assets").deleteRecursively()
//		JvmHeadlessApplication().start {
//			val dir = inject(Files).getDir("assets/packTest1") ?: throw Exception("Missing assets/packTest1 folder")
//			launch {
//				val packedData = AcornTexturePacker(inject(AssetManager)).pack(dir)
//				JvmTextureAtlasWriter().writeAtlas("packTest1.json", "packTest{0}", packedData, File("../build/test/assets"))
//			}
//		}
//
//	}
//
//}
