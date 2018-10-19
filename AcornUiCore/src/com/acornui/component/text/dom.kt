//package com.acornui.component.text
//
//import com.acornui.core.Parent
//import com.acornui.math.Bounds
//import com.acornui.math.Vector2
//
//
///**
// * AcornUi must create its own interfaces for dom nodes and operations, to expose what is possible in both JS
// * and JVM versions.
// * @see TextField
// */
//interface Node : Parent<Node> {
//
//	override val parent: Node?
//
//	/**
//	 * The nodeType property can be used to distinguish different kind of nodes, such that elements, text and comments,
//	 * from each other.
//	 * @see NodeType
//	 */
//	val nodeType: Short
//
//	/**
//	 * The name of the current node as a string.
//	 */
//	val nodeName: String
//
//	/**
//	 * The value of this node.
//	 */
//	var nodeValue: String?
//
//	/**
//	 * Represents the text content of a node and its descendants.
//	 */
//	var textContent: String?
//
//	/**
//	 * The Node.normalize() method puts the specified node and all of its sub-tree into a "normalized" form. In a
//	 * normalized sub-tree, no text nodes in the sub-tree are empty and there are no adjacent text nodes.
//	 */
//	fun normalize()
//
//	fun cloneNode(deep: Boolean): Node
//
//	/**
//	 * Tests whether two nodes are equal. Two nodes are equal when they have the same type, defining characteristics
//	 * (for elements, this would be their ID, number of children, and so forth), its attributes match, and so on.
//	 * The specific set of data points that much match varies depending on the types of the nodes.
//	 */
//	fun isEqualNode(otherNode: Node?): Boolean
//
//	/**
//	 * @see DocumentPositionCompare
//	 */
//	fun compareDocumentPosition(other: Node): Short
//
//	/**
//	 * Takes a prefix and returns the namespace URI associated with it on the given node if found (and null if not).
//	 * Supplying null for the prefix will return the default namespace.
//	 */
//	fun lookupNamespaceURI(prefix: String?): String?
//
//	/**
//	 * Returns a String containing the prefix for a given namespace URI, if present, and null if not. When multiple
//	 * prefixes are possible, the result is implementation-dependent.
//	 */
//	fun lookupPrefix(namespace: String?): String?
//
//	/**
//	 * This method checks if the specified [namespace] is the
//	 * default namespace or not.
//	 */
//	fun isDefaultNamespace(namespace: String?): Boolean
//
//}
//
//object NodeType {
//	val ELEMENT_NODE: Short = 1
//	val ATTRIBUTE_NODE: Short = 2
//	val TEXT_NODE: Short = 3
//	val CDATA_SECTION_NODE: Short = 4
//	val ENTITY_REFERENCE_NODE: Short = 5
//	val ENTITY_NODE: Short = 6
//	val PROCESSING_INSTRUCTION_NODE: Short = 7
//	val COMMENT_NODE: Short = 8
//	val DOCUMENT_NODE: Short = 9
//	val DOCUMENT_TYPE_NODE: Short = 10
//	val DOCUMENT_FRAGMENT_NODE: Short = 11
//	val NOTATION_NODE: Short = 12
//}
//
//object DocumentPositionCompare {
//	val DOCUMENT_POSITION_DISCONNECTED: Short = 0x01
//	val DOCUMENT_POSITION_PRECEDING: Short = 0x02
//	val DOCUMENT_POSITION_FOLLOWING: Short = 0x04
//	val DOCUMENT_POSITION_CONTAINS: Short = 0x08
//	val DOCUMENT_POSITION_CONTAINED_BY: Short = 0x10
//	val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short = 0x20
//}
//
//interface Element : Node {
//	var innerHtml: String
//	val namespaceUri: String?
//	val prefix: String?
//	val localName: String
//	var id: String
//
//	var className: String?
//		get() = classList.firstOrNull()
//		set(value) {
//			classList.clear()
//			if (value != null)
//				classList.add(value)
//		}
//
//	val classList: MutableList<String>
//	val attributes: NamedNodeMap
//
//	fun scrollIntoView()
//}
//
//interface NamedNodeMap : MutableList<Attr> {
//	fun getNamedItem(name: String): Attr?
//	fun getNamedItemNS(namespace: String?, localName: String): Attr?
//	fun setNamedItem(attr: Attr): Attr?
//	fun setNamedItemNS(attr: Attr): Attr?
//	fun removeNamedItem(name: String): Attr
//	fun removeNamedItemNS(namespace: String?, localName: String): Attr
//}
//
//interface Attr {
//	val namespaceUri: String?
//	val prefix: String?
//	val localName: String
//	val name: String
//	var value: String
//	var nodeValue: String
//	var textContent: String
//	val ownerElement: Element?
//	val specified: Boolean
//}
//
//interface CharacterData : Node {
//	var data: String
//	val length: Int
//	fun substringData(offset: Int, count: Int): String
//	fun appendData(data: String)
//	fun insertData(offset: Int, data: String)
//	fun deleteData(offset: Int, count: Int)
//	fun replaceData(offset: Int, count: Int, data: String)
//}
//
//interface TextNodeRo : CharacterData {
//	open val wholeText: String
//	fun splitText(offset: Int): TextNodeRo
//}
//
//interface HtmlElement : Element {
//	var title: String
//
//	val dataset: stringMapOf<String>
//
//	var spellcheck: Boolean
//
//	val bounds: Bounds
//	val position: Vector2
//
//	val style: RwCssStyleDeclaration
//	val computedStyle: CssStyleDeclaration
//
//	var contentEditable: Boolean
//
//	fun click()
//	fun focus()
//	fun blur()
//
//	fun forceSpellCheck()
//}
//
//interface CssStyleDeclaration : List<String> {
//	val cssText: String
//	val lastIndex: Int
//		get() = size - 1
//
//	fun getPropertyValue(property: String): String
//	fun getPropertyPriority(property: String): String
//}
//
//interface RwCssStyleDeclaration : CssStyleDeclaration {
//	override var cssText: String
//
//	fun setProperty(property: String, value: String, priority: String = "")
//	fun setPropertyValue(property: String, value: String)
//	fun setPropertyPriority(property: String, priority: String)
//	fun removeProperty(property: String): String
//}