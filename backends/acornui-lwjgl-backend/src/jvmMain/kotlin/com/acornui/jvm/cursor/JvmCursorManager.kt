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

package com.acornui.jvm.cursor

import com.acornui.async.then
import com.acornui.core.LifecycleBase
import com.acornui.core.asset.*
import com.acornui.core.cursor.Cursor
import com.acornui.core.cursor.CursorManagerBase
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.graphic.Texture
import com.acornui.jvm.io.JvmBufferUtil
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWImage

class JvmCursorManager(assets: AssetManager, private val window: Long) : CursorManagerBase() {

	val cursorsPath = "assets/uiskin/cursors/"

	init {
		// Cursors
		with (StandardCursors) {
			DEFAULT = JvmStandardCursor(window, GLFW.GLFW_ARROW_CURSOR)
			IBEAM = JvmStandardCursor(window, GLFW.GLFW_IBEAM_CURSOR)
			CROSSHAIR = JvmStandardCursor(window, GLFW.GLFW_CROSSHAIR_CURSOR)
			HAND = JvmStandardCursor(window, GLFW.GLFW_HAND_CURSOR)
			RESIZE_EW = JvmStandardCursor(window, GLFW.GLFW_HRESIZE_CURSOR)
			RESIZE_NS = JvmStandardCursor(window, GLFW.GLFW_VRESIZE_CURSOR)
			ALIAS = JvmTextureCursor(assets, window, cursorsPath + "Alias.png", 2, 2)
			ALL_SCROLL = JvmTextureCursor(assets, window, cursorsPath + "AllScroll.png", 12, 12)
			CELL = JvmTextureCursor(assets, window, cursorsPath + "Cell.png", 12, 12)
			COPY = JvmTextureCursor(assets, window, cursorsPath + "Copy.png", 2, 2)
			HELP = JvmTextureCursor(assets, window, cursorsPath + "Help.png", 2, 2)
			MOVE = JvmTextureCursor(assets, window, cursorsPath + "Move.png", 12, 12)
			NONE = HiddenCursor(window)
			NOT_ALLOWED = JvmTextureCursor(assets, window, cursorsPath + "NotAllowed.png", 12, 12)
			POINTER_WAIT = JvmTextureCursor(assets, window, cursorsPath + "PointerWait.png", 1, 3)
			RESIZE_NE = JvmTextureCursor(assets, window, cursorsPath + "ResizeNE.png", 13, 13)
			RESIZE_SE = JvmTextureCursor(assets, window, cursorsPath + "ResizeSE.png", 13, 13)
			WAIT = JvmTextureCursor(assets, window, cursorsPath + "Wait.png", 6, 2)
		}
	}
}

/**
 * Loads a texture atlas and pulls out the cursor region, sending the pixels to the OS.
 */
class JvmTextureCursor(
		assets: AssetManager,
		private val window: Long,
		texturePath: String,
		val hotX: Int,
		val hotY: Int
) : LifecycleBase(), Cursor {

	private var cursor: Long = -1L

	init {
		assets.load(texturePath, AssetType.TEXTURE).then {
			setTexture(it)
		}
	}

	private fun setTexture(texture: Texture) {
		val i = GLFWImage.create()
		i.width(texture.width)
		i.height(texture.height)
		i.pixels(JvmBufferUtil.wrap(texture.rgbData.bytes))
//		i.width = texture.width()
//		i.height = texture.height()
//		i.setPixels(JvmBufferUtil.wrap(texture.rgbData().bytes))

		if (cursor != -1L) {
			GLFW.glfwDestroyCursor(cursor)
		}
		cursor = GLFW.glfwCreateCursor(i, hotX, hotY)
		if (isActive) onActivated()
	}

	override fun onActivated() {
		if (cursor == -1L) return // Not ready
		GLFW.glfwSetCursor(window, cursor)
	}

	override fun onDeactivated() {
		if (cursor == -1L) return
		GLFW.glfwSetCursor(window, 0L) // TODO: this probably doesn't work... no NULL?
	}

	override fun dispose() {
		super.dispose()

		if (cursor != -1L) {
			GLFW.glfwDestroyCursor(cursor)
		}
	}
}

class JvmStandardCursor(
		private val window: Long,
		shape: Int
) : LifecycleBase(), Cursor {

	private val cursor: Long = GLFW.glfwCreateStandardCursor(shape)

	override fun onActivated() {
		GLFW.glfwSetCursor(window, cursor)
	}

	override fun onDeactivated() {
		GLFW.glfwSetCursor(window, 0L)
	}

	override fun dispose() {
		super.dispose()
		GLFW.glfwDestroyCursor(cursor)
	}
}

class HiddenCursor(val window: Long) : LifecycleBase(), Cursor {

	override fun onActivated() {
		GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN)
	}

	override fun onDeactivated() {
		GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
	}
}
