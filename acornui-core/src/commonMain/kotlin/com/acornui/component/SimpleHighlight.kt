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

@file:Suppress("unused", "UNUSED_PARAMETER")

package com.acornui.component

import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.graphic.Color
import com.acornui.math.Bounds
import com.acornui.math.Matrix4
import com.acornui.math.Matrix4Ro
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface HighlightView : UiComponent {
	var highlighted: UiComponentRo?
}

open class SimpleHighlight(
		owner: Context,
		atlasPaths: Map<Float, String>,
		regionName: String
) : ContainerImpl(owner), HighlightView {

	private val highlight = addChild(atlas(atlasPaths, regionName))

	/**
	 * Overridden so that the parent render context doesn't get set to the parent.
	 */
	override var parent: ContainerRo? = null

	override val viewProjectionTransform: Matrix4Ro
		get() = highlighted?.viewProjectionTransform ?: Matrix4.IDENTITY

	override val viewTransform: Matrix4Ro
		get() = highlighted?.viewTransform ?: Matrix4.IDENTITY

	override val projectionTransform: Matrix4Ro
		get() = highlighted?.projectionTransform ?: Matrix4.IDENTITY

	override val transformGlobal: Matrix4Ro
		get() = highlighted?.transformGlobal ?: Matrix4.IDENTITY

	/**
	 * SimpleHighlight overrides [transformGlobal] and therefore must override [useMvpTransforms].
	 */
	override val useMvpTransforms: Boolean = true

	/**
	 * The target being highlighted.
	 */
	override var highlighted: UiComponentRo? = null
		set(value) {
			if (field != value) {
				field?.invalidated?.remove(::highlightedInvalidatedHandler)
				field = value
				field?.invalidated?.add(::highlightedInvalidatedHandler)
				invalidate(ValidationFlags.LAYOUT or ValidationFlags.VIEW_PROJECTION)
			}
		}

	private val delegatedFlags = ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM or ValidationFlags.VIEW_PROJECTION

	private fun highlightedInvalidatedHandler(c: UiComponentRo, flags: Int) {
		invalidate(flags and delegatedFlags)
	}

	init {
		interactivityMode = InteractivityMode.NONE
		includeInLayout = false
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val highlighted = highlighted ?: return
		val w = explicitWidth ?: highlighted.width
		val h = explicitHeight ?: highlighted.height
		val splits = highlight.regionData?.splits
		val dpiScaleX = highlight.dpiScaleX
		val dpiScaleY = highlight.dpiScaleY

		if (splits != null) {
			// left, top, right, bottom
			// If the highlight is a nine patch, offset the highlight by the padding. This allows for the ability to
			// curve around the highlighted target without cutting into it.
			highlight.size(w + (splits[0] + splits[2]) / dpiScaleX, h + (splits[1] + splits[3]) / dpiScaleY)
			highlight.position(-splits[0] / dpiScaleX, -splits[1] / dpiScaleY)
		} else {
			highlight.size(w, h)
			highlight.position(0f, 0f)
		}
		out.set(highlight.bounds)
	}

	override fun dispose() {
		highlighted = null
		super.dispose()
	}
}

inline fun Context.simpleHighlight(atlasPaths: Map<Float, String>, regionName: String, init: ComponentInit<SimpleHighlight> = {}): SimpleHighlight {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return SimpleHighlight(this, atlasPaths, regionName).apply(init)
}

open class HighlightStyle : StyleBase() {

	override val type: StyleType<HighlightStyle> = HighlightStyle

	/**
	 * The factory for the highlight view.
	 */
	var highlight by prop<Context.() -> HighlightView?> { null }

	/**
	 * This will be set as the pop up priority for the highlight.
	 */
	var highlightPriority by prop(99999f)

	var junk by prop(Color.WHITE)

	companion object : StyleType<HighlightStyle>
}