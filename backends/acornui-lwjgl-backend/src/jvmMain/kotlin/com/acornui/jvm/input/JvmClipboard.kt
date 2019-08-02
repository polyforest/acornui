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

package com.acornui.jvm.input

import com.acornui.Disposable
import com.acornui.focus.FocusManager
import com.acornui.input.*
import com.acornui.input.interaction.*
import org.lwjgl.glfw.GLFW


class JvmClipboard(
		private val keyInput: KeyInput,
		private val focusManager: FocusManager,
		private val interactivityManager: InteractivityManager,
		private val windowId: Long
) : Clipboard, Disposable {

	private val pasteEvent = JvmPasteInteraction(windowId)
	private val copyEvent = JvmCopyInteraction(windowId)

	init {
		keyInput.keyDown.add(::keyDownHandler)
	}

	override fun copy(str: String): Boolean {
		GLFW.glfwSetClipboardString(windowId, str)
		return true
	}

	override fun triggerCopy(): Boolean {
		val focused = focusManager.focused ?: return false
		copyEvent.clear()
		copyEvent.type = CopyInteractionRo.COPY
		interactivityManager.dispatch(focused, copyEvent)
		return true
	}

	private fun keyDownHandler(e: KeyInteractionRo) {
		val focused = focusManager.focused ?: return
		if (e.commandPlat && e.keyCode == Ascii.V) {
			pasteEvent.clear()
			pasteEvent.type = PasteInteractionRo.PASTE
			interactivityManager.dispatch(focused, pasteEvent)
		} else if (e.commandPlat && e.keyCode == Ascii.C) {
			copyEvent.clear()
			copyEvent.type = CopyInteractionRo.COPY
			interactivityManager.dispatch(focused, copyEvent)
		} else if (e.commandPlat && e.keyCode == Ascii.X) {
			copyEvent.clear()
			copyEvent.type = CopyInteractionRo.CUT
			interactivityManager.dispatch(focused, copyEvent)
		}
	}

	override fun dispose() {
		keyInput.keyDown.remove(::keyDownHandler)
	}
}

private class JvmPasteInteraction(private val windowId: Long) : InteractionEventBase(), PasteInteractionRo {

	@Suppress("UNCHECKED_CAST")
	override suspend fun <T : Any> getItemByType(type: ClipboardItemType<T>): T? {
		return when (type) {
			ClipboardItemType.PLAIN_TEXT -> {
				GLFW.glfwGetClipboardString(windowId) as T
			}

			ClipboardItemType.HTML -> {
				GLFW.glfwGetClipboardString(windowId) as T
			}

			ClipboardItemType.TEXTURE ->  {
				TODO()
			}
			ClipboardItemType.FILE_LIST -> TODO()

			else -> null
		}
	}
}


private class JvmCopyInteraction(private val windowId: Long) : InteractionEventBase(), CopyInteractionRo {

	override fun <T : Any> addItem(type: ClipboardItemType<T>, value: T) {
		when (type) {
			ClipboardItemType.PLAIN_TEXT -> {
				GLFW.glfwSetClipboardString(windowId, value as String)
			}

			ClipboardItemType.HTML -> {
				GLFW.glfwSetClipboardString(windowId, value as String)
			}

			ClipboardItemType.TEXTURE ->  {
				TODO()
			}
			ClipboardItemType.FILE_LIST -> TODO()
		}
	}
}
