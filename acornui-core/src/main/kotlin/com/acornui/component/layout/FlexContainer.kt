/*
 * Copyright 2020 Poly Forest, LLC
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

@file:Suppress("CssUnresolvedCustomProperty")

package com.acornui.component.layout

import com.acornui.component.ComponentInit
import com.acornui.component.Div
import com.acornui.component.WithNode
import com.acornui.component.div
import com.acornui.component.layout.LayoutStyles.grid
import com.acornui.component.layout.LayoutStyles.hFlowGroup
import com.acornui.component.layout.LayoutStyles.hGroup
import com.acornui.component.layout.LayoutStyles.vFlowGroup
import com.acornui.component.layout.LayoutStyles.vGroup
import com.acornui.component.style.cssClass
import com.acornui.component.style.cssVar
import com.acornui.di.Context
import com.acornui.dom.addStyleToHead
import com.acornui.skins.CssProps
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/*
 * Flexbox amazingly enough does not have a gap property, only grid does.
 *
 * In order to simulate a gap property we need to set the margins on the child elements.
 * We use css variables for this: `hGap and vGap`
 */

/**
 * CSS styles for common layouts.
 */
object LayoutStyles {

	val flexContainer by cssClass()
	val contents by cssClass()

	val hGroup by cssClass()
	val vGroup by cssClass()
	val hFlowGroup by cssClass()
	val vFlowGroup by cssClass()
	val grid by cssClass()

	/**
	 * The default vertical and horizontal gap in a layout.
	 * Defaults to [CssProps.gap]
	 */
	val gap by cssVar()

	/**
	 * The horizontal gap in a flow, grid, or horizontal layout.
	 * Defaults to [gap]
	 */
	val hGap by cssVar()

	/**
	 * The vertical gap in a flow, grid, or vertical layout.
	 * Defaults to [gap]
	 */
	val vGap by cssVar()

	private val parentHGap by cssVar()
	private val parentVGap by cssVar()

	init {
		@Suppress("CssInvalidPropertyValue")
		addStyleToHead(
			"""
				
$flexContainer {
	$gap: ${CssProps.gap.v};
	$hGap: ${gap.v};
	$vGap: ${gap.v};
	display: block;
}				

$grid {
	column-gap: ${hGap.v};
  	row-gap: ${vGap.v};
}

$grid > $contents {
	display: grid;
}

$contents {
	display: flex;
	flex-wrap: inherit;
	width: inherit;
	height: inherit;
	justify-content: inherit;
	justify-items: inherit;
	align-content: inherit;
	align-items: inherit;
	flex-direction: inherit;
	column-gap: inherit;
	row-gap: inherit;
	$parentHGap: ${hGap.v};
	$parentVGap: ${vGap.v};
}

$hFlowGroup > $contents, $vFlowGroup > $contents {
	margin: 0 calc(-1 * ${vGap.v}) calc(-1 * ${hGap.v}) 0;
}

$hFlowGroup > $contents > *, $vFlowGroup $contents > * {
	margin: 0 ${parentVGap.v} ${parentHGap.v} 0;
}

$hGroup > $contents, $hFlowGroup > $contents {
	height: 100%;
}

$hGroup > $contents > *:not(:last-child) {
	margin-right: ${parentHGap.v};
}

$vGroup > $contents, $vFlowGroup > $contents {
	width: 100%;
}

$vGroup > $contents > *:not(:last-child) {
	margin-bottom: ${parentVGap.v};
}

$vGroup {
	flex-direction: column;
	align-items: self-start;
}

$hGroup {
	flex-direction: row;
	align-items: baseline;
}

$hFlowGroup {
	align-items: baseline;
	flex-direction: row;
	flex-wrap: wrap;
}

$vFlowGroup {
	align-items: self-start;
	flex-direction: column;
	flex-wrap: wrap;
}
		"""
		)
	}
}

/**
 * A div wrapper to avoid the lack of gap support.
 */
open class FlexContainer(owner: Context) : Div(owner) {

	private val contents = addChild(div {
		addClass(LayoutStyles.contents)
	})

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		contents.removeElement(index)
	}

	init {
		addClass(LayoutStyles.flexContainer)
	}

}

open class VGroup(owner: Context) : FlexContainer(owner) {
	init {
		addClass(vGroup)
	}
}

inline fun Context.vGroup(init: ComponentInit<VGroup> = {}): VGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return VGroup(this).apply(init)
}

open class HGroup(owner: Context) : FlexContainer(owner) {
	init {
		addClass(hGroup)
	}
}

inline fun Context.hGroup(init: ComponentInit<HGroup> = {}): HGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return HGroup(this).apply(init)
}

open class HFlowGroup(owner: Context) : FlexContainer(owner) {
	init {
		addClass(hFlowGroup)
	}
}

inline fun Context.hFlowGroup(init: ComponentInit<HFlowGroup> = {}): HFlowGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return HFlowGroup(this).apply(init)
}

open class VFlowGroup(owner: Context) : FlexContainer(owner) {
	init {
		addClass(vFlowGroup)
	}
}

inline fun Context.vFlowGroup(init: ComponentInit<VFlowGroup> = {}): VFlowGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return VFlowGroup(this).apply(init)
}

open class Grid(owner: Context) : FlexContainer(owner) {
	init {
		addClass(grid)
	}
}

inline fun Context.grid(init: ComponentInit<Grid> = {}): Grid {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Grid(this).apply(init)
}