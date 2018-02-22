package com.acornui.core.input.interaction

import com.acornui.core.graphics.Texture
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType

interface PasteInteractionRo : InteractionEventRo {

	suspend fun <T : Any> getItemByType(type: ClipboardItemType<T>): T?

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