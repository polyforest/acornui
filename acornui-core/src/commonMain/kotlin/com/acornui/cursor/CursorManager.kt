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

import com.acornui.Disposable
import com.acornui.Lifecycle
import com.acornui.LifecycleBase
import com.acornui.collection.sortedInsertionIndex
import com.acornui.di.Context

interface CursorManager {

	/**
	 * Adds a cursor with the given priority. (Only the last cursor will be displayed.)
	 * @param cursor The cursor to add. Use [StandardCursor] to grab a cursor object.
	 * @param priority The priority of the cursor. Use [CursorPriority] to get useful default priorities. Higher numbers
	 * will take precedence over lower numbers.
	 * @return Returns the cursor reference of the cursor added, this can be used to remove the cursor.
	 */
	fun addCursor(cursor: Cursor, priority: Float = 0f): CursorReference

	/**
	 * Returns a [Cursor] object for a [StandardCursor].
	 */
	fun getStandardCursor(cursor: StandardCursor): Cursor

	/**
	 * Removes the cursor by reference.
	 * Calling [CursorReference.remove] will invoke this function.
	 */
	fun removeCursor(cursorReference: CursorReference)

	companion object : Context.Key<CursorManager>
}

/**
 * Adds the predefined cursor.
 * This is the same as calling `addCursor(getStandardCursor(cursor), priority)`
 * @see addCursor
 */
fun CursorManager.addCursor(cursor: StandardCursor, priority: Float = 0f): CursorReference =
		addCursor(getStandardCursor(cursor), priority)

interface Cursor : Lifecycle

class CursorReference(
		val manager: CursorManager,
		val cursor: Cursor,
		val priority: Float = 0f
) : Comparable<CursorReference>, Disposable {

	/**
	 * A convenience function to remove this cursor from the same manager to which it was added.
	 */
	@Deprecated("Use dispose", ReplaceWith("dispose()"))
	fun remove() = dispose()

	override fun dispose() {
		manager.removeCursor(this)
	}

	override fun compareTo(other: CursorReference): Int {
		return priority.compareTo(other.priority)
	}
}

/**
 * A [CursorManager] implementation without the platform-specific cursor factory methods.
 */
abstract class CursorManagerBase : CursorManager {

	private val cursorStack = ArrayList<CursorReference>()

	private var _currentCursor: CursorReference? = null

	override fun addCursor(cursor: Cursor, priority: Float): CursorReference {
		val cursorReference = CursorReference(this, cursor, priority)
		val index = cursorStack.sortedInsertionIndex(cursorReference)
		cursorStack.add(index, cursorReference)
		currentCursor(cursorStack.lastOrNull())
		return cursorReference
	}

	override fun removeCursor(cursorReference: CursorReference) {
		val index = cursorStack.indexOf(cursorReference)
		if (index == -1) return
		cursorStack.removeAt(index)
		currentCursor(cursorStack.lastOrNull())
	}

	private fun currentCursor(value: CursorReference?) {
		if (_currentCursor == value) return
		if (_currentCursor?.cursor?.isActive == true) {
			_currentCursor?.cursor?.deactivate()
		}
		_currentCursor = value
		if (_currentCursor?.cursor?.isActive != true) {
			_currentCursor?.cursor?.activate()
		}
	}
}

@Deprecated("Use StandardCursor", ReplaceWith("StandardCursor"))
typealias StandardCursors = StandardCursor

/**
 * A suite of cursors that standard components should be able to rely on existing.
 */
enum class StandardCursor {
	ALIAS,
	ALL_SCROLL,
	CELL,
	COPY,
	CROSSHAIR,
	DEFAULT,
	HAND,
	HELP,
	IBEAM,
	MOVE,
	NONE,
	NOT_ALLOWED,
	POINTER_WAIT,
	RESIZE_EW,
	RESIZE_NS,
	RESIZE_NE,
	RESIZE_SE,
	WAIT,
}

object DummyCursor : LifecycleBase(), Cursor

object CursorPriority {
	var PASSIVE: Float = 0f
	var ACTIVE: Float = 10f
	var POINTER_WAIT: Float = 50f
	var WAIT: Float = 100f
	var NOT_ALLOWED: Float = 1000f
}

fun Context.busyCursor(): CursorReference? {
	return injectOptional(CursorManager)?.addCursor(StandardCursor.POINTER_WAIT, CursorPriority.POINTER_WAIT)
}
