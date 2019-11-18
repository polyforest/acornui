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

package com.acornui.component.layout.algorithm

import com.acornui.component.ComponentInit
import com.acornui.component.UiComponent
import com.acornui.component.layout.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Owned
import com.acornui.math.Bounds
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

class CanvasLayout : LayoutAlgorithm<CanvasLayoutStyle, CanvasLayoutData> {

	override val style = CanvasLayoutStyle()

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		if (elements.isEmpty()) return

		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast
			element.setSize(layoutData?.getPreferredWidth(explicitWidth), layoutData?.getPreferredHeight(explicitHeight))
		}

		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast

			if (layoutData == null) {
				element.moveTo(0f, 0f)
				out.ext(element.width, element.height)
			} else {
				val left = layoutData.left
				val right = layoutData.right
				val horizontalCenter = layoutData.horizontalCenter
				val explicitHalfW = explicitWidth * 0.5f
				val leftAnchor = left ?: explicitHalfW + horizontalCenter
				val rightAnchor = explicitWidth - right ?: explicitHalfW + horizontalCenter
				
				val defaultHAlign: HAlign = if (leftAnchor == null && rightAnchor == null) {
					HAlign.LEFT // No anchors set
				} else if (leftAnchor != null && rightAnchor != null) {
					HAlign.CENTER // Two anchors set
				} else {
					if (leftAnchor != null) HAlign.LEFT else HAlign.RIGHT // One anchor set
				}
				val x: Float = when (layoutData.horizontalAlign ?: style.horizontalAlign ?: defaultHAlign) {
					HAlign.LEFT -> leftAnchor
					HAlign.CENTER -> {
						val midX = (leftAnchor + rightAnchor) * 0.5f ?: leftAnchor ?: rightAnchor ?: explicitHalfW
						midX - element.width * 0.5f
					}
					HAlign.RIGHT -> rightAnchor - element.width
				} ?: 0f
				
				val top = layoutData.top
				val bottom = layoutData.bottom
				val verticalCenter = layoutData.verticalCenter
				val explicitHalfH = explicitHeight * 0.5f
				val topAnchor = top ?: explicitHalfH + verticalCenter
				val bottomAnchor = explicitHeight - bottom ?: explicitHalfH + verticalCenter
				
				val defaultVAlign: VAlign = if (topAnchor == null && bottomAnchor == null) {
					VAlign.TOP // No anchors set
				} else if (topAnchor != null && bottomAnchor != null) {
					VAlign.MIDDLE // Two anchors set
				} else {
					if (topAnchor != null) VAlign.TOP else VAlign.BOTTOM // One anchor set
				}
				val y: Float = when (layoutData.verticalAlign ?: style.verticalAlign ?: defaultVAlign) {
					VAlign.TOP -> topAnchor
					VAlign.MIDDLE -> {
						val midY = (topAnchor + bottomAnchor) * 0.5f ?: topAnchor ?: bottomAnchor ?: explicitHalfH
						midY - element.height * 0.5f
					}
					VAlign.BOTTOM -> bottomAnchor - element.height
					VAlign.BASELINE -> bottomAnchor - element.baseline
				} ?: 0f

				element.moveTo(x, y)
				out.ext(element.right + (layoutData.right ?: 0f), element.bottom + (layoutData.bottom ?: 0f))
			}
		}
	}

	override fun createLayoutData() = CanvasLayoutData()
}

class CanvasLayoutStyle : StyleBase() {

	override val type: StyleType<CanvasLayoutStyle> = Companion

	/**
	 * The default alignment between two anchor points.
	 * This can be overridden on the individual element with [CanvasLayoutData.horizontalAlign]
	 * Possible horizontal anchor points are [CanvasLayoutData.left], [CanvasLayoutData.horizontalCenter], and
	 * [CanvasLayoutData.right]
	 * If no vertical alignment is set, then the default alignment will be as follows:
	 * No anchors - left alignment
	 * Left anchor only - left alignment
	 * Right anchor only - right alignment
	 * Else - center alignment.
	 */
	var horizontalAlign: HAlign? by prop(null)

