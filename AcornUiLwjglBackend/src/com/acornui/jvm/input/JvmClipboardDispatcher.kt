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

import com.acornui.collection.Clearable
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
import com.acornui.jvm.io.readTextAndClose
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.InputStream
import java.io.StringReader


class JvmClipboardDispatcher(
		override val injector: Injector
) : Scoped, Disposable {

	private val key = inject(KeyInput)
	private val interactivity = inject(InteractivityManager)
	private val focus = inject(FocusManager)
	private val stage = inject(Stage)

	private val pasteEvent = JvmPasteInteraction()
	private val copyEvent = JvmCopyInteraction()

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

private class JvmPasteInteraction : InteractionEventBase(), PasteInteractionRo {

	private val clipboard = Toolkit.getDefaultToolkit().systemClipboard

	@Suppress("UNCHECKED_CAST")
	override suspend fun <T : Any> getItemByType(type: ClipboardItemType<T>): T? {
		return when (type) {
			ClipboardItemType.PLAIN_TEXT -> {
				@Suppress("DEPRECATION")
				clipboard.getString(Flavors.PLAIN_TEXT_FLAVOR, DataFlavor.stringFlavor, DataFlavor.plainTextFlavor) as T?
			}

			ClipboardItemType.HTML -> {
				clipboard.getString(Flavors.HTML_TEXT_FLAVOR) as T?
			}

			ClipboardItemType.TEXTURE ->  {
				TODO()
			}
			ClipboardItemType.FILE_LIST -> TODO()

			else -> null
		}
	}

	private fun Clipboard.getDataOrNull(flavor: DataFlavor): Any? {
		return if (isDataFlavorAvailable(flavor)) getData(flavor) else null
	}

	private fun Clipboard.getString(vararg flavors: DataFlavor): String? {
		for (flavor in flavors) {
			val data = getDataOrNull(flavor)
			if (data is InputStream) {
				return data.readTextAndClose()
			} else if (data is String) {
				return data
			}
		}
		return null
	}
}


private class JvmCopyInteraction : InteractionEventBase(), CopyInteractionRo {

	private val clipboard = Toolkit.getDefaultToolkit().systemClipboard

	private val contents = object : Transferable, Clearable {

		private val map = HashMap<DataFlavor, Any>()

		override fun getTransferData(flavor: DataFlavor?): Any {
			return map[flavor]!!
		}

		override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
			return map.containsKey(flavor)
		}

		override fun getTransferDataFlavors(): Array<DataFlavor> {
			return map.keys.toTypedArray()
		}

		fun addData(flavor: DataFlavor, data: Any) {
			map[flavor] = data
		}

		override fun clear() {
			map.clear()
		}
	}

	override fun <T : Any> addItem(type: ClipboardItemType<T>, value: T) {
		when (type) {
			ClipboardItemType.PLAIN_TEXT -> {
				contents.addData(DataFlavor.stringFlavor, value as String)
				val reader = StringReader(value as String)
				contents.addData(Flavors.PLAIN_TEXT_FLAVOR, reader)
				contents.addData(DataFlavor.plainTextFlavor, reader)
			}

			ClipboardItemType.HTML -> {
				contents.addData(DataFlavor.stringFlavor, value as String)
				val reader = StringReader(value as String)
				contents.addData(Flavors.HTML_TEXT_FLAVOR, reader)
				contents.addData(Flavors.PLAIN_TEXT_FLAVOR, reader)
				contents.addData(DataFlavor.plainTextFlavor, reader)
			}

			ClipboardItemType.TEXTURE ->  {
				TODO()
			}
			ClipboardItemType.FILE_LIST -> TODO()
		}
		clipboard.setContents(contents, null)
	}

	override fun clear() {
		super.clear()
		contents.clear()
	}
}
internal object Flavors {

	val PLAIN_TEXT_FLAVOR = DataFlavor("text/plain; class=java.io.InputStream; charset=UTF-8")
	val HTML_TEXT_FLAVOR = DataFlavor("text/html; class=java.io.InputStream; charset=UTF-8")
}