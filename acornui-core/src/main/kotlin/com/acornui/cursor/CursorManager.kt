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

package com.acornui.cursor

import com.acornui.component.UiComponent

/**
 * A list of standard cursors supported in css.
 */
object StandardCursor {
	const val ALIAS = "alias"
	const val ALL_SCROLL = "all-scroll"
	const val CELL = "cell"
	const val COPY = "copy"
	const val CROSSHAIR = "crosshair"
	const val DEFAULT = "default"
	const val POINTER = "pointer"
	const val HELP = "help"
	const val TEXT = "text"
	const val MOVE = "move"
	const val NONE = "none"
	const val NOT_ALLOWED = "not-allowed"
	const val PROGRESS = "progress"
	const val EW_RESIZE = "ew-resize"
	const val NS_RESIZE = "ns-resize"
	const val NE_RESIZE = "ne-resize"
	const val SE_RESIZE = "se-resize"
	const val WAIT = "wait"
}

fun UiComponent.cursor(cursor: String) {
	dom.style.cursor = cursor
}