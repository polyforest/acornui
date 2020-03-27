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
import com.acornui.di.ContextImpl
import com.acornui.function.as1
import com.acornui.input.interaction.rollOut
import com.acornui.input.interaction.rollOver
import com.acornui.properties.afterChange
import com.acornui.properties.afterChangeWithInit

/**
 * An attachment that changes the cursor on roll over.
 */
class RollOverCursor(
		private val target: UiComponentRo
) : ContextImpl(target) {

	@Suppress("RemoveExplicitTypeArguments")
	var priority by afterChange<Float>(CursorPriority.ACTIVE) {
		refresh()
	}

	var cursor by afterChange<Cursor?>(null) {
		refresh()
	}

	private val cursorManager = injectOptional(CursorManager)

	private var cursorRef: CursorReference? = null

	var enabled: Boolean by afterChangeWithInit(true) { value ->
		if (value) {
			target.rollOver().add(::show.as1)
			target.rollOut().add(::hide.as1)
		} else {
			hide()
			target.rollOver().remove(::show.as1)
			target.rollOut().remove(::hide.as1)
		}
	}

	private fun show() {
		hide()
		val cursor = cursor ?: return
		cursorRef = cursorManager?.addCursor(cursor, priority)
	}

	private fun hide() {
		cursorRef?.dispose()
		cursorRef = null
	}

	private fun refresh() {
		if (cursorRef != null) {
			hide()
			show()
		}
	}

	override fun dispose() {
		super.dispose()
		enabled = false
	}

	companion object
}

/**
 * Disposes the roll over cursor attachment.
 */
fun UiComponentRo.clearCursor() {
	removeAttachment<RollOverCursor>(RollOverCursor)?.dispose()
}

/**
 * Sets the roll over cursor to be used on this component.
 * Setting this will replace any previous roll over cursor.
 * @return Returns a disposable reference to the [RollOverCursor] attachment.
 * @see clearCursor
 */
fun UiComponentRo.cursor(cursor: Cursor?, priority: Float = CursorPriority.ACTIVE): RollOverCursor? {
	return if (cursor == null) {
		clearCursor()
		null
	} else createOrReuseAttachment(RollOverCursor) { RollOverCursor(this) }.also {
		it.cursor = cursor
		it.priority = priority
	}
}

/**
 * Sets the roll over cursor to be used on this component.
 * Setting this will replace any previous roll over cursor.
 * @return Returns a disposable reference to the [RollOverCursor] attachment.
 * @see clearCursor
 */
fun UiComponentRo.cursor(cursor: StandardCursor, priority: Float = CursorPriority.ACTIVE): RollOverCursor? {
	return cursor(inject(CursorManager).getStandardCursor(cursor), priority)
}
