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
import com.acornui.gl.core.GlTextureBase
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture
import com.acornui.io.*
import java.nio.ByteBuffer
import kotlin.time.Duration

/**
 * A
 * @author nbilyk
 */
class JvmTexture(gl: Gl20,
				 override val rgbData: RgbData,
				 displayName: String? = null
) : GlTextureBase(gl, displayName) {

	var bytes: ByteBuffer? = JvmBufferUtil.wrap(rgbData.bytes)

	override val widthPixels: Int
		get() = rgbData.width

	override val heightPixels: Int
		get() = rgbData.height

}

/**
 * Creates an http request, processing the results as a [Texture].
 */
suspend fun loadTexture(gl: Gl20, requestData: UrlRequestData, progressReporter: ProgressReporter = GlobalProgressReporter, initialTimeEstimate: Duration, connectTimeout: Duration): Texture {
	return JvmTexture(gl, loadRgbData(requestData, progressReporter, initialTimeEstimate, connectTimeout), requestData.urlStr)
}
