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

import com.acornui.collection.find2
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
import com.acornui.core.input.interaction.*
import com.acornui.core.input.interaction.ClipboardItemType.*
import com.acornui.io.ReadBuffer
import com.acornui.jvm.io.readTextAndClose
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.InputStream


class JvmClipboardDispatcher(
		override val injector: Injector
) : Scoped, Disposable {

	private val key = inject(KeyInput)
	private val interactivity = inject(InteractivityManager)
	private val focus = inject(FocusManager)
	private val stage = inject(Stage)

	private val event = JvmClipboardInteraction()

	init {
		key.keyDown.add(this::keyDownHandler)
	}

	private fun keyDownHandler(e: KeyInteractionRo) {
		if (e.ctrlKey && e.keyCode == Ascii.V) {
			event.clear()
			event.type = ClipboardInteractionRo.PASTE
			interactivity.dispatch(focus.focused() ?: stage, event)
		}
	}

	override fun dispose() {
		key.keyDown.remove(this::keyDownHandler)
	}
}

private class JvmClipboardInteraction : InteractionEventBase(), ClipboardInteractionRo {

	private var _items = ArrayList<JvmDataTransferItem>()

	private fun populate() {
//			val clipboard = Toolkit.getDefaultToolkit().systemClipboard
//			for (i in 0..clipboard.availableDataFlavors.lastIndex) {
//				val flavor = clipboard.availableDataFlavors[i]
//				_items.add(JvmDataTransferItem(flavor))
//			}
//			//val contents: Transferable? = clipboard.getContents(this)

//			if (contents != null) {
//				for (i in 0..contents.transferDataFlavors.lastIndex) {
//					val flavor = contents.transferDataFlavors[i]
//					_items.add(JvmDataTransferItem(flavor))
//				}
//			}
	}

	override fun getItemByType(type: ClipboardItemType): DataTransferItem? {
		return when (type) {
			PLAIN_TEXT -> {
				getItemByFlavor(PLAIN_TEXT_TYPE)
			}

			ClipboardItemType.HTML -> {
				_items.find2 {
					it.flavor == DataFlavor.allHtmlFlavor
				}
			}

			ClipboardItemType.URI_LIST -> _items.find2 {
				it.flavor == DataFlavor.stringFlavor
			}

			ClipboardItemType.TEXTURE -> _items.find2 {
				it.flavor == DataFlavor.imageFlavor
			}
			ClipboardItemType.FILE_LIST -> TODO()
		}
	}

	private fun getItemByFlavor(flavor: DataFlavor): DataTransferItem? {
		val clipboard = Toolkit.getDefaultToolkit().systemClipboard
		if (clipboard.isDataFlavorAvailable(flavor)) {
			return JvmDataTransferItem(flavor)
		} else {
			return null
		}
	}

	override fun clear() {
		super.clear()
	}

	companion object {

		private val PLAIN_TEXT_TYPE = DataFlavor("text/plain; class=java.io.InputStream; charset=UTF-8")
	}
}

private class JvmDataTransferItem(val flavor: DataFlavor) : DataTransferItem {

	override val humanName: String
		get() = flavor.humanPresentableName

	override val mimeType: String
		get() = flavor.mimeType

	init {
	}

	override suspend fun getAsString(): String? {
		val clipboard = Toolkit.getDefaultToolkit().systemClipboard
		val data = clipboard.getData(flavor) as InputStream
		return data.readTextAndClose()
	}

	override suspend fun getAsTexture(): Texture? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override suspend fun getAsBlob(): ReadBuffer<Byte>? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun toString(): String {
		return "JvmDataTransferItem(kind=$humanName, mimeType=$mimeType)"
	}


}