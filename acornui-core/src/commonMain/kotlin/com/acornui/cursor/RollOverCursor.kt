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

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.Disposable
import com.acornui.di.injectOptional
import com.acornui.input.interaction.MouseInteractionRo
import com.acornui.input.interaction.rollOut
import com.acornui.input.interaction.rollOver

/**
 * An attachment that changes the cursor on roll over.
 */
class RollOverCursor(
		private val target: UiComponentRo,
		var cursor: Cursor,
		var priority: Float = CursorPriority.ACTIVE) : Disposable {

	private val cursorManager = target.injectOptional(CursorManager)

	private var cursorRef: CursorReference? = null

	var enabled: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				if (value) {
					target.rollOver().add(::rollOverHandler)
					target.rollOut().add(::rollOutHandler)
				} else {
					cursorRef?.remove()
					cursorRef = null
					target.rollOver().remove(::rollOverHandler)
					target.rollOut().remove(::rollOutHandler)
				}
			}
		}

	init {
		enabled = true
	}

	private fun rollOverHandler(event: MouseInteractionRo) {
		cursorRef?.remove()
		cursorRef = cursorManager?.addCursor(cursor, priority)
	}

	private fun rollOutHandler(event: MouseInteractionRo) {
		cursorRef?.remove()
		cursorRef = null
	}

	override fun dispose() {
		cursorRef?.remove()
		cursorRef = null
		target.rollOver().remove(::rollOverHandler)
		target.rollOut().remove(::rollOutHandler)
	}

	companion object
}

fun UiComponentRo.clearCursor() {
	removeAttachment<RollOverCursor>(RollOverCursor)?.dispose()
}

fun UiComponentRo.cursor(cursor: Cursor?, priority: Float = CursorPriority.ACTIVE): RollOverCursor? {
	return if (cursor == null) {
		clearCursor()
		null
	} else createOrReuseAttachment(RollOverCursor) { RollOverCursor(this, cursor, priority) }.also { 
		it.cursor = cursor
		it.priority = priority
	}
}
