/*
* Derived from LibGDX by Nicholas Bilyk
* https://github.com/libgdx
* Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

package com.acornui.core.graphics

import com.acornui.collection.copy
import com.acornui.gl.core.Gl20

open class BlendMode(
		val source: Int,
		val sourcePma: Int,
		val dest: Int,

		/**
		 * The name of the blend mode, used in debugging and serialization.
		 */
		val name: String
) {

	companion object {
		val NONE = BlendMode(0, 0, 0, "none")
		val NORMAL = BlendMode(Gl20.SRC_ALPHA, Gl20.ONE, Gl20.ONE_MINUS_SRC_ALPHA, "normal")
		val ADDITIVE = BlendMode(Gl20.SRC_ALPHA, Gl20.ONE, Gl20.ONE, "additive")
		val MULTIPLY = BlendMode(Gl20.DST_COLOR, Gl20.DST_COLOR, Gl20.ONE_MINUS_SRC_ALPHA, "multiply")
		val INVERTED = BlendMode(Gl20.ONE_MINUS_DST_ALPHA, Gl20.ONE_MINUS_DST_ALPHA, Gl20.ONE_MINUS_SRC_ALPHA, "inverted")
		val SCREEN = BlendMode(Gl20.ONE, Gl20.ONE, Gl20.ONE_MINUS_SRC_COLOR, "screen")

		private val registry = HashMap<String, BlendMode>()

		fun register(vararg blendModes: BlendMode) {
			for (blendMode in blendModes) {
				registry[blendMode.name] = blendMode
			}
		}

		fun getRegistered(): List<BlendMode> {
			return registry.values.copy()
		}

		fun fromStr(name: String?): BlendMode? = registry[name]

		init {
			register(NONE, NORMAL, ADDITIVE, MULTIPLY, INVERTED, SCREEN)
		}
	}

	open fun applyBlending(gl: Gl20, premultipliedAlpha: Boolean) {
		gl.blendFunc(if (premultipliedAlpha) sourcePma else source, dest)
	}
}
