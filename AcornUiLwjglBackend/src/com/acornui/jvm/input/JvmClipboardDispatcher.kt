/*
 * Copyright 2018 Nicholas Bilyk
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

import com.acornui.component.Stage
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.focus.FocusManager
import com.acornui.core.input.Ascii
import com.acornui.core.input.InteractionEventBase
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.KeyInput
import com.acornui.core.input.interaction.ClipboardItemType
import com.acornui.core.input.interaction.CopyInteractionRo
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.core.input.interaction.PasteInteractionRo
import org.lwjgl.glfw.GLFW


class JvmClipboardDispatcher(
		override val injector: Injector,
		windowId: Long
) : Scoped, Disposable {

	private val key = inject(KeyInput)
	private val interactivity = inject(InteractivityManager)
	private val focus = inject(FocusManager)
	private val stage = inject(Stage)

	private val pasteEvent = JvmPasteInteraction(windowId)
	private val copyEvent = JvmCopyInteraction(windowId)

	init {
		key.keyDown.add(this::keyDownHandler)
	}

	private fun keyDownHandler(e: KeyInteractionRo) {
		if (e.ctrlKey && e.keyCode == Ascii.V) {
			pasteEvent.clear()
			pasteEvent.type = PasteInteractionRo.PASTE
			interactivity.dispatch(focus.focused() ?: stage, pasteEvent)
		} else if (e.ctrlKey && e.keyCode == Ascii.C) {
			copyEvent.clear()
			copyEvent.type = CopyInteractionRo.COPY
			interactivity.dispatch(focus.focused() ?: stage, copyEvent)
		} else if (e.ctrlKey && e.keyCode == Ascii.X) {
			copyEvent.clear()
			copyEvent.type = CopyInteractionRo.CUT
			interactivity.dispatch(focus.focused() ?: stage, copyEvent)
		}
	}

	override fun dispose() {
		key.keyDown.remove(this::keyDownHandler)
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