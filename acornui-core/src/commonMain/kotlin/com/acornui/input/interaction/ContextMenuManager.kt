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

import com.acornui.Disposable
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.*
import com.acornui.component.style.*
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.input.*
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.Vector2Ro
import com.acornui.math.vec2
import com.acornui.popup.PopUpInfo
import com.acornui.popup.PopUpManager
import com.acornui.signal.StoppableSignal

class ContextMenuManager(owner: Context) : ContextImpl(owner), Disposable {

	private val contextEvent = ContextMenuEvent()
	private val interactivity by InteractivityManager
	private val popUpManager by PopUpManager

	init {
		stage.rightClick().add(::rightClickHandler)
	}

	private fun rightClickHandler(event: MouseEventRo) {
		if (!event.defaultPrevented()) {
			contextEvent.clear()
			contextEvent.type = ContextMenuEventRo.CONTEXT_MENU
			interactivity.dispatch(contextEvent, event.target)
			if (!contextEvent.defaultPrevented() && contextEvent.menuGroups.isNotEmpty()) {
				event.preventDefault() // Prevent native context menu

				val view = ContextMenuView(stage)
				view.setData(contextEvent.menuGroups)
				popUpManager.addPopUp(PopUpInfo(view, dispose = true))
			}
		}
	}

	override fun dispose() {
		super.dispose()
		stage.rightClick().remove(::rightClickHandler)
	}
}

interface ContextMenuEventRo : EventRo {

	fun addMenuGroup(group: ContextMenuGroup, priority: Float = 0f)

	companion object {

		val CONTEXT_MENU = EventType<ContextMenuEventRo>("contextMenu")
	}
}

class ContextMenuEvent : EventBase(), ContextMenuEventRo {

	private val _menuGroups = ArrayList<ContextMenuGroup>()

	/**
	 * A list of the added menu groups, sorted by their priority.
	 */
	val menuGroups: List<ContextMenuGroup>
		get() = _menuGroups

	private val menuGroupPriorities = ArrayList<Float>()

	override fun addMenuGroup(group: ContextMenuGroup, priority: Float) {
		val index = menuGroupPriorities.sortedInsertionIndex(priority)
		_menuGroups.add(index, group)
		menuGroupPriorities.add(index, priority)
	}

	override fun clear() {
		super.clear()
		_menuGroups.clear()
		menuGroupPriorities.clear()
	}
}

class ContextMenuItem(

		/**
		 * The text to display in the middle column.
		 */
		val text: String,

		/**
		 * If set, this component will be displayed in the left column.
		 */
		val icon: UiComponent? = null,

		/**
		 * If set, the first character this matches in the text will be underlined, and when this menu is open
		 * pressing this character will select this item.
		 */
		val hotLetter: Char? = null,

		/**
		 * If not-empty, there will be a sub-menu for this menu item with the given groups.
		 */
		val children: List<ContextMenuGroup> = emptyList(),

		/**
		 * If set, this context menu item will display a hotkey string representation in the right column.
		 * This does not
		 */
		val hotkey: Hotkey? = null,

		/**
		 * If false, the context menu item will be visible, but not selectable.
		 */
		val enabled: Boolean = true,

		/**
		 * When this context menu item is selected, this callback will be invoked.
		 */
		val onSelected: () -> Unit
) {

	init {
		if (children.isNotEmpty() && hotkey != null)
			throw IllegalArgumentException("Either children or hotkey should be set, not both.")
	}
}

class ContextMenuGroup(val items: List<ContextMenuItem>)

class ContextMenuView(owner: Context) : ContainerImpl(owner) {

	val style = bind(ContextMenuStyle())

	private var background: UiComponent? = null
	private val rowBackgrounds = addChild(container())
	private val contents = addChild(container { interactivityMode = InteractivityMode.NONE })
	private val itemViews = ArrayList<ContextMenuItemView>()

	private var _highlightedRow: ContextMenuItemView? = null

	private val _mousePosition = vec2()

	private val stageMouseMoveHandler = { e: MouseEventRo ->
		updateHighlight()
	}

