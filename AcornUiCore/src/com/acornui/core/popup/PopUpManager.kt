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

import com.acornui.collection.Clearable
import com.acornui.collection.find2
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.*
import com.acornui.component.layout.LayoutContainerImpl
import com.acornui.component.layout.algorithm.CanvasLayout
import com.acornui.component.layout.algorithm.CanvasLayoutData
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleTag
import com.acornui.core.di.DKey
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.focus.FocusManager
import com.acornui.core.focus.focusFirst
import com.acornui.core.input.Ascii
import com.acornui.core.input.interaction.KeyInteraction
import com.acornui.core.input.interaction.click
import com.acornui.core.input.keyDown
import com.acornui.core.tween.Tween
import com.acornui.core.tween.drive
import com.acornui.core.tween.tweenAlpha
import com.acornui.math.Easing

interface PopUpManager : Clearable {

	val view: UiComponent

	/**
	 * Returns a list of the current pop-ups.
	 */
	val currentPopUps: List<PopUpInfo<*>>

	/**
	 * Adds the pop-up to the pop-up layer.
	 *
	 * Note: The child's layoutData property is set to the layoutData defined in the PopUpInfo object.
	 */
	fun <T : UiComponent> addPopUp(popUpInfo: PopUpInfo<T>)

	/**
	 * Requests that the pop-ups starting from the last modal pop-up to be closed.
	 */
	fun requestModalClose()

	/**
	 * Removes the pop-up with the given component.
	 */
	fun removePopUp(child: UiComponent) {
		val info = currentPopUps.find2 { it.child == child }
		if (info != null)
			removePopUp(info)
	}

	fun <T : UiComponent> removePopUp(popUpInfo: PopUpInfo<T>)

	/**
	 * Clears the active pop-ups.
	 */
	override fun clear()

	companion object : DKey<PopUpManager> {

		val MODAL_STYLE = styleTag()
	}
}

class PopUpInfo<T : UiComponent>(

		/**
		 * The child to add when the pop-up is activated.
		 * If the child has layoutData set, it is expected to be of type [CanvasLayoutData]
		 */
		val child: T,

		/**
		 * If true, a UI blocker will be added a layer below the child.
		 */
		val isModal: Boolean = true,

		/**
		 * The greater the value the higher z-index the pop-up will receive.
		 */
		val priority: Float = 0f,

		/**
		 * If true, the pop-up will be disposed on removal.
		 */
		var dispose: Boolean = false,

		/**
		 * If true, when the pop-up is displayed, the first focusable element will be focused.
		 */
		var focusFirst: Boolean = true,

		/**
		 * If true and [focusFirst] is true, when the first focusable element is focused, it will also be highlighted.
		 */
		var highlightFocused: Boolean = false,

		/**
		 * When a close is requested via clicking on the modal screen, the callback determines if the close succeeds.
		 * (true - remove the pop-up, false - halt removal)
		 */
		val onCloseRequested: (child: T) -> Boolean = { true },

		/**
		 * When this pop-up is removed, this callback will be invoked.
		 * If [dispose] is true, this callback will be called before disposal
		 */
		val onClosed: (child: T) -> Unit = {},

		/**
		 * The layout data used for the pup-up's layout.
		 */
		val layoutData: CanvasLayoutData = CanvasLayoutData().apply {
			center()
		}
)

class PopUpManagerStyle : StyleBase() {

	override val type: StyleType<PopUpManagerStyle> = PopUpManagerStyle

	var modalEaseIn by prop(Easing.pow2In)
	var modalEaseOut by prop(Easing.pow2Out)
	var modalEaseInDuration by prop(0.2f)
	var modalEaseOutDuration by prop(0.2f)

	companion object : StyleType<PopUpManagerStyle>
}

class PopUpManagerImpl(private val root: UiComponent) : LayoutContainerImpl<PopUpManagerStyle, CanvasLayoutData>(root, CanvasLayout(), PopUpManagerStyle()), PopUpManager {

	override val view: UiComponent = this

	private val _currentPopUps = ArrayList<PopUpInfo<*>>()
	override val currentPopUps: List<PopUpInfo<*>>
		get() = _currentPopUps

	private val modalFill = +rect {
		styleTags.add(PopUpManager.MODAL_STYLE)
		visible = false
		click().add { requestModalClose() }
	} layout {
		fill()
	}

	private var showingModal: Boolean = false
	private var tween: Tween? = null

