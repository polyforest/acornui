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

package com.acornui.popup

import com.acornui.component.*
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.di.Context
import com.acornui.focus.focus
import com.acornui.function.as2
import com.acornui.math.Bounds
import com.acornui.math.Matrix4
import com.acornui.math.vec2
import com.acornui.math.vec3
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * The Lift component will place its elements as children in the pop up layer, automatically transforming the children
 * to match transformation as if they were part of this component's display hierarchy.
 */
class Lift(owner: Context) : ElementContainerImpl<UiComponent>(owner), LayoutDataProvider<StackLayoutData> {

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	/**
	 * If true, the contents position will be constrained to not extend beyond the stage.
	 */
	var constrainToStage: Boolean = true

	/**
	 * When the pop-up is closed, this will be invoked.
	 */
	var onClosed: (() -> Unit)? = null

	var isModal = false

	/**
	 * The pop-up manager will place higher priority pop-ups in front of lower priority pop-ups.
	 */
	var priority: Float = 0f

	/**
	 * If true, the contents will be focused when added to the stage.
	 */
	var focus = false

	/**
	 * If true and [focus] is true, when the first focusable element is focused, it will also be highlighted.
	 */
	var highlightFocused = false

	private val contents = stack {
		includeInLayout = false // So the pop up manager doesn't attempt to lay out the contents.
	}

	val style = contents.style

	init {
		isFocusContainer = true
		interactivityMode = InteractivityMode.NONE // The elements are interactive but this Lift component is virtual. 

		validation.addNode(CONTENTS_TRANSFORM, ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM or ValidationFlags.VIEW_PROJECTION, ::updateContentsTransform)

		contents.invalidated.add { child, flagsInvalidated ->
			if (flagsInvalidated and child.layoutInvalidatingFlags > 0) {
				// A child has invalidated a flag marked as layout invalidating.
				if (validation.currentFlag != ValidationFlags.LAYOUT && (child.shouldLayout || flagsInvalidated and ValidationFlags.LAYOUT_ENABLED > 0)) {
					// If we are currently within a layout validation, do not attempt another invalidation.
					// If the child isn't laid out (invisible or includeInLayout is false), don't invalidate the layout
					// unless shouldLayout has just changed.
					invalidate(ValidationFlags.LAYOUT)
				}
			}
		}
	}

	override fun onActivated() {
		super.onActivated()
		window.sizeChanged.add(::windowResizedHandler.as2)

		addPopUp(PopUpInfo(contents, dispose = false, isModal = isModal, priority = priority, focus = focus, highlightFocused = highlightFocused, onClosed = { onClosed?.invoke() }))
		invalidate(CONTENTS_TRANSFORM)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.sizeChanged.remove(::windowResizedHandler.as2)
		removePopUp(contents)
	}

	private fun windowResizedHandler() {
		if (constrainToStage)
			invalidate(CONTENTS_TRANSFORM)
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		contents.size(explicitWidth, explicitHeight)
	}

	private val tmpVec = vec3()
	private val tmpMat = Matrix4()
	private val points = arrayOf(vec2(0f, 0f), vec2(1f, 0f), vec2(1f, 1f), vec2(0f, 1f))

	private fun updateContentsTransform() {
		tmpMat.set(contents.viewProjectionTransformInv).mul(viewProjectionTransform).mul(transformGlobal)
		if (constrainToStage) {
			val w = window.width
			val h = window.height
			val contentsW = contents.width
			val contentsH = contents.height
			for (point in points) {
				tmpVec.set(contentsW * point.x, contentsH * point.y, 0f)
				tmpMat.prj(tmpVec)
				if (tmpVec.x > w) {
					tmpMat.trn(w - tmpVec.x, 0f, 0f)
				}
				if (tmpVec.x < 0f) {
					tmpMat.trn(-tmpVec.x, 0f, 0f)
				}
				if (tmpVec.y > h) {
					tmpMat.trn(0f, h - tmpVec.y, 0f)
				}
				if (tmpVec.y < 0f) {
					tmpMat.trn(0f, -tmpVec.y, 0f)
				}
			}
		}
		contents.transformGlobalOverride = tmpMat
	}

	override fun updateColorTint() {
		super.updateColorTint()
		contents.colorTintGlobalOverride = colorTintGlobal
	}

	companion object {
		private const val CONTENTS_TRANSFORM = 1 shl 16
	}
}

inline fun Context.lift(init: ComponentInit<Lift> = {}): Lift {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val l = Lift(this)
	l.init()
	return l
}