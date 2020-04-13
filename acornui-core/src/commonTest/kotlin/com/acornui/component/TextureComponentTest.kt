/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.component

import com.acornui.asset.Loaders
import com.acornui.di.context
import com.acornui.graphic.Texture
import com.acornui.graphic.exit
import com.acornui.headless.MockLoader
import com.acornui.headless.MockTexture
import com.acornui.headless.headlessApplication
import com.acornui.io.UrlRequestData
import com.acornui.io.await
import com.acornui.io.progressReporter
import com.acornui.runMainTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.seconds

class TextureComponentTest {

	@Test fun explicitTextureRefCount() = runMainTest {
		headlessApplication {
			val mockT = MockTexture()
			val texC = textureC {
				texture(mockT)
				assertEquals(mockT, explicitTexture)
				assertEquals(mockT, texture)
				assertEquals(0, mockT.refCount) // Not added to stage yet
			}
			+texC
			assertEquals(1, mockT.refCount)
			-texC
			assertEquals(0, mockT.refCount)
			+texC
			val mockT2 = MockTexture()
			texC.texture(mockT2)
			assertEquals(mockT2, texC.explicitTexture)
			assertEquals(mockT2, texC.texture)
			assertEquals(0, mockT.refCount) // Old texture should be decremented
			assertEquals(1, mockT2.refCount) // New texture should be incremented

			texC.dispose()
			assertEquals(0, mockT2.refCount) // Set texture should be decremented if used on dispose

			exit()
		}
	}

	@Test fun pathRefCount() = runMainTest {
		headlessApplication {
			context {
				var constructedTextures = 0
				dependencies += Loaders.textureLoader to MockLoader(0.2.seconds) {
					constructedTextures++
					delay(0.2.seconds)
					MockTextureWithRequest(it, MockTexture())
				}
				launch {
					val texC = +textureC()
					texC.texture("path0")
					texC.join()
					assertEquals(1, constructedTextures)
					assertEquals(1, texC.texture?.refCount)
					texC.texture("path0")
					texC.join()
					assertEquals(1, texC.texture?.refCount)
					assertEquals(1, constructedTextures)

					texC.clear()
					texC.texture("path0")
					texC.join()
					assertEquals(1, texC.texture?.refCount)
					assertEquals(1, constructedTextures)

					val previousTexture = texC.texture
					texC.texture("path1")
					texC.join()
					assertEquals(1, texC.texture?.refCount)
					assertEquals(0, previousTexture?.refCount)
					assertEquals(2, constructedTextures)
					val previousTexture2 = texC.texture
					texC.texture("path2")
					assertEquals(0, previousTexture?.refCount)
					assertEquals(0, previousTexture2?.refCount)

					texC.join()
					assertEquals(3, constructedTextures)
					assertEquals(1, texC.texture?.refCount)

					exit()
				}
			}
		}
	}

	@Test fun pathReusesTextures() = runMainTest {
		headlessApplication {
			context {
				var totalTextures = 0
				dependencies += Loaders.textureLoader to MockLoader {
					delay(100L)
					++totalTextures; MockTexture()
				}

				val texC = textureC()
				launch {
					texC.texture("path0")
					progressReporter.await()
					assertEquals(1, totalTextures)
					texC.texture("path0")
					progressReporter.await()
					assertEquals(1, totalTextures)
					exit()
				}
			}
		}
	}
}

private class MockTextureWithRequest(val request: UrlRequestData, private val mockTexture: MockTexture) : Texture by mockTexture