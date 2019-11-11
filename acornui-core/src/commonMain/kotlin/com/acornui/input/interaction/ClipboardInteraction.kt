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

package com.acornui.input.interaction

import com.acornui.graphic.Texture
import com.acornui.input.InteractionEventRo
import com.acornui.input.InteractionType

interface PasteInteractionRo : InteractionEventRo {

	fun <T : Any> getItemByType(type: ClipboardItemType<T>): T?

	companion object {
		val PASTE = InteractionType<PasteInteractionRo>("paste")
	}
}

interface CopyInteractionRo : InteractionEventRo {

	fun <T : Any> addItem(type: ClipboardItemType<T>, value: T)

	companion object {
		val COPY = InteractionType<CopyInteractionRo>("copy")
		val CUT = InteractionType<CopyInteractionRo>("cut")
	}
}

@Suppress("unused")
class ClipboardItemType<T : Any> {

	companion object {

		val PLAIN_TEXT = ClipboardItemType<String>()

		val HTML = ClipboardItemType<String>()

		val TEXTURE = ClipboardItemType<Texture>()

		val FILE_LIST = ClipboardItemType<List<ClipboardFile>>()
	}
}

class ClipboardFile {

}
