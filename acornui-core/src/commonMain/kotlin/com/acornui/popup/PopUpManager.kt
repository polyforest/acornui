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

import com.acornui.Disposable
import com.acornui.collection.addBefore
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.*
import com.acornui.component.layout.ElementLayoutContainer
import com.acornui.component.layout.algorithm.CanvasLayout
import com.acornui.component.layout.algorithm.CanvasLayoutData
import com.acornui.component.layout.algorithm.CanvasLayoutStyle
import com.acornui.component.layout.size
import com.acornui.component.style.OptionalSkinPart
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.di.own
import com.acornui.focus.*
import com.acornui.graphic.Color
import com.acornui.input.Ascii
import com.acornui.input.interaction.KeyInteractionRo
import com.acornui.input.interaction.click
import com.acornui.input.interaction.clickHandledForAFrame
import com.acornui.input.keyDown
import com.acornui.math.Bounds
import com.acornui.math.Easing
import com.acornui.properties.afterChange
import com.acornui.recycle.Clearable
import com.acornui.signal.Signal0
import com.acornui.signal.addOnce
import com.acornui.start
import com.acornui.time.callLater
import com.acornui.tween.Tween
import com.acornui.tween.tweenAlpha

interface PopUpManager : Clearable {

	/**
	 * Initializes the pop-up manager, returning the view.
	 */
	fun init(owner: Context): UiComponent

	/**
	 * Returns a list of the current pop-ups.
	 */
	val currentPopUps: List<PopUpInfo<UiComponent>>

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
	fun removePopUp(child: UiComponent)

	fun <T : UiComponent> removePopUp(popUpInfo: PopUpInfo<T>)

	/**
	 * Clears the active pop-ups.
	 */
	override fun clear()

	companion object : Context.Key<PopUpManager>, StyleTag {

		override val factory = dependencyFactory {
			PopUpManagerImpl(it)
		}
	}
}

class PopUpInfo<T : UiComponent>(

		/**
		 * The child to add when the pop-up is activated.
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
		 * If true, the pop-up [child] will be disposed on removal.
		 */
		val dispose: Boolean = false,

		/**
		 * If true, when the pop-up is displayed, the first focusable element will be focused.
		 */
		val focus: Boolean = true,

		/**
		 * If true and [focus] is true, when the first focusable element is focused, it will also be highlighted.
		 */
		val highlightFocused: Boolean = false,

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
) : Comparable<PopUpInfo<*>> {

	override fun compareTo(other: PopUpInfo<*>): Int {
		return priority.compareTo(other.priority)
	}

}

class PopUpManagerStyle : StyleBase() {

	override val type: StyleType<PopUpManagerStyle> = PopUpManagerStyle

	var modalFill by prop<OptionalSkinPart> {
		rect {
			style.backgroundColor = Color(0f, 0f, 0f, 0.7f)
		}
	}

	var modalEaseIn by prop(Easing.pow2In)
	var modalEaseOut by prop(Easing.pow2Out)
	var modalEaseInDuration by prop(0.2f)
	var modalEaseOutDuration by prop(0.2f)

	companion object : StyleType<PopUpManagerStyle>
}

class PopUpManagerImpl(owner: Context) : ContextImpl(owner), PopUpManager, Disposable {

	private lateinit var view: PopUpManagerView

	override fun init(owner: Context): UiComponent {
		view = PopUpManagerView(owner)
		view.modalCloseRequested.add(::requestModalClose)
		return view
	}

	private val lastModalIndex: Int
		get() = currentPopUps.indexOfLast { it.isModal }

	private fun refreshModalBlocker() {
		if (!view.isDisposed)
			view.modalIndex = lastModalIndex
	}

	private var _currentPopUps = mutableListOf<PopUpInfo<UiComponent>>()
	override var currentPopUps: List<PopUpInfo<UiComponent>> = _currentPopUps

	override fun clear() {
		while (_currentPopUps.isNotEmpty()) {
			removePopUp(_currentPopUps.last())
		}
	}

