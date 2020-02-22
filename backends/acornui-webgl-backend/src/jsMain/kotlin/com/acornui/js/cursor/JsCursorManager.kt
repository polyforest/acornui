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

package com.acornui.js.cursor

import com.acornui.LifecycleBase
import com.acornui.cursor.Cursor
import com.acornui.cursor.CursorManagerBase
import com.acornui.cursor.StandardCursor
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

class JsCursorManager(private val canvas: HTMLElement) : CursorManagerBase() {

	private val standardCursors = HashMap<StandardCursor, Cursor>()

	override fun getStandardCursor(cursor: StandardCursor): Cursor {
		return standardCursors.getOrPut(cursor) {
			when (cursor) {
				StandardCursor.ALIAS -> JsStandardCursor("alias", canvas)
				StandardCursor.ALL_SCROLL -> JsStandardCursor("all-scroll", canvas)
				StandardCursor.CELL -> JsStandardCursor("cell", canvas)
				StandardCursor.COPY -> JsStandardCursor("copy", canvas)
				StandardCursor.CROSSHAIR -> JsStandardCursor("crosshair", canvas)
				StandardCursor.DEFAULT -> JsStandardCursor("default", canvas)
				StandardCursor.HAND -> JsStandardCursor("pointer", canvas)
				StandardCursor.HELP -> JsStandardCursor("help", canvas)
				StandardCursor.IBEAM -> JsStandardCursor("text", canvas)
				StandardCursor.MOVE -> JsStandardCursor("move", canvas)
				StandardCursor.NONE -> JsStandardCursor("none", canvas)
				StandardCursor.NOT_ALLOWED -> JsStandardCursor("not-allowed", canvas)
				StandardCursor.POINTER_WAIT -> JsStandardCursor("progress", canvas)
				StandardCursor.RESIZE_EW -> JsStandardCursor("ew-resize", canvas)
				StandardCursor.RESIZE_NS -> JsStandardCursor("ns-resize", canvas)
				StandardCursor.RESIZE_NE -> JsStandardCursor("ne-resize", canvas)
				StandardCursor.RESIZE_SE -> JsStandardCursor("se-resize", canvas)
				StandardCursor.WAIT -> JsStandardCursor("wait", canvas)
			}
		}
	}
}

/**
 * Loads a texture atlas and pulls out the cursor region, sending the pixels to the OS.
 *
 */
// TODO: IE doesn't support hot spot, and only the .cur format...
class JsTextureCursor(
		private val texturePath: String,
		private val hotX: Int,
		private val hotY: Int,
		private val canvas: HTMLCanvasElement
) : LifecycleBase(), Cursor {

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
		private val identifier: String,
		private val canvas: HTMLElement) : LifecycleBase(), Cursor {

	override fun onActivated() {
		canvas.style.cursor = identifier
	}

	override fun onDeactivated() {
		canvas.style.cursor = "auto"
	}
}
