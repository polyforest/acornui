package com.acornui.component.text

import com.acornui.component.BoxStyle
import com.acornui.component.scroll.ClampedScrollModel
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.component.style.StyleTag
import com.acornui.core.focus.Focusable
import com.acornui.graphics.Color

/**
 * An EditableTextField is a TextField that allows its content to be edited. This is different from TextArea in that
 * this component allows mixed content as opposed to plain text.
 */
interface EditableTextField : Focusable, TextField {

	val boxStyle: BoxStyle

	/**
	 * True if this component is editable (default - true)
	 */
	var editable: Boolean

	val hScrollModel: ClampedScrollModel
	val vScrollModel: ClampedScrollModel
	var hScrollPolicy: ScrollPolicy
	var vScrollPolicy: ScrollPolicy

	val textCommander: TextCommander

	companion object : StyleTag
}

interface TextCommander {

	/**
	 * Executes the following text command for this editable text.
	 * @return Returns true if the command was supported.
	 */
	fun exec(commandName: String, value: String): Boolean

	/**
	 * Queries the current state of the selected text as a boolean.
	 */
	fun queryBool(commandId: String): Boolean

	/**
	 * Queries the current state of the selected text as a string.
	 */
	fun queryString(commandId: String): String

	/**
	 * Queries the current state of the selected text as a color.
	 */
	fun queryColor(commandId: String): Color
}

fun TextCommander.exec(command: TextCommand, value: String = "") = exec(command.name, value)
fun TextCommander.queryBool(command: TextCommand): Boolean = queryBool(command.name)
fun TextCommander.queryString(command: TextCommand): String = queryString(command.name)
fun TextCommander.queryColor(command: TextCommand): Color = queryColor(command.name)

fun TextCommander.backColor(color: Color): Boolean = exec(TextCommand.backColor, "#" + color.toRgbString())
fun TextCommander.bold(): Boolean = exec(TextCommand.bold)
fun TextCommander.copy(): Boolean = exec(TextCommand.copy)
fun TextCommander.createLink(uri: String): Boolean = exec(TextCommand.createLink, uri)
fun TextCommander.cut(): Boolean = exec(TextCommand.cut)
fun TextCommander.fontName(fontName: String): Boolean = exec(TextCommand.fontName, fontName)
fun TextCommander.fontSize(fontSize: Int): Boolean = exec(TextCommand.fontSize, fontSize.toString())
fun TextCommander.foreColor(color: Color): Boolean = exec(TextCommand.foreColor, "#" + color.toRgbString())
fun TextCommander.formatBlock(blockTag: String): Boolean = exec(TextCommand.formatBlock, blockTag)
fun TextCommander.indent(): Boolean = exec(TextCommand.indent)
fun TextCommander.insertHorizontalRule(): Boolean = exec(TextCommand.insertHorizontalRule)
fun TextCommander.insertImage(imageSource: String): Boolean = exec(TextCommand.insertImage, imageSource)
fun TextCommander.insertOrderedList(): Boolean = exec(TextCommand.insertOrderedList)
fun TextCommander.insertUnorderedList(): Boolean = exec(TextCommand.insertUnorderedList)
fun TextCommander.insertParagraph(): Boolean = exec(TextCommand.insertParagraph)
fun TextCommander.insertText(text: String): Boolean = exec(TextCommand.insertText, text)
fun TextCommander.insertHtml(html: String): Boolean = exec(TextCommand.insertHTML, html)
fun TextCommander.italic(): Boolean = exec(TextCommand.italic)
fun TextCommander.justifyCenter(): Boolean = exec(TextCommand.justifyCenter)
fun TextCommander.justifyFull(): Boolean = exec(TextCommand.justifyFull)
fun TextCommander.justifyLeft(): Boolean = exec(TextCommand.justifyLeft)
fun TextCommander.justifyRight(): Boolean = exec(TextCommand.justifyRight)
fun TextCommander.outdent(): Boolean = exec(TextCommand.outdent)
fun TextCommander.paste(): Boolean = exec(TextCommand.paste)
fun TextCommander.redo(): Boolean = exec(TextCommand.redo)
fun TextCommander.removeFormat(): Boolean = exec(TextCommand.removeFormat)
fun TextCommander.selectAll(): Boolean = exec(TextCommand.selectAll)
fun TextCommander.strikeThrough(): Boolean = exec(TextCommand.strikeThrough)
fun TextCommander.subscript(): Boolean = exec(TextCommand.subscript)
fun TextCommander.superscript(): Boolean = exec(TextCommand.superscript)
fun TextCommander.underline(): Boolean = exec(TextCommand.underline)
fun TextCommander.undo(): Boolean = exec(TextCommand.undo)
fun TextCommander.unlink(): Boolean = exec(TextCommand.unlink)
fun TextCommander.hiliteColor(color: Color): Boolean = exec(TextCommand.hiliteColor, "#" + color.toRgbString())
fun TextCommander.styleWithCSS(value: Boolean): Boolean = exec(TextCommand.styleWithCSS, value.toString())

