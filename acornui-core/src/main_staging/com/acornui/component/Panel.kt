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
import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.noSkinOptional
import com.acornui.di.Context

import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.signal.Cancel
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class Panel(
		owner: Context
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
		addClass(Panel)
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

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		contents.size(explicitWidth, explicitHeight)
		out.set(contents.bounds)
		background?.size(out.width, out.height)
	}

	open fun close() {
		closing.dispatch(this, cancel.reset())
		if (!cancel.isCancelled) {
			closed.dispatch(this)
		}
	}

	companion object : StyleTag
}

open class PanelStyle : ObservableBase() {

	override val type: StyleType<PanelStyle> = PanelStyle

	var background by prop(noSkinOptional)
	var padding by prop(Pad(5.0))

	companion object : StyleType<PanelStyle>
}

inline fun panelStyle(init: ComponentInit<PanelStyle> = {}): PanelStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return PanelStyle().apply(init)
}

inline fun Context.panel(init: ComponentInit<Panel> = {}): Panel {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val p = Panel(this)
	p.init()
	return p
}
