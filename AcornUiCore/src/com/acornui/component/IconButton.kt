package com.acornui.component

import com.acornui.collection.mapTo
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.VAlign
import com.acornui.component.style.StyleTag
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.core.graphics.atlas
import com.acornui.core.graphics.contentsAtlas
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.math.round

class IconButton(
		owner: Owned
) : Button(owner) {

	init {
		styleTags.add(IconButton)
	}

	private var _iconMap: Map<ButtonState, UiComponent>? = null

	/**
	 * Sets a map of icons to use.
	 * ButtonState.UP must be set in the map.
	 */
	fun iconMap(map: Map<ButtonState, UiComponent>) {
		if (!map.containsKey(ButtonState.UP)) throw IllegalArgumentException("iconMap must at least set the icon for the UP state.")
		clearElements(dispose = true)
		_iconMap = map
		refreshContents()
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		if (elements.size > 1) throw Exception("Icon buttons can only have one child.")
		refreshContents()
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		refreshContents()
	}

	override fun onCurrentStateChanged(previousState: ButtonState, newState: ButtonState, previousSkinPart: UiComponent?, newSkinPart: UiComponent?) {
		refreshContents()
	}

	private var _contentsContainer: ElementContainer<UiComponent>? = null

	private fun getContents(): UiComponent? {
		val iconMap = _iconMap
		if (iconMap != null) {
			return currentState.backupWalk {
				iconMap[it]
			}
		}
		return elements.getOrNull(0)
	}

	private fun refreshContents() {
		val contents = getContents()
		@Suppress("UNCHECKED_CAST")
		val currentContentsContainer = currentSkinPart as? ElementContainer<UiComponent>
		if (currentContentsContainer != null && currentContentsContainer.elements.getOrNull(0) != contents) {
			_contentsContainer?.clearElements(dispose = false)
			_contentsContainer = currentContentsContainer
			currentContentsContainer.addOptionalElement(contents)
		}
	}

	companion object : StyleTag
}

fun Owned.iconButton(init: ComponentInit<IconButton> = {}): IconButton {
	val b = IconButton(this)
	b.init()
	return b
}

fun Owned.iconButton(icon: String, init: ComponentInit<IconButton> = {}): IconButton {
	val b = IconButton(this)
	b.contentsImage(icon)
	b.init()
	return b
}

fun Owned.iconButton(atlasPath: String, region: String, init: ComponentInit<IconButton> = {}): IconButton {
	val b = IconButton(this)
	b.contentsAtlas(atlasPath, region)
	b.init()
	return b
}

fun Owned.iconButton(atlasPath: String, regions: Map<ButtonState, String>, init: ComponentInit<IconButton> = {}): IconButton {
	val b = IconButton(this)
	b.iconMap(regions.mapTo {
		key, value ->
		key to atlas(atlasPath, value)
	})
	b.init()
	return b
}


/**
 * A typical implementation of a skin part for an icon button state.
 */
open class IconButtonSkinPart(
		owner: Owned,
		private val texture: UiComponent,
		private val padding: PadRo = Pad(5f, 5f, 5f, 5f),
		private val hGap: Float = 5f,

		/**
		 * The vertical alignment between the icon and the label.
		 */
		private val vAlign: VAlign = VAlign.MIDDLE
) : ElementContainerImpl<UiComponent>(owner), Labelable {

	private val icon: Image
	private val textField: TextField

	init {
		addChild(texture)
		icon = addChild(image())
		textField = addChild(text())
	}

	override var label: String
		get() = textField.text
		set(value) {
			textField.text = value
		}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		icon.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		icon.removeElement(element)
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		out.width.min = icon.width + padding.left + padding.right
		out.height.min = icon.height + padding.top + padding.bottom
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)
		val textWidth = if (childAvailableWidth == null) null else childAvailableWidth - icon.width - hGap
		textField.setSize(textWidth, childAvailableHeight)
		val contentWidth = if (label == "") icon.width else icon.width + hGap + textField.width
		val contentHeight = if (label == "") icon.height else maxOf(textField.height, icon.height)
		val w = maxOf(contentWidth + padding.left + padding.right, explicitWidth ?: 4f)
		val h = maxOf(contentHeight + padding.top + padding.bottom, explicitHeight ?: 4f)

		texture.setSize(w, h)
		out.set(w, h)

		if (childAvailableWidth != null) {
			icon.x = ((childAvailableWidth - contentWidth) * 0.5f + padding.left)
		} else {
			icon.x = (padding.left)
		}
		textField.x = round(icon.x + icon.width + hGap)

		val yOffset = if (childAvailableHeight == null) padding.top else (childAvailableHeight - contentHeight) * 0.5f + padding.top

		when (vAlign) {
			VAlign.TOP -> {
				icon.y = yOffset
				textField.y = yOffset
			}
			VAlign.MIDDLE -> {
				icon.y = yOffset + (contentHeight - icon.height) * 0.5f
				textField.y = round((yOffset + (contentHeight - textField.height) * 0.5f))
			}
			VAlign.BOTTOM -> {
				icon.y = yOffset + (contentHeight - icon.height)
				textField.y = round(yOffset + (contentHeight - textField.height))
			}
		}
	}
}
