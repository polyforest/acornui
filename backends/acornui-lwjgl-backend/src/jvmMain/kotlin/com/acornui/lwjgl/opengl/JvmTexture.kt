/*
 * Copyright 2019 Poly Forest, LLC
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

package com.acornui.lwjgl.opengl

import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.GlTextureBase
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture
import com.acornui.io.*
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * @author nbilyk
 */
class JvmTexture(gl: Gl20,
				 glState: GlState,
				 private val _rgbData: RgbData
) : GlTextureBase(gl, glState) {

	var bytes: ByteBuffer? = JvmBufferUtil.wrap(_rgbData.bytes)

	override val widthPixels: Int
		get() = _rgbData.width

	override val heightPixels: Int
		get() = _rgbData.height

	override val rgbData: RgbData
		get() = _rgbData
}

/**
 * Creates an http request, processing the results as a [Texture].
 */
suspend fun loadTexture(gl: Gl20, glState: GlState, requestData: UrlRequestData, progressReporter: ProgressReporter = GlobalProgressReporter, initialTimeEstimate: Duration = Bandwidth.downBpsInv.seconds * 100_000): Texture {
	return JvmTexture(gl, glState, loadRgbData(requestData, progressReporter, initialTimeEstimate))
}
