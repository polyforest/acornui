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

import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import com.acornui.collection.sortedInsertionIndex
import com.acornui.Lifecycle
import com.acornui.LifecycleBase
import com.acornui.di.DKey
import com.acornui.di.Owned
import com.acornui.di.injectOptional

interface CursorManager {

	/**
	 * Adds a cursor at the given index in the stack. (Only the last cursor will be displayed.)
	 * @param cursor The cursor to add. Use [StandardCursors] to grab a cursor object.
	 * @param priority The priority of the cursor. Use [CursorPriority] to get useful default priorities. Higher numbers
	 * will take precedence over lower numbers.
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
		if (_currentCursor?.cursor?.isActive == true) {
			_currentCursor?.cursor?.deactivate()
		}
		_currentCursor = value
		if (_currentCursor?.cursor?.isActive != true) {
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
	var ALIAS: Cursor = DummyCursor
	var ALL_SCROLL: Cursor = DummyCursor
	var CELL: Cursor = DummyCursor
	var COPY: Cursor = DummyCursor
	var CROSSHAIR: Cursor = DummyCursor
	var DEFAULT: Cursor = DummyCursor
	var HAND: Cursor = DummyCursor
	var HELP: Cursor = DummyCursor
	var IBEAM: Cursor = DummyCursor
	var MOVE: Cursor = DummyCursor
	var NONE: Cursor = DummyCursor
	var NOT_ALLOWED: Cursor = DummyCursor
	var POINTER_WAIT: Cursor = DummyCursor
	var RESIZE_EW: Cursor = DummyCursor
	var RESIZE_NS: Cursor = DummyCursor
	var RESIZE_NE: Cursor = DummyCursor
	var RESIZE_SE: Cursor = DummyCursor
	var WAIT: Cursor = DummyCursor
}

object DummyCursor : LifecycleBase(), Cursor

object CursorPriority {
	var PASSIVE: Float = 0f
	var ACTIVE: Float = 10f
	var POINTER_WAIT: Float = 50f
	var WAIT: Float = 100f
	var NOT_ALLOWED: Float = 1000f
}

fun Owned.busyCursor(): CursorReference? {
	return injectOptional(CursorManager)?.addCursor(StandardCursors.POINTER_WAIT, CursorPriority.POINTER_WAIT)
}
