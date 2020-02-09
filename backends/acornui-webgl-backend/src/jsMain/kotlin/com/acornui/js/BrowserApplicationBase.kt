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

package com.acornui.js

import com.acornui.JsApplicationBase
import com.acornui.component.HtmlComponent
import com.acornui.cursor.CursorManager
import com.acornui.di.Context
import com.acornui.di.dKey
import com.acornui.focus.FocusManager
import com.acornui.graphic.Window
import com.acornui.input.*
import com.acornui.js.cursor.JsCursorManager
import com.acornui.js.input.JsClipboard
import com.acornui.js.input.JsKeyInput
import com.acornui.js.input.JsMouseInput
import com.acornui.uncaughtExceptionHandler
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.HTMLElement
import org.w3c.dom.LOADING
import kotlin.browser.document
import kotlin.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The base class for browser-based Acorn UI applications.
 * This will add boot tasks that initialize input for the target canvas.
 */
@Suppress("unused")
abstract class BrowserApplicationBase : JsApplicationBase() {

	init {
		val window = if (jsTypeOf(window) != "undefined") window else error("BrowserApplicationBase can only be used in browser applications.")

		// Uncaught exception handler
		val prevOnError = window.onerror
		window.onerror = { message, source, lineNo, colNo, error ->
			prevOnError?.invoke(message, source, lineNo, colNo, error)
			if (error is Throwable)
				uncaughtExceptionHandler(error)
			else
				uncaughtExceptionHandler(Exception("Unknown error: $message $lineNo $source $colNo $error"))
		}

		val oBU = window.onbeforeunload
		window.onbeforeunload = {
			oBU?.invoke(it)
			dispose()
			undefined // Necessary for ie11 not to alert user.
		}
	}

	private suspend fun contentLoad() = suspendCoroutine<Unit> { cont ->
		if (document.readyState == DocumentReadyState.LOADING) {
			document.addEventListener("DOMContentLoaded", {
				cont.resume(Unit)
			})
		} else {
			cont.resume(Unit)
		}
	}

	abstract val canvasTask: suspend () -> HTMLElement
	abstract val windowTask: suspend () -> Window
	abstract val componentsTask: suspend () -> (owner: Context) -> HtmlComponent

	protected open val mouseInputTask by task(MouseInput) {
		JsMouseInput(get(CANVAS))
	}

	protected open val keyInputTask by task(KeyInput) {
		JsKeyInput(get(CANVAS), config().input.jsCaptureAllKeyboardInput)
	}

	protected open val interactivityTask by task(InteractivityManager) {
		InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager))
	}

	protected open val cursorManagerTask by task(CursorManager) {
		JsCursorManager(get(CANVAS))
	}

	protected open val clipboardTask by task(Clipboard) {
		JsClipboard(
				get(CANVAS),
				get(FocusManager),
				get(InteractivityManager),
				config().input.jsCaptureAllKeyboardInput
		)
	}

	companion object {
		protected val CANVAS = dKey<HTMLElement>()
	}

}