enum class TextCommand {
	// Changes the document background color. In styleWithCss mode, it affects the background color of the containing block instead. This requires a <color> value string to be passed in as a value argument. Note that Internet Explorer uses this to set the text background color.
	backColor,
	// Toggles bold on/off for the selection or at the insertion point. Internet Explorer uses the <strong> tag instead of <b>.
	bold,
	//Makes the content document either read-only or editable. This requires a boolean true/false to be passed in as a value argument. (Not supported by Internet Explorer.)
	contentReadOnly,
	//Copies the current selection to the clipboard. Conditions of having this behavior enabled vary from one browser to another, and have evolved over time. Check the compatibility table to determine if you can use it in your case.
	copy,
	//Creates an anchor link from the selection, only if there is a selection. This requires the HREF URI string to be passed in as a value argument. The URI must contain at least a single character, which may be a white space. (Internet Explorer will create a link with a null URI value.)
	createLink,
	//Cuts the current selection and copies it to the clipboard. Conditions of having this behavior enabled vary from one browser to another, and have evolved over time. Check the compatibility table for knowing if you can use it in your case.
	cut,
	//Adds a <small> tag around the selection or at the insertion point. (Not supported by Internet Explorer.)
	decreaseFontSize,
	//Deletes the current selection.
	delete,
	//Enables or disables the table row and column insertion and deletion controls. (Not supported by Internet Explorer.)
	enableInlineTableEditing,
	//Enables or disables the resize handles on images and other resizable objects. (Not supported by Internet Explorer.)
	enableObjectResizing,
	//Changes the font name for the selection or at the insertion point. This requires a font name string ("Arial" for example) to be passed in as a value argument.
	fontName,
	//Changes the font size for the selection or at the insertion point. This requires an HTML font size (1-7) to be passed in as a value argument.
	fontSize,
	//Changes a font color for the selection or at the insertion point. This requires a color value string to be passed in as a value argument.
	foreColor,
	//Adds an HTML block-style tag around the line containing the current selection, replacing the block element containing the line if one exists (in Firefox, BLOCKQUOTE is the exception - it will wrap any containing block element). Requires a tag-name string to be passed in as a value argument. Virtually all block style tags can be used (eg. "H1", "P", "DL", "BLOCKQUOTE"). (Internet Explorer supports only heading tags H1 - H6, ADDRESS, and PRE, which must also include the tag delimiters < >, such as "<H1>".)
	formatBlock,
	//Deletes the character ahead of the cursor's position.  It is the same as hitting the delete key.
	forwardDelete,
	//Adds a heading tag around a selection or insertion point line. Requires the tag-name string to be passed in as a value argument (i.e. "H1", "H6"). (Not supported by Internet Explorer and Safari.)
	heading,
	//Changes the background color for the selection or at the insertion point. Requires a color value string to be passed in as a value argument. UseCSS must be turned on for this to function. (Not supported by Internet Explorer.)
	hiliteColor,
	//Adds a BIG tag around the selection or at the insertion point. (Not supported by Internet Explorer.)
	increaseFontSize,
	//Indents the line containing the selection or insertion point. In Firefox, if the selection spans multiple lines at different levels of indentation, only the least indented lines in the selection will be indented.
	indent,
	//Controls whether the Enter key inserts a br tag or splits the current block element into two. (Not supported by Internet Explorer.)
	insertBrOnReturn,
	//Inserts a horizontal rule at the insertion point (deletes selection).
	insertHorizontalRule,
	//Inserts an HTML string at the insertion point (deletes selection). Requires a valid HTML string to be passed in as a value argument. (Not supported by Internet Explorer.)
	insertHTML,
	//Inserts an image at the insertion point (deletes selection). Requires the image SRC URI string to be passed in as a value argument. The URI must contain at least a single character, which may be a white space. (Internet Explorer will create a link with a null URI value.)
	insertImage,
	//Creates a numbered ordered list for the selection or at the insertion point.
	insertOrderedList,
	//Creates a bulleted unordered list for the selection or at the insertion point.
	insertUnorderedList,
	//Inserts a paragraph around the selection or the current line. (Internet Explorer inserts a paragraph at the insertion point and deletes the selection.)
	insertParagraph,
	//Inserts the given plain text at the insertion point (deletes selection).
	insertText,
	//Toggles italics on/off for the selection or at the insertion point. (Internet Explorer uses the EM tag instead of I.)
	italic,
	//Centers the selection or insertion point.
	justifyCenter,
	//Justifies the selection or insertion point.
	justifyFull,
	//Justifies the selection or insertion point to the left.
	justifyLeft,
	//Right-justifies the selection or the insertion point.
	justifyRight,
	//Outdents the line containing the selection or insertion point.
	outdent,
	//Pastes the clipboard contents at the insertion point (replaces current selection). Clipboard capability must be enabled in the user.js preference file. See [1].
	paste,
	//Redoes the previous undo command.
	redo,
	//Removes all formatting from the current selection.
	removeFormat,
	//Selects all of the content of the editable region.
	selectAll,
	//Toggles strikethrough on/off for the selection or at the insertion point.
	strikeThrough,
	//Toggles subscript on/off for the selection or at the insertion point.
	subscript,
	//Toggles superscript on/off for the selection or at the insertion point.
	superscript,
	//Toggles underline on/off for the selection or at the insertion point.
	underline,
	//Undoes the last executed command.
	undo,
	//Removes the anchor tag from a selected anchor link.
	unlink,
	//Replaces the useCSS command; argument works as expected, i.e. true modifies/generates style attributes in markup, false generates formatting elements.
	styleWithCSS
}