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

import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.*
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.input.interaction.click
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.signal.Cancel
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2

open class WindowPanel(owner: Owned) : ElementContainerImpl<UiComponent>(owner), Labelable, Closeable, LayoutDataProvider<StackLayoutData> {

	private val _closing = own(Signal2<Closeable, Cancel>())
	override val closing = _closing.asRo()

	private val _closed = own(Signal1<Closeable>())
	override val closed = _closed.asRo()

	protected val cancel = Cancel()

	val style = bind(WindowPanelStyle())

	protected val textField = addChild(text {
		styleTags.add(TITLE_BAR_STYLE)
	})

	private val contents = addChild(stack {
		styleTags.add(CONTENTS_STYLE)
	})
	private var background: UiComponent? = null
	private var titleBarBackground: UiComponent? = null
	private var closeButton: UiComponent? = null

	override var label: String
		get() = textField.label
		set(value) {
			textField.label = value
		}

	init {
		styleTags.add(WindowPanel)
		watch(style) {
			background?.dispose()
			background = addChild(0, it.background(this))

			titleBarBackground?.dispose()
			titleBarBackground = addChild(1, it.titleBarBackground(this))

			closeButton?.dispose()
			closeButton = addChild(2, it.closeButton(this).apply {
				styleTags.add(TITLE_BAR_STYLE)
				click().add { close() }
			})

		}
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	open fun close() {
		_closing.dispatch(this, cancel.reset())
		if (!cancel.canceled) {
			_closed.dispatch(this)
		}
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		val padding = style.padding
		val titleBarPadding = style.titleBarPadding
		val cS = contents.sizeConstraints
		val tCS = textField.sizeConstraints
		out.width.min = padding.expandWidth(cS.width.min)
		out.width.max = padding.expandWidth(cS.width.max)
		out.height.min = (padding.expandHeight(cS.height.min) ?: 0f) + (titleBarPadding.expandHeight(tCS.height.min) ?: 0f)
		out.set(contents.sizeConstraints)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val padding = style.padding
		val titleBarPadding = style.titleBarPadding
		val closeButton = closeButton!!
		val background = background!!

		textField.setSize(if (explicitWidth == null) null else titleBarPadding.reduceWidth2(explicitWidth - closeButton.width - style.titleBarGap), null)

		val tFH = maxOf(textField.height, closeButton.height)
		textField.moveTo(titleBarPadding.left, titleBarPadding.top + (tFH - textField.height) * 0.5f)
		val titleBarHeight = titleBarPadding.expandHeight2(maxOf(textField.height, closeButton.height))
		val contentsW = explicitWidth
		val contentsH = if (explicitHeight == null) null else explicitHeight - titleBarHeight

		contents.setSize(padding.reduceWidth(contentsW), padding.reduceHeight(contentsH))
		contents.setPosition(padding.left, titleBarHeight + padding.top)
		val measuredWidth = maxOf(titleBarPadding.expandWidth2(textField.width + style.titleBarGap + closeButton.width), padding.expandWidth2(contents.width))
		background.setSize(measuredWidth, padding.expandHeight2(contents.height))

		background.setPosition(0f, titleBarHeight)
		out.set(measuredWidth, titleBarHeight + background.height)
		titleBarBackground?.setSize(out.width, titleBarHeight)
		closeButton.setPosition(out.width - titleBarPadding.right - closeButton.width, titleBarPadding.top)
	}

	companion object : StyleTag {
		val TITLE_BAR_STYLE = styleTag()
		val CONTENTS_STYLE = styleTag()
	}
}

class WindowPanelStyle : StyleBase() {

	override val type: StyleType<WindowPanelStyle> = WindowPanelStyle

	var background by prop(noSkin)
	var titleBarBackground by prop(noSkin)
	var closeButton by prop(noSkin)
	var padding by prop(Pad(6f))
	var titleBarPadding by prop(Pad(5f))
	var titleBarGap by prop(5f)

	companion object : StyleType<WindowPanelStyle>
}

fun Owned.windowPanel(
		init: ComponentInit<WindowPanel> = {}): WindowPanel {
	val p = WindowPanel(this)
	p.init()
	return p
}
