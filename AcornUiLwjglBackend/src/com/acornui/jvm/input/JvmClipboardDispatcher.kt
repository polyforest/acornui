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
import com.acornui.core.graphics.Texture
import com.acornui.core.input.Ascii
import com.acornui.core.input.InteractionEventBase
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.KeyInput
import com.acornui.core.input.interaction.PasteInteractionRo
import com.acornui.core.input.interaction.ClipboardItemType
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.io.ReadBuffer
import com.acornui.jvm.io.readTextAndClose
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.InputStream


class JvmClipboardDispatcher(
		override val injector: Injector
) : Scoped, Disposable {

	private val key = inject(KeyInput)
	private val interactivity = inject(InteractivityManager)
	private val focus = inject(FocusManager)
	private val stage = inject(Stage)

	private val event = JvmPasteInteraction()

	init {
		key.keyDown.add(this::keyDownHandler)
	}

	private fun keyDownHandler(e: KeyInteractionRo) {
		if (e.ctrlKey && e.keyCode == Ascii.V) {
			event.clear()
			event.type = PasteInteractionRo.PASTE
			interactivity.dispatch(focus.focused() ?: stage, event)
		}
	}

	override fun dispose() {
		key.keyDown.remove(this::keyDownHandler)
	}
}

private class JvmPasteInteraction : InteractionEventBase(), PasteInteractionRo {

	@Suppress("UNCHECKED_CAST")
	override suspend fun <T : Any> getItemByType(type: ClipboardItemType<T>): T? {
		return when (type) {
			ClipboardItemType.PLAIN_TEXT -> {
				getItemByFlavor(PLAIN_TEXT_FLAVOR)?.getAsString() as T?
			}

			ClipboardItemType.HTML -> {
				getItemByFlavor(HTML_TEXT_FLAVOR)?.getAsString() as T?
			}

			ClipboardItemType.TEXTURE ->  {
				TODO()
			}
			ClipboardItemType.FILE_LIST -> TODO()

			else -> null
		}
	}

	private fun getItemByFlavor(flavor: DataFlavor): JvmDataTransferItem? {
		val clipboard = Toolkit.getDefaultToolkit().systemClipboard
		if (clipboard.isDataFlavorAvailable(flavor)) {
			return JvmDataTransferItem(flavor)
		} else {
			return null
		}
	}

	companion object {

		private val PLAIN_TEXT_FLAVOR = DataFlavor("text/plain; class=java.io.InputStream; charset=UTF-8")
		private val HTML_TEXT_FLAVOR = DataFlavor("text/html; class=java.io.InputStream; charset=UTF-8")
	}
}

private class JvmDataTransferItem(val flavor: DataFlavor) {

	fun getAsString(): String? {
		val clipboard = Toolkit.getDefaultToolkit().systemClipboard
		val data = clipboard.getData(flavor) as InputStream
		return data.readTextAndClose()
	}

	suspend fun getAsTexture(): Texture? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	suspend fun getAsBlob(): ReadBuffer<Byte>? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

}