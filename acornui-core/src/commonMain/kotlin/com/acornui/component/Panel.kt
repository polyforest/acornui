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
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.signal.Cancel
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2

open class Panel(
		owner: Owned
) : ElementContainerImpl<UiComponent>(owner), Closeable, LayoutDataProvider<StackLayoutData> {

	override val closing = own(Signal2<Closeable, Cancel>())
	override val closed = own(Signal1<Closeable>())
	protected val cancel = Cancel()

	val style = bind(PanelStyle())

	private val contents = addChild(stack())
	private var background: UiComponent? = null

	var contentsInteractivityMode: InteractivityMode
		get() = contents.interactivityMode
		set(value) {
			contents.interactivityMode = value
		}

	init {
		styleTags.add(Panel)
		watch(style) {
			contents.style.padding = it.padding
			background?.dispose()
			background = addOptionalChild(0, it.background(this))
		}
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		out.set(contents.sizeConstraints)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		contents.setSize(explicitWidth, explicitHeight)
		out.set(contents.bounds)
		background?.setSize(out.width, out.height)
	}

	open fun close() {
		closing.dispatch(this, cancel.reset())
		if (!cancel.canceled) {
			closed.dispatch(this)
		}
	}

	companion object : StyleTag
}

open class PanelStyle : StyleBase() {

	override val type: StyleType<PanelStyle> = PanelStyle

	var background by prop(noSkinOptional)
	var padding by prop(Pad(5f))

	companion object : StyleType<PanelStyle>
}

fun Owned.panel(
		init: ComponentInit<Panel> = {}): Panel {
	val p = Panel(this)
	p.init()
	return p
}
