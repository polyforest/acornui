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

package com.acornui.filter

import com.acornui.di.Context
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.ShaderProgram
import com.acornui.gl.core.useProgram

/**
 * A filter that sets a custom shader.
 */
class ShaderFilter(
		owner: Context,

		/**
		 * The shader to apply to the component.
		 */
		var shader: ShaderProgram,

		/**
		 * Before rendering, this will be called, providing a time to set uniforms.
		 */
		var configure: (gl: Gl20, shader: ShaderProgram) -> Unit = { _, _ -> }
) : RenderFilterBase(owner) {

	override fun render(inner: () -> Unit) {
		gl.useProgram(shader.program) {
			configure(gl, shader)
			inner()
		}
	}

}
