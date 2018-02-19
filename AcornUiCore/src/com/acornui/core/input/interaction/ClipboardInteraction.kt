package com.acornui.core.input.interaction

import com.acornui.collection.find2
import com.acornui.core.graphics.Texture
import com.acornui.core.input.InteractionEventBase
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType
import kotlin.properties.Delegates

interface ClipboardInteractionRo : InteractionEventRo {

	val dataTransfer: DataTransferRead

	companion object {
		val COPY = InteractionType<ClipboardInteraction>("copy")
		val CUT = InteractionType<ClipboardInteraction>("cut")
		val PASTE = InteractionType<ClipboardInteraction>("paste")
	}
}

class ClipboardInteraction : InteractionEventBase(), ClipboardInteractionRo {

	override var dataTransfer: DataTransferRead by Delegates.notNull()

}

interface DataTransferRead {

	val items: List<DataTransferItem>

	fun getItemByType(type: String): DataTransferItem? {
		return items.find2 { it.type == type }
	}
}

//fun addItem(data: String, type: String): DataTransferItem?
//fun addItem(data: File): DataTransferItem?
//fun removeItem(index: Int)
//fun clearItems()

interface DataTransferItem {

	val kind: String
	val type: String

	/**
	 * Retrieves the data transfer as plain text, passing it to the [callback] argument when ready.
	 */
	fun getAsString(callback: (String) -> Unit)

	/**
	 * Retrieves the data transfer as a Texture, passing it to the [callback] argument when ready.
	 */
	fun getAsTexture(callback: (Texture) -> Unit)

//	fun getAsBlob(callback: (ByteArray) -> Unit)

}