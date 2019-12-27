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

@file:Suppress("PropertyName", "MemberVisibilityCanBePrivate")

package com.acornui.component

import com.acornui.ParentRo
import com.acornui.collection.*
import com.acornui.component.layout.intersectsGlobalRay
import com.acornui.di.Owned
import com.acornui.focus.invalidateFocusOrderDeep
import com.acornui.math.Ray
import com.acornui.math.RayRo
import com.acornui.math.Vector3
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

interface ContainerRo : UiComponentRo, ParentRo<UiComponentRo>

/**
 * An interface for a ui component that has child components.
 */
interface Container : UiComponent, ContainerRo


/**
 * The base class for a component that contains children.
 *
 * @author nbilyk
 */
open class ContainerImpl(
		owner: Owned
) : UiComponentImpl(owner), Container {

	/**
	 * The validation flags that, if a child has invalidated, will cause the same flags on this container to become
	 * invalidated.
	 */
	protected var bubblingFlags = defaultBubblingFlags

	/**
	 * These flags, when invalidated, will cascade down to this container's children.
	 */
	protected var cascadingFlags = defaultCascadingFlags

	protected val _children: MutableConcurrentList<UiComponent> = ChildrenList()
	override val children: List<UiComponentRo> = _children

	/**
	 * Appends a child to the display children.
	 */
	protected fun <T : UiComponent> addChild(child: T): T {
		return addChild(_children.size, child)
	}

	/**
	 * Appends a child to the display children. If the child is null, nothing will will happen.
	 */
	protected fun <T : UiComponent> addOptionalChild(child: T?): T? {
		if (child == null) return null
		return addChild(_children.size, child)
	}

	/**
	 * Adds a child to the display children at the given position. If the child is null, nothing will will happen.
	 */
	protected fun <T : UiComponent> addOptionalChild(index: Int, child: T?): T? {
		if (child == null) return null
		return addChild(index, child)
	}

	/**
	 * Adds or reorders the specified child to this container at the specified index.
	 * If the child is already added to a different container, an error will be thrown.
	 * @param index The index of where to insert the child.
	 */
	protected fun <T : UiComponent> addChild(index: Int, child: T): T {
		_children.add(index, child)
		return child
	}

	/**
	 * Removes a child from the display children.
	 */
	protected fun removeChild(child: UiComponent?): Boolean {
		if (child == null) return false
		return _children.remove(child)
	}

	/**
	 * Removes a child at the given index from this container.
	 * @return Returns true if a child was removed, or false if the index was out of range.
	 */
	protected fun removeChild(index: Int): UiComponent {
		return _children.removeAt(index)
	}

	/**
	 * Removes all children, optionally disposing them.
	 */
	protected fun clearChildren(dispose: Boolean = true) {
		val c = children
		while (c.isNotEmpty()) {
			val child = removeChild(c.lastIndex)
			if (dispose)
				child.dispose()
		}
	}

	//-----------------------------------------------------------------------

	override fun onActivated() {
		super.onActivated()
		_children.iterate { child ->
			if (!child.isActive)
				child.activate()
			true
		}
	}

	override fun onDeactivated() {
		_children.iterate { child ->
			if (child.isActive)
				child.deactivate()
			true
		}
	}

	//-------------------------------------------------------------------------------------------------

	override fun onInvalidated(flagsInvalidated: Int) {
		val flagsToCascade = flagsInvalidated and cascadingFlags
		if (flagsToCascade > 0) {
			// This component has flags that have been invalidated that must cascade down to the children.
			_children.iterate { child ->
				child.invalidate(flagsToCascade)
				true
			}
		}
	}

	private val childrenUpdateIterator = _children.concurrentIterator()
	private val vec3Tmp = Vector3()

	override fun update() {
		super.update()
		childrenUpdateIterator.iterate {
			it.update()
			true
		}
	}

	override fun draw() {
		// The children list shouldn't be modified during a draw, so no reason to do a safe iteration here.
		_children.forEach2 { child ->
			if (child.includeInRender)
				child.render()
		}
	}

	//-----------------------------------------------------
	// Interactivity utility methods
	//-----------------------------------------------------

	private val rayTmp = Ray()

	override fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean, returnAll: Boolean, out: MutableList<UiComponentRo>, rayCache: RayRo?): MutableList<UiComponentRo> {
		if (!visible || (onlyInteractive && interactivityModeInherited == InteractivityMode.NONE)) return out
		val ray = rayCache ?: getPickRay(canvasX, canvasY, rayTmp)
		if (interactivityMode == InteractivityMode.ALWAYS || intersectsGlobalRay(ray)) {
			if ((returnAll || out.isEmpty())) {
				_children.forEachReversed2 { child ->
					val childRayCache = if (child.cameraEquals(this)) ray else null
					child.getChildrenUnderPoint(canvasX, canvasY, onlyInteractive, returnAll, out, childRayCache)
					// Continue iterating if we haven't found an intersecting child yet, or if returnAll is true.
					returnAll || out.isEmpty()
				}
			}
			if ((returnAll || out.isEmpty()) && (!onlyInteractive || interactivityEnabled)) {
				// This component intersects with the ray, but none of its children do.
				out.add(this)
			}
		}
		return out
	}

	//-----------------------------------------------------
	// Utility
	//-----------------------------------------------------

	/**
	 * Creates a dummy placeholder component which maintains the child index position.
	 */
	protected fun <T : UiComponent> createSlot(disposeOld: Boolean = true): ReadWriteProperty<Any?, T?> {
		val placeholder = addChild(UiComponentImpl(this))
		return Delegates.observable(null as T?) { _, oldValue, newValue ->
			if (oldValue !== newValue) {
				val index = _children.indexOf(oldValue ?: placeholder)
				removeChild(index)
				if (disposeOld)
					oldValue?.dispose()
				addChild(index, newValue ?: placeholder)
			}
		}
	}

	/**
	 * True if the current validation step is on layout or size constraints.
	 */
	protected val isValidatingLayout: Boolean
		get() = validation.currentFlag == ValidationFlags.LAYOUT

	protected open fun onChildInvalidated(child: UiComponent, flagsInvalidated: Int) {
		if (flagsInvalidated and child.layoutInvalidatingFlags > 0) {
			// A child has invalidated a flag marked as layout invalidating.
			if (!isValidatingLayout && (child.shouldLayout || flagsInvalidated and ValidationFlags.LAYOUT_ENABLED > 0)) {
				// If we are currently within a layout validation, do not attempt another invalidation.
				// If the child isn't laid out (invisible or includeInLayout is false), don't invalidate the layout
				// unless shouldLayout has just changed.
				invalidateLayout()
			}
		}
		invalidate(flagsInvalidated and bubblingFlags)
	}

	protected open fun onChildDisposed(child: UiComponent) {
		removeChild(child)
	}

	//-----------------------------------------------------
	// Disposable
	//-----------------------------------------------------

	/**
	 * Disposes this container, removes all its children.
	 * Components with this container as the owner will be disposed as well.
	 */
	override fun dispose() {
		clearChildren(dispose = false)
		super.dispose()
	}

	init {
		focusEnabledChildren = true
	}

	private inner class ChildrenList : MutableListBase<UiComponent>(), MutableConcurrentList<UiComponent> {

		private val list = ConcurrentListImpl<UiComponent>()

		override fun add(index: Int, element: UiComponent) {
			check(!isDisposed) { "This Container is disposed." }
			check(!element.isDisposed) { "Added child is disposed." }
			check(index >= 0 && index <= list.size) { "index is out of bounds." }
			val maybeLayout = if (!isValidatingLayout && element.layoutInvalidatingFlags and ValidationFlags.LAYOUT_ENABLED > 0) ValidationFlags.LAYOUT else 0
			if (element.parent == this@ContainerImpl) {
				// Reorder child.
				val oldIndex = list.indexOf(element)
				val newIndex = if (index > oldIndex) index - 1 else index
				list.removeAt(oldIndex)
				list.add(newIndex, element)
				invalidate(bubblingFlags or maybeLayout)
				element.invalidateFocusOrderDeep()
				return
			} else {
				check(element.parent == null) { "Remove child first." }
			}

			configureChild(element)
			list.add(index, element)
			invalidate(bubblingFlags or maybeLayout)
		}

		override fun removeAt(index: Int): UiComponent {
			check(!isDisposed) { "This Container is disposed." }

			val element = list.removeAt(index)
			unconfigureChild(element)
			val maybeLayout = if (!isValidatingLayout && element.layoutInvalidatingFlags and ValidationFlags.LAYOUT_ENABLED > 0) ValidationFlags.LAYOUT else 0
			invalidate(bubblingFlags or maybeLayout)
			return element
		}

		override val size: Int
			get() = list.size

		override fun get(index: Int): UiComponent = list[index]

		override fun set(index: Int, element: UiComponent): UiComponent {
			check(!isDisposed) { "This Container is disposed." }
			check(!element.isDisposed) { "Set child is disposed." }

			val maybeLayout = if (!isValidatingLayout && element.layoutInvalidatingFlags and ValidationFlags.LAYOUT_ENABLED > 0) ValidationFlags.LAYOUT else 0
			val oldChild = list[index]
			unconfigureChild(oldChild)
			configureChild(element)
			list[index] = element

			invalidate(bubblingFlags or maybeLayout)
			return oldChild
		}

		private fun configureChild(element: UiComponent) {
			element.parent = this@ContainerImpl
			element.invalidated.add(::onChildInvalidated)
			element.disposed.add(::onChildDisposed)
			if (isActive) element.activate()
			element.invalidate(cascadingFlags)
		}

		private fun unconfigureChild(oldChild: UiComponent) {
			oldChild.parent = null
			oldChild.invalidated.remove(::onChildInvalidated)
			oldChild.disposed.remove(::onChildDisposed)
			if (oldChild.isActive) oldChild.deactivate()
			oldChild.invalidate(cascadingFlags)
			oldChild.invalidateFocusOrderDeep()
		}

		override fun iterate(body: (UiComponent) -> Boolean) = list.iterate(body)
		override fun iterateReversed(body: (UiComponent) -> Boolean) = list.iterateReversed(body)

		override fun concurrentIterator(): MutableConcurrentListIterator<UiComponent> = list.concurrentIterator()
	}

	companion object {

		var defaultBubblingFlags = ValidationFlags.HIERARCHY_ASCENDING

		var defaultCascadingFlags = ValidationFlags.HIERARCHY_DESCENDING or
				ValidationFlags.STYLES or
				ValidationFlags.INHERITED_PROPERTIES or
				ValidationFlags.TRANSFORM or
				ValidationFlags.COLOR_TINT or
				ValidationFlags.VIEW_PROJECTION
	}
}

@JvmName("containerT")
inline fun <E : UiComponent> Owned.container(init: ComponentInit<ElementContainerImpl<E>> = {}): ElementContainerImpl<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ElementContainerImpl<E>(this).apply(init)
}

fun Owned.container(init: ComponentInit<ElementContainerImpl<UiComponent>> = {}): ElementContainerImpl<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ElementContainerImpl<UiComponent>(this).apply(init)
}
