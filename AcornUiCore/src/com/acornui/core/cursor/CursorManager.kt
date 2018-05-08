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

import com.acornui.collection.Clearable
import com.acornui.collection.ClearableObjectPool
import com.acornui.collection.sortedInsertionIndex
import com.acornui.core.Lifecycle
import com.acornui.core.di.DKey
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import kotlin.properties.Delegates

interface CursorManager {

	/**
	 * Adds a cursor at the given index in the stack. (Only the last cursor will be displayed.)
	 * @param cursor The cursor to add. Use [StandardCursors] to grab a cursor object.
	 * @param priority The priority of the cursor. Use [CursorPriority] to get useful default priorities.
	 * @return Returns the cursor reference of the cursor added, this can be used to remove the cursor.
	 */
	fun addCursor(cursor: Cursor, priority: Float = 0f): CursorReference

	fun removeCursor(cursorReference: CursorReference)

	companion object : DKey<CursorManager>
}

interface Cursor : Lifecycle

interface CursorReference : Comparable<CursorReference> {

	val priority: Float

	/**
	 * A convenience function to remove this cursor from the same manager to which it was added.
	 */
	fun remove()
}

private class CursorReferenceImpl : Clearable, CursorReference {

	var cursor: Cursor? = null
	override var priority: Float = 0f

	var manager: CursorManager? = null

	override fun remove() {
		manager!!.removeCursor(this)
		manager = null
	}

	override fun clear() {
		cursor = null
		manager = null
		priority = 0f
	}

	override fun compareTo(other: CursorReference): Int {
		return priority.compareTo(other.priority)
	}

}

/**
 * A [CursorManager] implementation without the platform-specific cursor factory methods.
 */
abstract class CursorManagerBase : CursorManager {

	private val cursorStack = ArrayList<CursorReferenceImpl>()

	private var _currentCursor: CursorReferenceImpl? = null

	override fun addCursor(cursor: Cursor, priority: Float): CursorReference {
		val cursorReference = obtainCursor(cursor, priority)
		cursorReference.manager = this
		val index = cursorStack.sortedInsertionIndex(cursorReference)
		cursorStack.add(index, cursorReference)
		currentCursor(cursorStack.lastOrNull())
		return cursorReference
	}

	override fun removeCursor(cursorReference: CursorReference) {
		val index = cursorStack.indexOf(cursorReference)
		if (index == -1) return
		val removed = cursorStack.removeAt(index)
		currentCursor(cursorStack.lastOrNull())
		cursorReferencePool.free(removed)
	}

	private fun currentCursor(value: CursorReferenceImpl?) {
		if (_currentCursor == value) return
		if (_currentCursor?.cursor?.isActive ?: false) {
			_currentCursor?.cursor?.deactivate()
		}
		_currentCursor = value
		if (!(_currentCursor?.cursor?.isActive ?: false)) {
			_currentCursor?.cursor?.activate()
		}
	}

	companion object {
		private val cursorReferencePool = ClearableObjectPool { CursorReferenceImpl() }

		private fun obtainCursor(cursor: Cursor, priority: Float): CursorReferenceImpl {
			val r = cursorReferencePool.obtain()
			r.cursor = cursor
			r.priority = priority
			return r
		}
	}
}

/**
 * A suite of cursors that standard components should be able to rely on existing.
 */
object StandardCursors {
	var ALIAS: Cursor by Delegates.notNull()
	var ALL_SCROLL: Cursor by Delegates.notNull()
	var CELL: Cursor by Delegates.notNull()
	var COPY: Cursor by Delegates.notNull()
	var CROSSHAIR: Cursor by Delegates.notNull()
	var DEFAULT: Cursor by Delegates.notNull()
	var HAND: Cursor by Delegates.notNull()
	var HELP: Cursor by Delegates.notNull()
	var IBEAM: Cursor by Delegates.notNull()
	var MOVE: Cursor by Delegates.notNull()
	var NONE: Cursor by Delegates.notNull()
	var NOT_ALLOWED: Cursor by Delegates.notNull()
	var POINTER_WAIT: Cursor by Delegates.notNull()
	var RESIZE_E: Cursor by Delegates.notNull()
	var RESIZE_N: Cursor by Delegates.notNull()
	var RESIZE_NE: Cursor by Delegates.notNull()
	var RESIZE_SE: Cursor by Delegates.notNull()
	var WAIT: Cursor by Delegates.notNull()
}

object CursorPriority {
	var PASSIVE: Float = 0f
	var ACTIVE: Float = 10f
	var POINTER_WAIT: Float = 50f
	var WAIT: Float = 100f
	var NOT_ALLOWED: Float = 1000f
}

fun Owned.busyCursor(): CursorReference {
	return inject(CursorManager).addCursor(StandardCursors.POINTER_WAIT, CursorPriority.POINTER_WAIT)
}