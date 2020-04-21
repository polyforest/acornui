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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.component

import com.acornui.NodeRo
import com.acornui.ExperimentalAcorn
import com.acornui.component.layout.algorithm.hGroup
import com.acornui.component.layout.algorithm.vGroup
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.noSkin
import com.acornui.component.text.text
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.function.as1
import com.acornui.input.interaction.click
import com.acornui.math.Bounds
import com.acornui.observe.Observable
import com.acornui.signal.Cancel
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3
import com.acornui.text.StringFormatter
import com.acornui.text.ToStringFormatter

/**
 * A Tree component represents a hierarchy of parent/children relationships.
 */
@ExperimentalAcorn
class Tree<E : NodeRo>(owner: Context, rootFactory: (tree: Tree<E>) -> TreeItemRenderer<E> = { DefaultTreeItemRenderer(it, it) }) : ContainerImpl(owner) {

	private val toggledChangeRequestedCancel = Cancel()

	private val _nodeToggledChanging = own(Signal3<E, Boolean, Cancel>())

	/**
	 * A tree node toggled change is being requested. This may be prevented by calling [Cancel.cancel].
	 */
	val nodeToggledChanging = _nodeToggledChanging.asRo()

	private val _nodeToggledChanged = own(Signal2<E, Boolean>())

	/**
	 * A tree node toggled value has changed.
	 * @see getNodeToggled
	 */
	val nodeToggledChanged = _nodeToggledChanged.asRo()

	private val _root: TreeItemRenderer<E> = addChild(rootFactory(this))
	val root: TreeItemRendererRo<E>
		get() = _root

	var data: E?
		get() = _root.data
		set(value) {
			_root.data = value
		}

	var formatter: StringFormatter<E> by validationProp(ToStringFormatter, ValidationFlags.PROPERTIES)

	init {
		// Consider PROPERTIES to be a cascading flag
		cascadingFlags = cascadingFlags or ValidationFlags.PROPERTIES
		styleTags.add(Companion)
	}

	/**
	 * Requests that a node be toggled. A toggled node will be expanded and its children shown.
	 */
	fun setNodeToggled(node: E, toggled: Boolean) {
		val renderer = _root.findElement { it.data == node } ?: return
		if (renderer.toggled == toggled) return
		_nodeToggledChanging.dispatch(node, toggled, toggledChangeRequestedCancel.reset())
		if (toggledChangeRequestedCancel.canceled) return
		renderer.toggled = toggled
		_nodeToggledChanged.dispatch(node, toggled)
	}

	/**
	 * Returns true if the renderer for the given node is currently opened.
	 * (This returns false if the node could not be found.
	 */
	fun getNodeToggled(node: E): Boolean {
		return _root.findElement { it.data == node }?.toggled == true
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		_root.size(explicitWidth, explicitHeight)
		out.set(_root.bounds)
	}

	private fun TreeItemRenderer<E>.findElement(callback: (TreeItemRenderer<E>) -> Boolean): TreeItemRenderer<E>? {
		if (callback(this)) return this
		for (i in 0..elements.lastIndex) {
			val found = elements[i].findElement(callback)
			if (found != null) return found
		}
		return null
	}

	companion object : StyleTag
}

@ExperimentalAcorn
interface TreeItemRendererRo<out E : NodeRo> : ItemRendererRo<E>, ToggleableRo {

	val elements: List<TreeItemRendererRo<E>>

}

@ExperimentalAcorn
interface TreeItemRenderer<E : NodeRo> : TreeItemRendererRo<E>, ItemRenderer<E>, Toggleable {

	override val elements: List<TreeItemRenderer<E>>

}

@ExperimentalAcorn
open class DefaultTreeItemRenderer<E : NodeRo>(owner: Context, private val tree: Tree<E>) : ContainerImpl(owner), TreeItemRenderer<E> {

	val style = bind(DefaultTreeItemRendererStyle())

	protected var openedFolderIcon: UiComponent? = null
	protected var closedFolderIcon: UiComponent? = null
	protected var leafIcon: UiComponent? = null

	protected val hGroup = addChild(hGroup())
	protected val textField = hGroup.addElement(text())
	protected val childrenContainer = addChild(vGroup<TreeItemRenderer<E>>())

	private val _elements = ArrayList<TreeItemRenderer<E>>()
	override val elements: List<TreeItemRenderer<E>>
		get() = _elements

	init {
		validation.addNode(ValidationFlags.PROPERTIES, dependencies = ValidationFlags.STYLES, dependents = ValidationFlags.LAYOUT, onValidate = ::updateProperties)
		cascadingFlags = cascadingFlags or ValidationFlags.PROPERTIES
		styleTags.add(Companion)

		hGroup.cursor(StandardCursor.HAND)
		hGroup.click().add {
			val d = _data
			if (d != null) {
				if (!isLeaf) tree.setNodeToggled(d, !toggled)
			}
		}

		watch(style) {
			openedFolderIcon?.dispose()
			openedFolderIcon = hGroup.addElement(0, it.openedFolderIcon(this))
			closedFolderIcon?.dispose()
			closedFolderIcon = hGroup.addElement(0, it.closedFolderIcon(this))
			leafIcon?.dispose()
			leafIcon = if (it.useLeaf) hGroup.addElement(0, it.leafIcon(this)) else null
		}
	}