	/**
	 * The default alignment between two anchor points.
	 * Possible vertical anchor points are [CanvasLayoutData.top], [CanvasLayoutData.verticalCenter], and
	 * [CanvasLayoutData.bottom]
	 * This can be overridden on the individual element with [CanvasLayoutData.horizontalAlign]
	 * If no horizontal alignment is set, then the default alignment will be as follows:
	 * No anchors - Top alignment
	 * Top anchor only - top alignment
	 * Bottom anchor only - bottom alignment
	 * Else - middle alignment.
	 */
	var verticalAlign: VAlign? by prop(null)

	companion object : StyleType<CanvasLayoutStyle>

}

open class CanvasLayoutContainer<E : UiComponent>(owner: Owned) : ElementLayoutContainer<CanvasLayoutStyle, CanvasLayoutData, E>(owner, CanvasLayout())

open class CanvasLayoutData : BasicLayoutData() {

	/**
	 * If set, the horizontal alignment for this item overrides the canvas layout's horizontalAlign.
	 * @see CanvasLayoutStyle.horizontalAlign
	 */
	var horizontalAlign: HAlign? by bindable(null)

	/**
	 * If set, the vertical alignment for this item overrides the canvas layout's verticalAlign.
	 * @see CanvasLayoutStyle.verticalAlign
	 */
	var verticalAlign: VAlign? by bindable(null)

	/**
	 * The top anchor point.
	 */
	var top: Float? by bindable(null)
	var right: Float? by bindable(null)
	var bottom: Float? by bindable(null)
	var left: Float? by bindable(null)
	var horizontalCenter: Float? by bindable(null)
	var verticalCenter: Float? by bindable(null)

	/**
	 * Sets the horizontal and vertical center to 0f
	 */
	fun center() {
		horizontalAlign = HAlign.CENTER
		verticalAlign = VAlign.MIDDLE
	}

	override fun getPreferredWidth(availableWidth: Float?): Float? {
		if (availableWidth == null || width != null) return width
		val left = left
		val right = right
		val horizontalCenter = horizontalCenter
		val p = widthPercent ?: 1f
		return p * if (left != null && right != null) availableWidth - right - left
		else if (left != null && horizontalCenter != null) 0.5f * availableWidth - left + horizontalCenter
		else if (right != null && horizontalCenter != null) 0.5f * availableWidth - right - horizontalCenter
		else if (widthPercent != null) availableWidth
		else null
	}

	override fun getPreferredHeight(availableHeight: Float?): Float? {
		if (availableHeight == null || height != null) return height
		val top = top
		val bottom = bottom
		val verticalCenter = verticalCenter
		val p = heightPercent ?: 1f
		return p * if (top != null && bottom != null) availableHeight - bottom - top
		else if (top != null && verticalCenter != null) 0.5f * availableHeight - top + verticalCenter
		else if (bottom != null && verticalCenter != null) 0.5f * availableHeight - bottom - verticalCenter
		else if (heightPercent != null) availableHeight
		else null
	}
}

fun canvasLayoutData(init: CanvasLayoutData.() -> Unit = {}): CanvasLayoutData {
	val c = CanvasLayoutData()
	c.init()
	return c
}

@JvmName("canvasT")
inline fun <E : UiComponent> Owned.canvas(init: ComponentInit<CanvasLayoutContainer<E>> = {}): CanvasLayoutContainer<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return CanvasLayoutContainer<E>(this).apply(init)
}

inline fun Owned.canvas(init: ComponentInit<CanvasLayoutContainer<UiComponent>> = {}): CanvasLayoutContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return canvas<UiComponent>(init)
}

private operator fun Float?.times(x: Float?): Float? {
	if (this == null || x == null) return null
	return this * x
}

private operator fun Float?.plus(x: Float?): Float? {
	if (this == null || x == null) return null
	return this + x
}

private operator fun Float?.minus(x: Float?): Float? {
	if (this == null || x == null) return null
	return this - x
}