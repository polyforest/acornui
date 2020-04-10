/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.headless

import com.acornui.LifecycleBase
import com.acornui.cursor.Cursor
import com.acornui.cursor.CursorManager
import com.acornui.cursor.CursorReference
import com.acornui.cursor.StandardCursor

object MockCursorManager : CursorManager {

	override fun addCursor(cursor: Cursor, priority: Float): CursorReference {
		return CursorReference(this, cursor, priority)
	}

	override fun getStandardCursor(cursor: StandardCursor): Cursor = MockCursor()

	override fun removeCursor(cursorReference: CursorReference) {}
}

class MockCursor : Cursor, LifecycleBase()