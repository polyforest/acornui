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

package com.acornui.jvm.opengl

import com.acornui.gl.core.WrappedGl20
import org.lwjgl.opengl.ARBImaging.GL_TABLE_TOO_LARGE
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION

/**
 * @author nbilyk
 */
class JvmGl20Debug : WrappedGl20(LwjglGl20(), {}, {
	val errorCode = GL11.glGetError()

	if (errorCode != GL11.GL_NO_ERROR) {
		throw Exception("GL ERROR: code: $errorCode ${getErrorString(errorCode)}")
	}
}
)

/**
 * Translates an OpenGL error code to a String describing the error.
 *
 * @param errorCode the error code, as returned by {@link GL11#glGetError GetError}.
 *
 * @return the error description
 */
private fun getErrorString(errorCode: Int): String {
	return when (errorCode) {
		GL_NO_ERROR -> "No error"
		GL_INVALID_ENUM -> "Enum argument out of range"
		GL_INVALID_VALUE -> "Numeric argument out of range"
		GL_INVALID_OPERATION -> "Operation illegal in current state"
		GL_STACK_OVERFLOW -> "Command would cause a stack overflow"
		GL_STACK_UNDERFLOW -> "Command would cause a stack underflow"
		GL_OUT_OF_MEMORY -> "Not enough memory left to execute command"
		GL_INVALID_FRAMEBUFFER_OPERATION -> "Framebuffer object is not complete"
		GL_TABLE_TOO_LARGE -> "The specified table is too large"
		else -> "Unknown error: $errorCode"
	}
}
