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

package com.acornui.asset

import com.acornui.cursor.CursorManager
import com.acornui.cursor.CursorPriority
import com.acornui.cursor.CursorReference
import com.acornui.cursor.StandardCursors
import com.acornui.di.Injector
import com.acornui.di.Owned

/**
 * A utility to watch the loading queue for status changes, creating a busy cursor when it's loading something.
 */
class LoadingQueueBusyWatch(injector: Injector) {

	private val assets = injector.inject(AssetManager)
	private val cursor = injector.injectOptional(CursorManager)

	private var _busyCursor: CursorReference? = null
	private var _isRunning = false

	private fun currentLoadersChanged() {
		setBusy(assets.currentLoaders.isNotEmpty())
	}

	fun start() {
		if (_isRunning) return
		setBusy(true) // No matter what, toggle the busy cursor so we don't have an endless loop with the loading queue loading the cursor itself.
		currentLoadersChanged()
		assets.currentLoadersChanged.add(::currentLoadersChanged)
	}

	fun stop() {
		if (!_isRunning) return
		setBusy(false)
		assets.currentLoadersChanged.remove(::currentLoadersChanged)
	}

	private fun setBusy(value: Boolean) {
		if ((_busyCursor != null) == value) return
		_busyCursor = if (value) {
			cursor?.addCursor(StandardCursors.POINTER_WAIT, CursorPriority.POINTER_WAIT)
		} else {
			_busyCursor?.remove()
			null
		}
	}
}

fun Owned.loadingQueueBusyWatch(): LoadingQueueBusyWatch {
	return LoadingQueueBusyWatch(injector)
}