	private var _data: E? = null
	override var data: E?
		get() = _data
		set(value) {
			val oldData = _data
			if (oldData == value) return
			toggled = false
			if (oldData is Observable) {
				oldData.changed.remove(::invalidateProperties.as1)
			}
			_data = value
			if (value is Observable) {
				value.changed.add(::invalidateProperties.as1)
			}
			invalidateProperties()
		}

	override var toggled: Boolean by validationProp(false, ValidationFlags.PROPERTIES)

	protected open val isLeaf: Boolean
		get() = _data == null || _data!!.children.isEmpty()

	private fun updateProperties() {
		openedFolderIcon?.visible = false
		closedFolderIcon?.visible = false
		leafIcon?.visible = false
		if (style.useLeaf && isLeaf) {
			leafIcon?.visible = true
		} else {
			if (toggled) {
				openedFolderIcon?.visible = true
			} else {
				closedFolderIcon?.visible = true
			}
		}
		updateText()
		updateChildren()
	}

	protected open fun updateText() {
		textField.text = if (_data == null) "" else tree.formatter.format(_data!!)
	}

	protected open fun updateChildren() {
		@Suppress("UNCHECKED_CAST")
		childrenContainer.recycleItemRenderers(_data?.children as List<E>) {
			DefaultTreeItemRenderer(this, tree)
		}
		childrenContainer.visible = toggled
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		hGroup.size(explicitWidth, null)
		childrenContainer.size(
				if (explicitWidth == null) null else explicitWidth - style.indent,
				if (explicitHeight == null) null else explicitHeight - hGroup.height - style.verticalGap)

		childrenContainer.position(style.indent, hGroup.height + style.verticalGap)
		if (toggled && childrenContainer.elements.isNotEmpty())
			out.set(maxOf(childrenContainer.right, hGroup.right), childrenContainer.bottom)
		else
			out.set(hGroup.right, hGroup.bottom)
	}

	override fun dispose() {
		super.dispose()
		data = null
	}

	companion object : StyleTag
}

open class DefaultTreeItemRendererStyle : StyleBase() {

	override val type: StyleType<DefaultTreeItemRendererStyle> = Companion

	var openedFolderIcon by prop(noSkin)
	var closedFolderIcon by prop(noSkin)
	var leafIcon by prop(noSkin)

	/**
	 * If the node has no children, consider the node to be a leaf, and use the leaf icon.
	 */
	var useLeaf by prop(true)

	/**
	 * The number of pixels to indent from the left for child nodes.
	 */
	var indent by prop(5f)

	var verticalGap by prop(5f)

	companion object : StyleType<DefaultTreeItemRendererStyle>
}

@ExperimentalAcorn
fun <E : NodeRo> Context.tree(rootFactory: (tree: Tree<E>) -> TreeItemRenderer<E> = { DefaultTreeItemRenderer(it, it) }, init: ComponentInit<Tree<E>> = {}): Tree<E> {
	val tree = Tree(this, rootFactory)
	tree.init()
	return tree
}

/**
 * A simple data model representing the most rudimentary tree node.
 */
@ExperimentalAcorn
open class TreeNode(label: String) : NodeRo, Observable {

	private val _changed = Signal1<TreeNode>()
	override val changed = _changed.asRo()

	/**
	 * Syntax sugar for addChild.
	 */
	operator fun TreeNode.unaryPlus(): TreeNode {
		this@TreeNode.addChild(this@TreeNode.children.size, this)
		return this
	}

	override var parent: TreeNode? = null

	private val _children = ArrayList<TreeNode>()
	override val children: List<TreeNode>
		get() = _children

	fun <S : TreeNode> addChild(index: Int, child: S): S {
		child.parent = this
		_children.add(index, child)
		_changed.dispatch(this)
		return child
	}

	fun <S : TreeNode> addChild(child: S): S = addChild(children.size, child)

	fun removeChild(index: Int): TreeNode {
		val c = _children.removeAt(index)
		c.parent = null
		_changed.dispatch(this)
		return c
	}

	fun removeChild(child: TreeNode): Boolean {
		val index = children.indexOf(child)
		if (index == -1) return false
		removeChild(index)
		return true
	}

	private var _label: String = label
	var label: String
		get() = _label
		set(value) {
			if (value == _label) return
			_label = value
			_changed.dispatch(this)
		}

	/**
	 * @see Tree.formatter
	 */
	override fun toString(): String = label


}

@ExperimentalAcorn
fun treeNode(label: String, init: TreeNode.() -> Unit = {}): TreeNode {
	val treeNode = TreeNode(label)
	treeNode.init()
	return treeNode
}