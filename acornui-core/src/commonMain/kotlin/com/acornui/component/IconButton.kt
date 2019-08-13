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

package com.acornui.component

import com.acornui.collection.mapTo
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.VAlign
import com.acornui.component.style.StyleTag
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.MathUtils.offsetRound
import com.acornui.math.MathUtils.roundToNearest
import com.acornui.math.Pad
import com.acornui.math.PadRo

class IconButton(
		owner: Owned
) : ButtonImpl(owner), SingleElementContainer<UiComponent> {

	init {
		styleTags.add(IconButton)
	}

	private var _iconMap: Map<ButtonState, UiComponent>? = null

	/**
	 * Sets a map of icons to use.
	 * @throws IllegalArgumentException If the map does not contain the key [ButtonState.UP]
	 */
	fun iconMap(map: Map<ButtonState, UiComponent>) {
		if (!map.containsKey(ButtonState.UP)) throw IllegalArgumentException("iconMap must at least set the icon for the UP state.")
		element = null
		_iconMap = map
		refreshContents()
	}

	override var element: UiComponent? by validationProp(null, ValidationFlags.PROPERTIES)

	override fun updateProperties() {
		super.updateProperties()
		refreshContents()
	}

	private var _contentsContainer: SingleElementContainer<UiComponent>? = null

	private fun getContents(): UiComponent? {
		val iconMap = _iconMap
		if (iconMap != null) {
			return currentState.fallbackWalk {
				iconMap[it]
			}
		}
		return element
	}

	private fun refreshContents() {
		val contents = getContents()
		@Suppress("UNCHECKED_CAST")
		val currentContentsContainer = skin as? SingleElementContainer<UiComponent>
		if (currentContentsContainer != null && currentContentsContainer.element != contents) {
			_contentsContainer?.element = null
			_contentsContainer = currentContentsContainer
			currentContentsContainer.element = contents
		}
	}

	companion object : StyleTag
}

fun Owned.iconButton(init: ComponentInit<IconButton> = {}): IconButton {
	val b = IconButton(this)
	b.init()
	return b
}

fun Owned.iconButton(imagePath: String, init: ComponentInit<IconButton> = {}): IconButton {
	return IconButton(this).apply {
		element = iconImage(imagePath)
		init()
	}
}

fun Owned.iconButton(atlasPath: String, region: String, init: ComponentInit<IconButton> = {}): IconButton {
	return IconButton(this).apply {
		element = iconAtlas(atlasPath, region)
		init()
	}
}

fun Owned.iconButton(atlasPath: String, regions: Map<ButtonState, String>, init: ComponentInit<IconButton> = {}): IconButton {
	val b = IconButton(this)
	b.iconMap(regions.mapTo { key, value ->
		key to atlas(atlasPath, value)
	})
	b.init()
	return b
}


/**
 * A typical implementation of a skin part for an icon button state.
 */
@Deprecated("Use IconButtonSkin")
open class IconButtonSkinPart(
		owner: Owned,
		private val texture: UiComponent,
		private val padding: PadRo = Pad(4f),
		private val hGap: Float = 4f,

		/**
		 * The vertical alignment between the icon and the label.
		 */
		private val vAlign: VAlign = VAlign.MIDDLE,

		/**
		 * If false, the icon will be on the right instead of left.
		 */
		private val iconOnLeft: Boolean = true
) : SingleElementContainerImpl<UiComponent>(owner), Labelable {

	private val icon: Image
	private val textField: TextField

	init {
		addChild(texture)
		icon = addChild(image())
		textField = addChild(text {
			interactivityMode = InteractivityMode.NONE
		})
	}

	override var label: String
		get() = textField.text
		set(value) {
			textField.text = value
		}

	override fun onElementChanged(oldElement: UiComponent?, newElement: UiComponent?) {
		icon.element = newElement
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
		val contentWidth = roundToNearest(if (label == "") icon.width else icon.width + hGap + textField.width, 2f)
		val contentHeight = roundToNearest(if (label == "") icon.height else maxOf(textField.height, icon.height), 2f)
		val w = maxOf(padding.expandWidth2(contentWidth), explicitWidth ?: 4f)
		val h = maxOf(padding.expandHeight2(contentHeight), explicitHeight ?: 4f)

		texture.setSize(w, h)

		val iconX: Float
		val textFieldX: Float
		if (iconOnLeft) {
			iconX = if (childAvailableWidth != null) {
				(childAvailableWidth - contentWidth) * 0.5f + padding.left
			} else {
				padding.left
			}
			textFieldX = offsetRound(iconX + icon.width + hGap)
		} else {
			textFieldX = if (childAvailableWidth != null) {
				(childAvailableWidth - contentWidth) * 0.5f + padding.left
			} else {
				padding.left
			}
			iconX = textFieldX + textField.width + hGap
		}

		val yOffset = if (childAvailableHeight == null) padding.top else (childAvailableHeight - contentHeight) * 0.5f + padding.top

		val baseline = yOffset + textField.baseline
		val iconY: Float
		val textFieldY: Float
		when (vAlign) {
			VAlign.TOP -> {
				iconY = yOffset
				textFieldY = yOffset
			}
			VAlign.MIDDLE -> {
				iconY = yOffset + (contentHeight - icon.height) * 0.5f
				textFieldY = (yOffset + (contentHeight - textField.height) * 0.5f)
			}
			VAlign.BOTTOM -> {
				iconY = yOffset + (contentHeight - icon.height)
				textFieldY = yOffset + (contentHeight - textField.height)
			}
			VAlign.BASELINE -> {
				iconY = baseline - icon.baseline
				textFieldY = baseline - textField.baseline
			}
		}
		icon.moveTo(iconX, iconY)
		textField.moveTo(textFieldX, textFieldY)

		out.set(w, h, textField.baselineY)
	}
}