	override fun requestModalClose() {
		val lastModalIndex = lastModalIndex
		if (lastModalIndex == -1) return // no modals
		val currentPopUps = currentPopUps
		var i = currentPopUps.lastIndex
		while (i >= 0 && i >= lastModalIndex) {
			@Suppress("UNCHECKED_CAST")
			val p = currentPopUps[i--]
			if (p.onCloseRequested(p.child))
				removePopUp(p)
			else
				break
		}
	}

	override fun <T : UiComponent> addPopUp(popUpInfo: PopUpInfo<T>) {
		removePopUp(popUpInfo.child)
		val child = popUpInfo.child
		child.disposed.add(::popUpChildDisposedHandler)
		if (child is Closeable)
			child.closed.add(::childClosedHandler)

		val index = currentPopUps.sortedInsertionIndex(popUpInfo)
		@Suppress("UNCHECKED_CAST")
		_currentPopUps.add(index, popUpInfo as PopUpInfo<UiComponent>)

		callLater {
			updateView(popUpInfo)
		}
	}

	override fun <T : UiComponent> removePopUp(popUpInfo: PopUpInfo<T>) {
		val child = popUpInfo.child
		child.disposed.remove(::popUpChildDisposedHandler)
		if (child is Closeable)
			child.closed.remove(::childClosedHandler)

		@Suppress("UNCHECKED_CAST")
		_currentPopUps.remove(popUpInfo as PopUpInfo<UiComponent>)

		callLater {
			updateView(popUpInfo)
		}

	}

	private fun updateView(popUpInfo: PopUpInfo<UiComponent>) {
		val currentPopUps = currentPopUps
		val index = currentPopUps.indexOf(popUpInfo)
		val child = popUpInfo.child

		if (index == -1) {
			if (!currentPopUps.any { it.child == child }) {
				// Child was not re-added in a different pop-up.
				val wasFocused = child.isFocused
				view.removeElement(child)
				child.layoutData = null
				if (popUpInfo.dispose && !child.isDisposed)
					child.dispose()
				if (wasFocused) {
					val currentPopUp = currentPopUps.lastOrNull()
					if (currentPopUp?.focus == true) {
						currentPopUp.child.focus(FocusOptions(highlight = currentPopUp.highlightFocused))
					} else {
						view.focusModalFill()
					}
				}
				refreshModalBlocker()
			}
		} else {
			child.layoutData = popUpInfo.layoutData
			view.addElement(index, child)
			if (popUpInfo.focus)
				child.focus(FocusOptions(highlight = popUpInfo.highlightFocused))
			refreshModalBlocker()
		}
	}

	override fun removePopUp(child: UiComponent) {
		val found = currentPopUps.find { it.child == child } ?: return
		removePopUp(found)
	}

	private fun childClosedHandler(child: Closeable) {
		removePopUp(child as UiComponent)
	}

	private fun popUpChildDisposedHandler(child: UiComponent) {
		removePopUp(child)
	}

	override fun dispose() {
		super.dispose()
		clear()
	}
}

private class PopUpManagerView(owner: Context) : ElementLayoutContainer<CanvasLayoutStyle, CanvasLayoutData, UiComponent>(owner, CanvasLayout()) {

	private val _modalCloseRequested = own(Signal0())
	val modalCloseRequested = _modalCloseRequested.asRo()

	private val popUpManagerStyle = bind(PopUpManagerStyle())

	val modalFillContainer = addChild(stack {
		focusEnabled = true
		visible = false
		click().add {
			if (!it.handled)
				_modalCloseRequested.dispatch()
		}
	})

	var modalIndex: Int = -1
		set(value) {
			require(value < elements.size) { "modalIndex must be less than elements.size" }
			field = value
			// Reorder the modal fill container and set its visibility.
			if (value == -1) {
				showModal = false
			} else {
				showModal = true
				_children.addBefore(modalFillContainer, elements[value])
			}
		}

