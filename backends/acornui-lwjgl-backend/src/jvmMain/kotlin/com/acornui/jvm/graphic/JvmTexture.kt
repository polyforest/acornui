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

package com.acornui.jvm.graphic

import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.GlTextureBase
import com.acornui.core.graphic.*
import com.acornui.io.JvmBufferUtil
import java.nio.ByteBuffer

/**
 * @author nbilyk
 */
class JvmTexture(gl: Gl20,
				 glState: GlState,
				 private val _rgbData: RgbData
) : GlTextureBase(gl, glState) {

	var bytes: ByteBuffer? = JvmBufferUtil.wrap(_rgbData.bytes)

	override val width: Int
		get() = _rgbData.width

	override val height: Int
		get() = _rgbData.height

	override val rgbData: RgbData
		get() = _rgbData
}
