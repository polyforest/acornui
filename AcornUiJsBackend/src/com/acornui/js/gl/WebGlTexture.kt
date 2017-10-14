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

package com.acornui.js.gl

import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.GlTextureBase
import org.w3c.dom.HTMLImageElement
import kotlin.browser.document

// todo: https://stackoverflow.com/questions/13626606/read-pixels-from-a-webgl-texture
/**
 * @author nbilyk
 */
class WebGlTexture(
		gl: Gl20,
		glState: GlState
) : GlTextureBase(gl, glState) {

	val image: HTMLImageElement = document.createElement("img") as HTMLImageElement

	override val width: Int
		get() {
			return image.naturalWidth
		}

	override val height: Int
		get() {
			return image.naturalHeight
		}
}