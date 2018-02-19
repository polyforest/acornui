package com.acornui.js.dom

//import com.acornui.collection.equalsArray
//import com.acornui.collection.hashCodeIterable
//import com.acornui.core.graphics.Texture
//import com.acornui.core.input.interaction.DataTransferItem
//import com.acornui.core.input.interaction.DataTransferRead
//import org.w3c.dom.DataTransfer
//import org.w3c.dom.DataTransferItemList
//import org.w3c.dom.get
//import org.w3c.files.File
//import org.w3c.files.FileList
//import org.w3c.files.FileReader
//import org.w3c.files.get
//
//
//internal class DomDataTransfer(private val clipboardData: DataTransfer?) : DataTransferRead {
//
//	override val items = ArrayList<DataTransferItem>()
//
//	init {
//		if (clipboardData != null) {
//			if (clipboardData.types as Array<String>? != null) {
//				// Fx
//				for (type in clipboardData.types) {
//					val plainText = clipboardData.getData(type)
//					items.add(StringTransferItem(plainText, type))
//				}
//			} else if (clipboardData.items as DataTransferItemList? != null) {
//				// Chrome, Opera
//				for (i in 0..clipboardData.items.length - 1) {
//					val item = clipboardData.items[i]
//					items.add(DomDataTransferItem(item!!))
//				}
//			} else {
//				// IE
//				// Casts are needed because in IE these return values may be null.
//				val plainText = clipboardData.getData("text") as String? //
//				if (plainText != null)
//					items.add(StringTransferItem(plainText, "text/plain"))
//				val url = clipboardData.getData("url") as String?
//				if (url != null)
//					items.add(StringTransferItem(url, "text/url"))
//			}
//			if (clipboardData.files as FileList? != null) {
//				for (i in 0..clipboardData.files.length - 1) {
//					val file = clipboardData.files[i]!!
//					items.add(FileDataTransferItem(file))
//				}
//			}
//		}
//
//	}
//}
//
//private class DomDataTransferItem(private val item: org.w3c.dom.DataTransferItem) : DataTransferItem {
//
//	override val humanName: String = item.kind
//	override val mimeType: String = item.type
//
//	override fun getAsString(callback: (String) -> Unit) {
//		return item.getAsString(callback)
//	}
//
//	override fun getAsTexture(callback: (Texture) -> Unit) {
//		val f = item.getAsFile()!!
//		val t = DomTexture()
//		t.image.onload = {
//			callback(t)
//		}
//		t.blob(f)
//	}
//
//	override fun toString(): String {
//		return "DomDataTransferItem(kind='$humanName' type='$mimeType')"
//	}
//}
//
//private class FileDataTransferItem(private val item: File) : DataTransferItem {
//
//	override val humanName: String = "file"
//	override val mimeType: String = item.type
//
//	override fun getAsString(callback: (String) -> Unit) {
//		val r = FileReader()
//		r.onload = {
//			callback(r.result)
//		}
//		r.readAsText(item)
//	}
//
//	override fun getAsTexture(callback: (Texture) -> Unit) {
//		val t = DomTexture()
//		t.image.onload = {
//			callback(t)
//		}
//		t.blob(item)
//	}
//
//	override fun toString(): String {
//		return "FileDataTransferItem(kind='$humanName' type='$mimeType')"
//	}
//}
//
//private class StringTransferItem(
//		private val text: String,
//		override val mimeType: String
//) : DataTransferItem {
//
//	override val humanName: String = "string"
//
//	override fun getAsString(callback: (String) -> Unit) {
//		callback(text)
//	}
//
//	override fun getAsTexture(callback: (Texture) -> Unit) {
//		throw Exception("DataTransferItem with kind $humanName does not support textures.")
//	}
//
//	override fun toString(): String {
//		return "TextDataTransferItem(kind='$humanName' type='$mimeType' value='${text.substring(0, 50)}')"
//	}
//}
//
//fun MimeType(vararg types: String): MimeType {
//	return MimeType(*types)
//}
//
//data class MimeType(
//		val types: Array<String>
//) {
//
//	override fun equals(other: Any?): Boolean {
//		if (other !is MimeType) return false
//		return types.equalsArray(other.types)
//	}
//
//	override fun hashCode(): Int {
//		return 31 * types.hashCodeIterable()
//	}
//
//	companion object {
//		val TEXT = MimeType("text/plain")
//		val TEXT_URI_LIST = MimeType("text/uri-list")
//		val TEXT_CSV = MimeType("text/csv")
//		val TEXT_CSS = MimeType("text/css")
//		val TEXT_HTML = MimeType("text/html")
//		val APPLICATION_XHTML = MimeType("application/xhtml+xml")
//		val IMAGE_PNG = MimeType("image/png")
//		val IMAGE_JPG = MimeType("image/jpg", "image/jpeg")
//		val IMAGE_GIF = MimeType("image/gif")
//		val IMAGE_SVG = MimeType("image/svg+xml")
//		val XML = MimeType("application/xml", "text/xml")
//		val JAVASCRIPT = MimeType("application/javascript")
//		val JSON = MimeType("application/json")
//		val OCTET_STREAM = MimeType("application/octet-stream")
//	}
//}