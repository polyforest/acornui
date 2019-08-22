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

import com.acornui.collection.firstOrNull2
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.*
import com.acornui.component.layout.ElementLayoutContainerImpl
import com.acornui.component.layout.algorithm.CanvasLayout
import com.acornui.component.layout.algorithm.CanvasLayoutData
import com.acornui.component.style.*
import com.acornui.di.*
import com.acornui.focus.*
import com.acornui.graphic.Color
import com.acornui.input.Ascii
import com.acornui.input.interaction.KeyInteractionRo
import com.acornui.input.interaction.click
import com.acornui.input.interaction.clickHandledForAFrame
import com.acornui.input.keyDown
import com.acornui.isAncestorOf
import com.acornui.math.Easing
import com.acornui.recycle.Clearable
import com.acornui.signal.Cancel
import com.acornui.signal.addOnce
import com.acornui.time.start
import com.acornui.tween.Tween
import com.acornui.tween.tweenAlpha

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
		val info = currentPopUps.firstOrNull2 { it.child == child }
		if (info != null)
			removePopUp(info)
	}

	fun <T : UiComponent> removePopUp(popUpInfo: PopUpInfo<T>)

	/**
	 * Clears the active pop-ups.
	 */
	override fun clear()

	companion object : DKey<PopUpManager>, StyleTag {
		override fun factory(injector: Injector): PopUpManager? {
			return PopUpManagerImpl(injector)
		}
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

class PopUpManagerImpl(injector: Injector) : ElementLayoutContainerImpl<NoopStyle, CanvasLayoutData>(OwnedImpl(injector), CanvasLayout()), PopUpManager {

	private val popUpManagerStyle = bind(PopUpManagerStyle())

	override val view: UiComponent = this

	private val _currentPopUps = ArrayList<PopUpInfo<*>>()
	override val currentPopUps: List<PopUpInfo<*>>
		get() = _currentPopUps

	private val modalFillContainer = +stack {
		focusEnabled = true
		visible = false
		click().add {
			if (!it.handled)
				requestModalClose()
		}
	} layout {
		fill()
	}

	private var showingModal: Boolean = false
	private var tween: Tween? = null

	private fun refresh() {
		refreshModalBlocker()
	}

	private fun refreshModalBlocker() {
		val lastModalIndex = _currentPopUps.indexOfLast { it.isModal }
		val shouldShowModal = lastModalIndex != -1
		if (shouldShowModal) {
			// Set the modal blocker to be at the correct child index so that it is behind the last modal pop-up.
			val lastModal = _currentPopUps[lastModalIndex]
			val childIndex = elements.indexOf(lastModal.child)
			val modalIndex = elements.indexOf(modalFillContainer)
			if (modalIndex != childIndex - 1)
				addElement(childIndex, modalFillContainer)
		}
		val s = popUpManagerStyle
		if (shouldShowModal != showingModal) {
			showingModal = shouldShowModal
			if (shouldShowModal) {
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
	}

	override fun clear() {
		while (_currentPopUps.isNotEmpty()) {
			removePopUp(_currentPopUps.last())
		}
	}

	init {
		// The pop up manager is automatically added to the pending disposables registry and should not be
		// disposed when the stage is.
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
		focusManager.focusedChanging.add(::focusChangingHandler)
		stage.keyDown().add(::rootKeyDownHandler)
	}

	override fun onDeactivated() {
		// Must be before super.onDeactivated or the focus change prevention will get stuck.
		focusManager.focusedChanging.remove(::focusChangingHandler)
		stage.keyDown().remove(::rootKeyDownHandler)
		super.onDeactivated()
	}

	private fun rootKeyDownHandler(event: KeyInteractionRo) {
		if (_currentPopUps.isNotEmpty()) {
			if (!event.handled && event.keyCode == Ascii.ESCAPE) {
				event.handled = true
				requestModalClose()
			}
		}
	}

	private fun isBeneathModal(target: UiComponentRo?): Boolean {
		if (_currentPopUps.isEmpty() || target === modalFillContainer || target == null) return false
		// Prevent focusing anything below the modal layer.
		val lastModalIndex = _currentPopUps.indexOfLast { it.isModal }
		if (lastModalIndex == -1) return false // no modals
		for (i in _currentPopUps.lastIndex downTo lastModalIndex) {
			if (_currentPopUps[i].child.isAncestorOf(target)) {
				return false
			}
		}
		return true
	}

	private fun focusChangingHandler(old: UiComponentRo?, new: UiComponentRo?, cancel: Cancel) {
		if (!isBeneathModal(new)) return
		if (old === modalFillContainer && new === stage) {
			cancel.cancel()
			return
		}
		val lastModalIndex = _currentPopUps.indexOfLast { it.isModal }
		val focusables = focusManager.focusables
		val focusIndex = focusables.indexOf(new)
		val isForwards = focusIndex >= focusables.indexOf(old)

		val toFocus: UiComponentRo = if (isForwards) {
			if (old == modalFillContainer) {
				var firstFocusable: UiComponentRo = modalFillContainer
				for (i in lastModalIndex.._currentPopUps.lastIndex) {
					firstFocusable = _currentPopUps[i].child.firstFocusable ?: continue
					break
				}
				firstFocusable
			} else {
				modalFillContainer
			}
		} else {
			if (old == modalFillContainer) {
				var lastFocusable: UiComponentRo = modalFillContainer
				for (i in _currentPopUps.lastIndex downTo lastModalIndex) {
					lastFocusable = _currentPopUps[i].child.lastFocusable ?: continue
					break
				}
				lastFocusable
			} else {
				modalFillContainer
			}
		}
		toFocus.focusSelf()
		cancel.cancel()
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
		removePopUp(popUpInfo)
		val child = popUpInfo.child
		if (child is Closeable)
			child.closed.add(::childClosedHandler)
		child.disposed.add(::popUpChildDisposedHandler)
		val index = _currentPopUps.sortedInsertionIndex(popUpInfo)
		_currentPopUps.add(index, popUpInfo)
		if (index == _currentPopUps.lastIndex)
			addElement(child)
		else
			addElementBefore(child, _currentPopUps[index + 1].child)
		refresh()
		if (popUpInfo.focus)
			child.focus(popUpInfo.highlightFocused)
		child.layoutData = popUpInfo.layoutData
	}

	override fun <T : UiComponent> removePopUp(popUpInfo: PopUpInfo<T>) {
		val wasFocused = popUpInfo.child.isFocused
		val removed = _currentPopUps.remove(popUpInfo)
		if (!removed) return // Pop-up not found
		val child = popUpInfo.child
		removeElement(child)
		if (child is Closeable)
			child.closed.remove(::childClosedHandler)
		child.disposed.remove(::popUpChildDisposedHandler)
		popUpInfo.onClosed(child)
		if (popUpInfo.dispose && !child.disposed.isDispatching && !child.isDisposed)
			child.dispose()
		refresh()
		val currentPopUp = _currentPopUps.lastOrNull()
		if (wasFocused) {
			if (currentPopUp?.focus == true) {
				currentPopUp.child.focus(currentPopUp.highlightFocused)
			} else {
				modalFillContainer.focusSelf()
			}
		}
	}

	private fun childClosedHandler(child: Closeable) {
		removePopUp(child as UiComponent)
	}

	private fun popUpChildDisposedHandler(child: UiComponent) {
		removePopUp(child)
	}

	override fun dispose() {
		clear()
		super.dispose()
	}
}

fun <T : UiComponent> Scoped.addPopUp(popUpInfo: PopUpInfo<T>): T {
	inject(PopUpManager).addPopUp(popUpInfo)
	return popUpInfo.child
}

fun Scoped.removePopUp(popUpInfo: PopUpInfo<*>) {
	inject(PopUpManager).removePopUp(popUpInfo)
}

fun Scoped.removePopUp(popUpInfoChild: UiComponent) {
	inject(PopUpManager).removePopUp(popUpInfoChild)
}
