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

import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.*
import com.acornui.component.text.text
import com.acornui.di.Context

import com.acornui.input.interaction.click
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.signal.Cancel
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2

open class WindowPanel(owner: Context) : ElementContainerImpl<UiComponent>(owner), Labelable, Closeable, LayoutDataProvider<StackLayoutData> {

	private val _closing = own(Signal2<Closeable, Cancel>())
	override val closing = _closing.asRo()

	private val _closed = own(Signal1<Closeable>())
	override val closed = _closed.asRo()

	protected val cancel = Cancel()

	val style = bind(WindowPanelStyle())

	protected val textField = addChild(text {
		addClass(TITLE_BAR_STYLE)
	})

	private val contents = addChild(stack {
		addClass(CONTENTS_STYLE)
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
		addClass(WindowPanel)
		watch(style) {
			background?.dispose()
			background = addChild(0, it.background(this))

			titleBarBackground?.dispose()
			titleBarBackground = addChild(1, it.titleBarBackground(this))

			closeButton?.dispose()
			closeButton = addChild(2, it.closeButton(this).apply {
				addClass(TITLE_BAR_STYLE)
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
		if (!cancel.isCancelled) {
			_closed.dispatch(this)
		}
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val padding = style.padding
		val titleBarPadding = style.titleBarPadding
		val closeButton = closeButton!!
		val background = background!!

		textField.size(if (explicitWidth == null) null else titleBarPadding.reduceWidth(explicitWidth - closeButton.width - style.titleBarGap), null)

		val tFH = maxOf(textField.height, closeButton.height)
		textField.position(titleBarPadding.left, titleBarPadding.top + (tFH - textField.height) * 0.5)
		val titleBarHeight = titleBarPadding.expandHeight(maxOf(textField.height, closeButton.height))
		val contentsW = explicitWidth
		val contentsH = if (explicitHeight == null) null else explicitHeight - titleBarHeight

		contents.size(padding.reduceWidth(contentsW), padding.reduceHeight(contentsH))
		contents.position(padding.left, titleBarHeight + padding.top)
		val measuredWidth = maxOf(titleBarPadding.expandWidth(textField.width + style.titleBarGap + closeButton.width), padding.expandWidth(contents.width))
		background.size(measuredWidth, padding.expandHeight(contents.height))

		background.position(0.0, titleBarHeight)
		out.set(measuredWidth, titleBarHeight + background.height)
		titleBarBackground?.size(out.width, titleBarHeight)
		closeButton.position(out.width - titleBarPadding.right - closeButton.width, titleBarPadding.top)
	}

	companion object : StyleTag {
		val TITLE_BAR_STYLE = styleTag()
		val CONTENTS_STYLE = styleTag()
	}
}

class WindowPanelStyle : ObservableBase() {

	override val type: StyleType<WindowPanelStyle> = WindowPanelStyle

	var background by prop(noSkin)
	var titleBarBackground by prop(noSkin)
	var closeButton by prop(noSkin)
	var padding by prop(Pad(6.0))
	var titleBarPadding by prop(Pad(5.0))
	var titleBarGap by prop(5.0)

	companion object : StyleType<WindowPanelStyle>
}

fun Context.windowPanel(
		init: ComponentInit<WindowPanel> = {}): WindowPanel {
	val p = WindowPanel(this)
	p.init()
	return p
}