	private fun refresh() {
		val last = _currentPopUps.lastOrNull()
		if (last != null) {
			if (last.focusFirst) {
				last.child.focusFirst()
				if (last.highlightFocused)
					inject(FocusManager).highlightFocused()
			}
		}
		refreshModalBlocker()
	}

	private fun refreshModalBlocker() {
		val lastModalIndex = _currentPopUps.indexOfLast { it.isModal }
		val shouldShowModal = lastModalIndex != -1
		if (shouldShowModal) {
			// Set the modal blocker to be at the correct child index so that it is behind the last modal pop-up.
			val lastModal = _currentPopUps[lastModalIndex]
			var childIndex = elements.indexOf(lastModal.child)
			val modalIndex = elements.indexOf(modalFill)
			if (modalIndex != childIndex - 1) {
				if (modalIndex < childIndex) childIndex--
				removeElement(modalFill)
				addElement(childIndex, modalFill)
			}
		}
		val s = style
		if (shouldShowModal != showingModal) {
			showingModal = shouldShowModal
			if (shouldShowModal) {
				tween?.complete()
				modalFill.visible = true
				modalFill.alpha = 0f
				tween = modalFill.tweenAlpha(s.modalEaseInDuration, s.modalEaseIn, 1f).drive(timeDriver)
				tween!!.completed.addOnce {
					tween = null
				}
			} else {
				tween?.complete()
				tween = modalFill.tweenAlpha(s.modalEaseOutDuration, s.modalEaseOut, 0f).drive(timeDriver)
				tween!!.completed.addOnce {
					modalFill.visible = false
					tween = null
				}
			}
		}
	}

	private val childClosedHandler: (Closeable) -> Unit = {
		removePopUp(it as UiComponent)
	}

	override fun clear() {
		while (_currentPopUps.isNotEmpty()) {
			removePopUp(_currentPopUps.last())
		}
	}

	private val rootKeyDownHandler = {
		event: KeyInteraction ->
		if (_currentPopUps.isNotEmpty()) {
			if (!event.handled && event.keyCode == Ascii.ESCAPE) {
				event.handled = true
				requestModalClose()
			}
		}
	}

	init {
		root.keyDown().add(rootKeyDownHandler)

		// The pop up container should not intercept user interaction, unless it is modal.
		interactivityMode = InteractivityMode.CHILDREN
	}

	override fun requestModalClose() {
		val lastModalIndex = _currentPopUps.indexOfLast { it.isModal }
		if (lastModalIndex == -1) return // no modals
		var i = _currentPopUps.lastIndex
		while (i >= 0 && i >= lastModalIndex) {
			@Suppress("UNCHECKED_CAST")
			val p = _currentPopUps[i--] as PopUpInfo<UiComponent>
			if (p.onCloseRequested(p.child))
				removePopUp(p)
			else
				break
		}
	}

	override fun <T : UiComponent> addPopUp(popUpInfo: PopUpInfo<T>) {
		val child = popUpInfo.child
		if (child.parent != null) throw Exception("The pop-up must be removed before adding.")
		if (child is Closeable) {
			child.closed.add(childClosedHandler)
		}
		val index = _currentPopUps.sortedInsertionIndex(popUpInfo, { a, b -> a.priority.compareTo(b.priority) })
		_currentPopUps.add(index, popUpInfo)
		if (index == _currentPopUps.lastIndex) {
			addElement(child)
		} else {
			addElementBefore(child, _currentPopUps[index + 1].child)
		}
		refresh()
		child.layoutData = popUpInfo.layoutData
	}

	override fun <T : UiComponent> removePopUp(popUpInfo: PopUpInfo<T>) {
		val child = popUpInfo.child
		_currentPopUps.remove(popUpInfo)
		removeElement(child)
		if (child is Closeable) {
			child.closed.remove(childClosedHandler)
		}
		popUpInfo.onClosed(child)
		if (popUpInfo.dispose && !child.isDisposed)
			child.dispose()
		refresh()
	}

	override fun dispose() {
		super.dispose()
		root.keyDown().remove(rootKeyDownHandler)
	}
}

fun <T : UiComponent> Owned.addPopUp(popUpInfo: PopUpInfo<T>): T {
	inject(PopUpManager).addPopUp(popUpInfo)
	return popUpInfo.child
}

fun Owned.removePopUp(popUpInfo: PopUpInfo<*>) {
	inject(PopUpManager).removePopUp(popUpInfo)
}

fun Owned.removePopUp(popUpInfoChild: UiComponent) {
	inject(PopUpManager).removePopUp(popUpInfoChild)
}