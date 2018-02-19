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

package com.acornui.core.cursor

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.core.Disposable
import com.acornui.core.di.inject
import com.acornui.core.input.interaction.MouseInteraction
import com.acornui.core.input.interaction.rollOut
import com.acornui.core.input.interaction.rollOver

/**
 * An attachment that changes the cursor on roll over.
 */
class RollOverCursor(
		private val target: UiComponentRo,
		private val cursor: Cursor,
		private val priority: Float = CursorPriority.ACTIVE) : Disposable {

	private val cursorManager = target.inject(CursorManager)

	private var cursorRef: CursorReference? = null

	private val rollOverHandler = {
		event: MouseInteraction ->
//		_assert(cursorRef == null)
		// TODO: cursorRef shouldn't be set here...
		cursorRef?.remove()
		cursorRef = cursorManager.addCursor(cursor, priority)
	}

	private val rollOutHandler = {
		event: MouseInteraction ->
		cursorRef?.remove()
		cursorRef = null
	}

	init {
		target.rollOver().add(rollOverHandler)
		target.rollOut().add(rollOutHandler)
	}

	override fun dispose() {
		cursorRef?.remove()
		cursorRef = null
		target.rollOver().remove(rollOverHandler)
		target.rollOut().remove(rollOutHandler)
	}

	companion object
}

fun UiComponentRo.clearCursor() {
	removeAttachment<RollOverCursor>(RollOverCursor)?.dispose()
}

fun UiComponentRo.cursor(cursor: Cursor, priority: Float = CursorPriority.ACTIVE): RollOverCursor {
	return createOrReuseAttachment(RollOverCursor, { RollOverCursor(this, cursor, priority) })
}