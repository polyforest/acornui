package com.acornui.js.dom.component

import com.acornui.component.BoxStyle
import com.acornui.component.UiComponent
import com.acornui.component.layout.setSize
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.component.scroll.toCssString
import com.acornui.component.text.EditableTextField
import com.acornui.component.text.TextCommand
import com.acornui.component.text.TextCommander
import com.acornui.component.text.styleWithCSS
import com.acornui.core.di.Owned
import com.acornui.core.focus.blurred
import com.acornui.core.focus.focused
import com.acornui.core.input.Ascii
import com.acornui.core.input.keyDown
import com.acornui.core.input.keyUp
import com.acornui.core.userInfo
import com.acornui.graphics.Color
import com.acornui.js.html.ClipboardEvent
import com.acornui.js.html.getSelection
import com.acornui.math.Bounds
import com.acornui.reflect.observable
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.files.FileReader
import kotlin.browser.document

/**
 * The Dom Editable text implementation which provides the utility needed for a rich text editor.
 */
open class DomEditableTextField(
		owner: Owned,
		element: HTMLDivElement = document.createElement("div") as HTMLDivElement,
		domContainer: DomContainer = DomContainer(element)
) : DomTextField(owner, element, domContainer), EditableTextField {

	final override var focusEnabled: Boolean = false
	final override var focusOrder: Float = 0f
	final override var highlight: UiComponent? by createSlot()

	final override val boxStyle = bind(BoxStyle())

	override val hScrollModel = DomScrollLeftModel(element)
	override val vScrollModel = DomScrollTopModel(element)

	final override val textCommander: TextCommander = object : TextCommander {
		override fun exec(commandName: String, value: String): Boolean {
			focus()

			if (commandName == TextCommand.insertHTML.name) {

				val s = document.getSelection()
				if (s.rangeCount > 0) {
					val range = s.getRangeAt(0)
					range.deleteContents()

					val span = document.createElement("span") as HTMLSpanElement
					span.innerHTML = value

					if (span.childNodes.length > 0) {
						val frag = document.createDocumentFragment()
						var lastNode: Node? = null
						while (span.childNodes.length > 0) {
							lastNode = frag.appendChild(span.firstChild!!)
						}
						range.insertNode(frag)

						// Move the caret immediately after the last inserted node.
						if (lastNode != null) {
							val newRange = range.cloneRange()
							newRange.setStartAfter(lastNode)
							newRange.collapse(toStart = true)
							s.removeAllRanges()
							s.addRange(newRange)
						}
					}
				}
				return true
			} else {
				return document.execCommand(commandName, false, value)
			}
		}

		override fun queryBool(commandId: String): Boolean {
			return document.queryCommandState(commandId)
		}

		override fun queryString(commandId: String): String {
			return document.queryCommandValue2(commandId).toString()
		}

		override fun queryColor(commandId: String): Color {
			val obj = document.queryCommandValue2(commandId)
			if (userInfo.isIe && obj is Number) {
				// Yeah... this is insane, IE 11 will either return a #RRGGBB or a number represented as BGR.
				val bgr = obj.toInt()
				val c = Color()
				c.b = ((bgr and 0xff0000).ushr(16)).toFloat() / 255f
				c.g = ((bgr and 0x00ff00).ushr(8)).toFloat() / 255f
				c.r = ((bgr and 0x0000ff)).toFloat() / 255f
				c.a = 1f
				return c
			} else {
				if (obj is String) {
					if (obj.startsWith("#")) return Color.fromRgbaStr(obj.substring(1))
					else if (obj.startsWith("rgb", ignoreCase = true)) return Color.fromCssStr(obj)
					else return Color.from888Str(obj)
				} else {
					return Color.BLACK.copy()
				}
			}
		}
	}

	final override var hScrollPolicy: ScrollPolicy by observable(ScrollPolicy.OFF, {
		element.style.overflowX = it.toCssString()
	})

	final override var vScrollPolicy: ScrollPolicy by observable(ScrollPolicy.OFF, {
		element.style.overflowY = it.toCssString()
	})

	final override var editable: Boolean by observable(false, {
		element.contentEditable = if (it) "true" else "false"
	})

	val imagePasteHandler = {
		e: Event ->
		e as ClipboardEvent
		if (e.clipboardData != null && (e.clipboardData.items as DataTransferItemList?) != null) {
			for (i in 0..e.clipboardData.items.length - 1) {
				val item = e.clipboardData.items[i]!!
				if (item.kind == "file" && item.type == "image/png") {
					// get the blob
					val imageFile = item.getAsFile() ?: continue

					// read the blob as a data URL
					val fileReader = FileReader()
					fileReader.onloadend = {
						e ->
						// create an image
						val image = document.createElement("img") as HTMLImageElement
						image.src = fileReader.result

						// insert the image
						val win = kotlin.browser.window
						val range = win.getSelection().getRangeAt(0)
						range.insertNode(image)
						range.collapse(false)

						// set the selection to after the image
						val selection = win.getSelection()
						selection.removeAllRanges()
						selection.addRange(range)
					}

					fileReader.readAsDataURL(imageFile)

					// prevent the default paste action
					e.preventDefault()

					// only paste 1 image at a time
					break
				}
			}
		}
	}

	init {
		styleTags.add(EditableTextField)
		focusEnabled = true
		hScrollPolicy = ScrollPolicy.AUTO
		vScrollPolicy = ScrollPolicy.AUTO
		editable = true

		keyDown().add {
			it.handled = true
			if (it.keyCode == Ascii.TAB)
				it.preventDefault()
		}
		keyUp().add { it.handled = true }

		element.style.apply {
			userSelect(true)
		}

		element.addEventListener("paste", imagePasteHandler)
		textCommander.styleWithCSS(true)

		watch(boxStyle) {
			boxStyle.applyCss(element)
			boxStyle.applyBox(native as DomComponent)
		}
		watch(flowStyle) {
			flowStyle.applyCss(element)
		}

		focused().add(this::focusedHandler)
		blurred().add(this::blurredHandler)
	}

	override var htmlText: String?
		get() = element.innerHTML
		set(value) {
			element.innerHTML = value ?: ""
		}

	/**
	 * The last range that was selected when this text field lost focus.
	 */
	protected var lastRange: Range? = null

	private fun focusedHandler() {
		if (lastRange != null) {
			val s = document.getSelection()
			s.removeAllRanges()
			s.addRange(lastRange!!)
			lastRange = null
		}
	}

	private fun blurredHandler() {
		val s = document.getSelection()
		if (s.rangeCount > 0) {
			lastRange = s.getRangeAt(0).cloneRange()
		} else {
			lastRange = null
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		highlight?.setSize(out)
	}

	override fun dispose() {
		super.dispose()
		hScrollModel.dispose()
		vScrollModel.dispose()
		element.removeEventListener("paste", imagePasteHandler)
	}
}

private fun Document.queryCommandValue2(commandId: String): Any {
	val d: dynamic = this
	return d.queryCommandValue(commandId)
}
