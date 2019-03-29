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

package com.acornui.js.cursor

import com.acornui.core.LifecycleBase
import com.acornui.core.cursor.Cursor
import com.acornui.core.cursor.CursorManagerBase
import com.acornui.core.cursor.StandardCursors
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

class JsCursorManager(private val canvas: HTMLElement) : CursorManagerBase() {

	init {
		with (StandardCursors) {
			ALIAS = JsStandardCursor("alias", canvas)
			ALL_SCROLL = JsStandardCursor("all-scroll", canvas)
			CELL = JsStandardCursor("cell", canvas)
			COPY = JsStandardCursor("copy", canvas)
			CROSSHAIR = JsStandardCursor("crosshair", canvas)
			DEFAULT = JsStandardCursor("default", canvas)
			HAND = JsStandardCursor("pointer", canvas)
			HELP = JsStandardCursor("help", canvas)
			IBEAM = JsStandardCursor("text", canvas)
			MOVE = JsStandardCursor("move", canvas)
			NONE = JsStandardCursor("none", canvas)
			NOT_ALLOWED = JsStandardCursor("not-allowed", canvas)
			POINTER_WAIT = JsStandardCursor("progress", canvas)
			RESIZE_EW = JsStandardCursor("ew-resize", canvas)
			RESIZE_NS = JsStandardCursor("ns-resize", canvas)
			RESIZE_NE = JsStandardCursor("ne-resize", canvas)
			RESIZE_SE = JsStandardCursor("se-resize", canvas)
			WAIT = JsStandardCursor("wait", canvas)
		}
	}
}

/**
 * Loads a texture atlas and pulls out the cursor region, sending the pixels to the OS.
 *
 */
// TODO: IE doesn't support hot spot, and only the .cur format...
class JsTextureCursor(
		val texturePath: String,
		val hotX: Int,
		val hotY: Int,
		val canvas: HTMLCanvasElement) : LifecycleBase(), Cursor {

	init {
	}

	override fun onActivated() {
		canvas.style.cursor = "url(\"$texturePath\") $hotX $hotY, default"
	}

	override fun onDeactivated() {
		canvas.style.cursor = "auto"
	}
}

/**
 * Loads a texture atlas and pulls out the cursor region, sending the pixels to the OS.
 */
class JsStandardCursor(
		val identifier: String,
		val canvas: HTMLElement) : LifecycleBase(), Cursor {

	init {
	}

	override fun onActivated() {
		canvas.style.cursor = identifier
	}

	override fun onDeactivated() {
		canvas.style.cursor = "auto"
	}
}