	private var tween: Tween? = null
	private var showModal: Boolean by afterChange(false) { value ->
		val s = popUpManagerStyle
		if (value) {
			tween?.complete()
			// Often pop-ups are added via mouse down. Do not allow a click() on the modal blocker for one frame.
			modalFillContainer.clickHandledForAFrame()
			modalFillContainer.visible = true
			modalFillContainer.alpha = 0f
			tween = modalFillContainer.tweenAlpha(s.modalEaseInDuration, s.modalEaseIn, 1f).start()
			tween!!.completed.addOnce {
				tween = null
			}
		} else {
			tween?.complete()
			tween = modalFillContainer.tweenAlpha(s.modalEaseOutDuration, s.modalEaseOut, 0f).start()
			tween!!.completed.addOnce {
				modalFillContainer.visible = false
				tween = null
			}
		}
	}

	init {
		styleTags.add(PopUpManager)
		isFocusContainer = true
		interactivityMode = InteractivityMode.CHILDREN

		watch(popUpManagerStyle) {
			val modalFill = it.modalFill(this)
			modalFillContainer.clearElements(true)
			modalFillContainer.apply {
				addOptionalElement(modalFill)?.layout { fill() }
			}
			Unit
		}
	}

	override fun onActivated() {
		super.onActivated()
		stage.keyDown().add(::rootKeyDownHandler)
		stage.focusedEvent(true).add(::focusHandler)
	}

	override fun onDeactivated() {
		// Must be before super.onDeactivated or the focus change prevention will get stuck.
		stage.keyDown().remove(::rootKeyDownHandler)
		stage.focusedEvent(true).remove(::focusHandler)
		super.onDeactivated()
	}

	private fun rootKeyDownHandler(event: KeyInteractionRo) {
		if (!event.handled && event.keyCode == Ascii.ESCAPE) {
			event.handled = true
			_modalCloseRequested.dispatch()
		}
	}

	fun focusModalFill() {
		modalFillContainer.focusSelf()
	}

	private fun focusHandler(event: FocusEventRo) {
		val old = event.relatedTarget
		val new = event.target
		println("pop up focus handler $new")
		val modalFillContainer = modalFillContainer
		if (!isBeneathModal(new)) return
		if (old === modalFillContainer && new === stage) {
			event.preventDefault()
			return
		}
		val lastModalIndex = modalIndex
		val focusables = focusManager.focusables
		val focusIndex = focusables.indexOf(new)
		val isForwards = focusIndex >= focusables.indexOf(old)

		val toFocus: UiComponentRo = if (isForwards) {
			if (old == modalFillContainer) {
				var firstFocusable: UiComponentRo = modalFillContainer
				for (i in lastModalIndex..elements.lastIndex) {
					firstFocusable = elements[i].firstFocusable ?: continue
					break
				}
				firstFocusable
			} else {
				modalFillContainer
			}
		} else {
			if (old == modalFillContainer) {
				var lastFocusable: UiComponentRo = modalFillContainer
				for (i in elements.lastIndex downTo lastModalIndex) {
					lastFocusable = elements[i].lastFocusable ?: continue
					break
				}
				lastFocusable
			} else {
				modalFillContainer
			}
		}
		toFocus.focusSelf(event.options)
		event.preventDefault()
	}

	/**
	 * Returns true if the target element is underneath the modal fill container.
	 */
	private fun isBeneathModal(target: UiComponentRo?): Boolean {
		val lastModalIndex = modalIndex
		if (lastModalIndex == -1 || target == null || target === modalFillContainer || elements.isEmpty()) return false
		for (i in elements.lastIndex downTo lastModalIndex) {
			if (elements[i].isAncestorOf(target)) {
				return false
			}
		}
		return true
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		modalFillContainer.size(out)
	}
}

fun <T : UiComponent> Context.addPopUp(popUpInfo: PopUpInfo<T>): T {
	inject(PopUpManager).addPopUp(popUpInfo)
	return popUpInfo.child
}

fun Context.removePopUp(popUpInfo: PopUpInfo<*>) {
	inject(PopUpManager).removePopUp(popUpInfo)
}

fun Context.removePopUp(popUpInfoChild: UiComponent) {
	inject(PopUpManager).removePopUp(popUpInfoChild)
}
