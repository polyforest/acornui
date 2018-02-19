package com.acornui.core.input.interaction

import com.acornui.core.graphics.Texture
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType
import com.acornui.io.ReadBuffer

interface ClipboardInteractionRo : InteractionEventRo {

	fun getItemByType(type: ClipboardItemType): DataTransferItem?

	companion object {
		val COPY = InteractionType<ClipboardInteractionRo>("copy")
		val CUT = InteractionType<ClipboardInteractionRo>("cut")
		val PASTE = InteractionType<ClipboardInteractionRo>("paste")
	}
}

enum class ClipboardItemType {

	PLAIN_TEXT,

	HTML,

	URI_LIST,

	TEXTURE,

	FILE_LIST

}

//fun addItem(data: String, type: String): DataTransferItem?
//fun addItem(data: File): DataTransferItem?
//fun removeItem(index: Int)
//fun clearItems()

interface DataTransferItem {

	val humanName: String

	val mimeType: String

	/**
	 * Retrieves the data transfer as plain UTF-8 text.
	 */
	suspend fun getAsString(): String?

	/**
	 * Retrieves the data transfer as a Texture.
	 */
	suspend fun getAsTexture(): Texture?

	suspend fun getAsBlob(): ReadBuffer<Byte>?

//	fun getAsBlob(callback: (ByteArray) -> Unit)

}