	init {
		styleTags.add(Companion)

		watch(style) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))

			for (i in 0..itemViews.lastIndex) {
				val itemView = itemViews[i]

				itemView.rightArrow?.dispose()
				itemView.rightArrow = if (itemView.item.children.isEmpty()) null else contents.addElement(it.rightArrow(this))
				itemView.background.dispose()
				itemView.background = rowBackgrounds.addElement(it.rowBackground(this))
			}
		}

		click().add(::clickHandler)
	}

	override fun onActivated() {
		super.onActivated()
		stage.mouseMove().add(stageMouseMoveHandler)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		stage.mouseMove().remove(stageMouseMoveHandler)
	}

	private fun clickHandler(event: MouseEventRo) {
		if (!event.defaultPrevented()) {
			val e = getElementUnderPosition(mousePosition(_mousePosition)) ?: return
			e.item.onSelected()
		}
	}


	private fun updateHighlight() {
		val e = getElementUnderPosition(mousePosition(_mousePosition))
		if (e != _highlightedRow) {
			_highlightedRow?.background?.highlighted = false
			_highlightedRow = e
			e?.background?.highlighted = e?.item?.enabled ?: false
		}
	}

	private fun getElementUnderPosition(p: Vector2Ro): ContextMenuItemView? {
		for (i in 0..itemViews.lastIndex) {
			val bg = itemViews[i].background
			if (p.x >= bg.x && p.y >= bg.y && p.x < bg.right && p.y < bg.bottom) {
				return itemViews[bg.rowIndex]
			}
		}
		return null
	}

	fun setData(value: List<ContextMenuGroup>) {
		validate(ValidationFlags.STYLES)
		contents.clearElements(true)
		rowBackgrounds.clearElements(true)
		itemViews.clear()

		contents.apply {
			for (i in 0..value.lastIndex) {
				val group = value[i]
				if (i > 0) +hr()
				for (j in 0..group.items.lastIndex) {
					val item = group.items[j]
					val label = +text(item.text)
					val hotkey = item.hotkey
					val hotkeyLabel = if (hotkey != null) +text(hotkey.label) else null
					val rightArrow = if (item.children.isEmpty()) null else +style.rightArrow(this)
					val rowBackground = rowBackgrounds.addElement(style.rowBackground(this))
					val itemView = ContextMenuItemView(item, item.icon, label, hotkeyLabel, rightArrow, rowBackground)
					itemViews.add(itemView)
				}
			}
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val cellPadding = style.cellPadding
		var iconsW = 0f
		val gap = style.horizontalGap

		for (i in 0..itemViews.lastIndex) {
			iconsW = maxOf(iconsW, itemViews[i].icon?.width ?: 0f)
		}
		iconsW = cellPadding.expandWidth(iconsW)

		var tipsW = 0f
		for (i in 0..itemViews.lastIndex) {
			val itemView = itemViews[i]
			tipsW = maxOf(tipsW, (itemView.hotkeyLabel ?: itemView.rightArrow)?.width ?: 0f)
		}
		tipsW = cellPadding.expandWidth(tipsW)

		var labelsW = 0f
		for (i in 0..itemViews.lastIndex) {
			val label = itemViews[i].label
			if (explicitWidth != null) {
				label.width(style.padding.reduceWidth(explicitWidth) - iconsW - tipsW - gap * 2f)
			}
			labelsW = maxOf(labelsW, label.width)
		}
		labelsW = cellPadding.expandWidth(labelsW)

		val measuredW = style.padding.expandWidth(iconsW + labelsW + tipsW + gap * 2f)

		var rowY = style.padding.top
		val left = style.padding.left + cellPadding.left
		for (i in 0..itemViews.lastIndex) {
			val itemView = itemViews[i]
			val icon = itemView.icon
			val label = itemView.label
			val hotkeyLabel = itemView.hotkeyLabel
			val rightArrow = itemView.rightArrow
			val cellHeight = itemView.height

			icon?.position(left, rowY + cellPadding.top + (cellHeight - icon.height) / 2f)
			label.position(left + gap + iconsW, rowY + cellPadding.top + (cellHeight - label.height) / 2f)
			val rightColX = left + gap + iconsW + gap + labelsW
			hotkeyLabel?.position(rightColX, rowY + cellPadding.top + (cellHeight - hotkeyLabel.height) / 2f)
			rightArrow?.position(rightColX, rowY + cellPadding.top + (cellHeight - rightArrow.height) / 2f)

			val rowH = cellPadding.expandHeight(cellHeight)
			itemView.background.size(measuredW, rowH)
			itemView.background.position(0f, rowY)

			val rowBackground = itemView.background
			rowBackground.rowIndex = i
			rowBackground.highlighted = _highlightedRow == itemView && itemView.item.enabled
			rowBackground.size(measuredW, rowH)
			rowBackground.position(0f, rowY)

			rowY += rowH
		}

		out.set(measuredW, rowY + style.padding.bottom)

		background?.size(out.width, out.height)
	}

	companion object : StyleTag

}

private class ContextMenuItemView(
		val item: ContextMenuItem,
		val icon: UiComponent?,
		val label: UiComponent,
		val hotkeyLabel: UiComponent?,
		var rightArrow: UiComponent?,
		var background: RowBackground
) {

	val height: Float
		get() {
			return maxOf(icon?.height ?: 0f, label.height, (hotkeyLabel ?: rightArrow)?.height ?: 0f)
		}
}

class ContextMenuStyle : StyleBase() {

	override val type: StyleType<ContextMenuStyle> = ContextMenuStyle

	var background by prop(noSkinOptional)

	var rightArrow by prop(noSkin)

	var padding by prop(Pad(5f))

	var horizontalGap by prop(5f)

	var cellPadding by prop(Pad(0f))

	/**
	 * The background for each row.
	 */
	var rowBackground by prop<Context.() -> RowBackground>({ rowBackground() })

	companion object : StyleType<ContextMenuStyle>

}

/**
 * Dispatched when the mouse or touch is pressed down on this element.
 */
fun UiComponentRo.contextMenu(isCapture: Boolean = false): StoppableSignal<ContextMenuEventRo> {
	return createOrReuse(ContextMenuEventRo.CONTEXT_MENU, isCapture)
}
