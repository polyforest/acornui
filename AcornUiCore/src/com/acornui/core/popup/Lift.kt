/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.core.popup

import com.acornui.component.*
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Matrix4
import com.acornui.math.Vector2
import com.acornui.math.Vector3

/**
 * The Lift component will place its elements as children in the pop up layer, automatically transforming the children
 * to match transformation as if they were part of this component's display hierarchy.
 */
class Lift(owner: Owned) : ElementContainerImpl<UiComponent>(owner), LayoutDataProvider<StackLayoutData> {

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
	var focus = true

	var highlightFocused = false

	private val contents = LiftStack(this)

	val style = contents.style

	init {
		isFocusContainer = true

		contents.invalidated.add {
			child, flagsInvalidated ->
			if (flagsInvalidated and child.layoutInvalidatingFlags > 0) {
				// A child has invalidated a flag marked as layout invalidating.
				if (validation.currentFlag != ValidationFlags.LAYOUT && (child.shouldLayout || flagsInvalidated and ValidationFlags.LAYOUT_ENABLED > 0)) {
					// If we are currently within a layout validation, do not attempt another invalidation.
					// If the child isn't laid out (invisible or includeInLayout is false), don't invalidate the layout
					// unless shouldLayout has just changed.
					invalidate(ValidationFlags.SIZE_CONSTRAINTS)
				}
			}
			if (constrainToStage) invalidate(ValidationFlags.CONCATENATED_TRANSFORM)
		}
	}

	val windowResizedHandler: (Float, Float, Boolean) -> Unit = {
		newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		invalidate(ValidationFlags.CONCATENATED_TRANSFORM)
	}

	override fun onActivated() {
		super.onActivated()
		window.sizeChanged.add(windowResizedHandler)

		addPopUp(PopUpInfo(contents, dispose = false, isModal = isModal, priority = priority, focus = focus, highlightFocused = highlightFocused, onClosed = { onClosed?.invoke() }))
		if (constrainToStage) invalidate(ValidationFlags.CONCATENATED_TRANSFORM)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.sizeChanged.remove(windowResizedHandler)
		removePopUp(contents)
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		contents.setSize(explicitWidth, explicitHeight)
	}

	private val tmpVec = Vector3()
	private val tmpMat = Matrix4()
	private val points = arrayOf(Vector2(0f, 0f), Vector2(1f, 0f), Vector2(1f, 1f), Vector2(0f, 1f))

	override fun updateConcatenatedTransform() {
		super.updateConcatenatedTransform()
		tmpMat.set(concatenatedTransform)
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
		contents.setExternalTransform(tmpMat)
	}

	override fun updateConcatenatedColorTransform() {
		super.updateConcatenatedColorTransform()
		contents.colorTint = concatenatedColorTint
	}
}

fun Owned.lift(init: ComponentInit<Lift>): Lift {
	val l = Lift(this)
	l.init()
	return l
}

private class LiftStack(owner: Owned) : StackLayoutContainer(owner) {

	private val _externalTransform = Matrix4()

	init {
		includeInLayout = false
	}

	fun setExternalTransform(value: Matrix4) {
		_externalTransform.set(value)
		invalidate(ValidationFlags.TRANSFORM)
	}

	override fun updateTransform() {
		_transform.set(_externalTransform)
	}

}