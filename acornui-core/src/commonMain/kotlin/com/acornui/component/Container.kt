/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui._assert
import com.acornui.collection.ConcurrentListImpl
import com.acornui.component.layout.intersectsGlobalRay
import com.acornui.core.ParentRo
import com.acornui.core.di.Owned
import com.acornui.core.focus.invalidateFocusOrderDeep
import com.acornui.core.graphic.getPickRay
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

interface ContainerRo : UiComponentRo, ParentRo<UiComponentRo>

/**
 * An interface for a ui component that has child components.
 */
interface Container : UiComponent, ContainerRo


/**
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

	protected val _children = ConcurrentListImpl<UiComponent>()
	override val children: List<UiComponentRo>
		get() = _children

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
	 * Adds the specified child to this container.
	 * @param index The index of where to insert the child.
	 */
	protected fun <T : UiComponent> addChild(index: Int, child: T): T {
		_assert(!isDisposed, "This Container is disposed.")
		_assert(!child.isDisposed, "Added child is disposed.")
		if (child.parent == this) {
			// Reorder child.
			val oldIndex = _children.indexOf(child)
			val newIndex = if (index > oldIndex) index - 1 else index
			_children.removeAt(oldIndex)
			_children.add(newIndex, child)
			invalidate(bubblingFlags)
			child.invalidateFocusOrderDeep()
			return child
		}
		_assert(child.parent == null, "Remove child first.")
		if (index < 0 || index > _children.size)
			throw Exception("index is out of bounds.")

		child.parent = this
		_children.add(index, child)
		child.invalidated.add(::childInvalidatedHandler)
		child.disposed.add(::childDisposedHandler)

		if (isActive)
			child.activate()
		child.invalidate(cascadingFlags)
		invalidate(bubblingFlags)
		if (!isValidatingLayout)
			invalidateSize()

		return child
	}

	/**
	 * Adds a child after the [after] child.
	 */
	protected fun addChildAfter(child: UiComponent, after: UiComponent): Int {
		val index = _children.indexOf(after)
		if (index == -1) return -1
		addChild(index + 1, child)
		return index + 1
	}

	/**
	 * Adds a child before the [before] child.
	 */
	protected fun addChildBefore(child: UiComponent, before: UiComponent): Int {
		val index = _children.indexOf(before)
		if (index == -1) return -1
		addChild(index, child)
		return index
	}

	/**
	 * Removes a child from the display children.
	 */
	protected fun removeChild(child: UiComponent?): Boolean {
		if (child == null) return false
		val index = _children.indexOf(child)
		if (index == -1) return false
		removeChild(index)
		return true
	}

	/**
	 * Removes a child at the given index from this container.
	 * @return Returns true if a child was removed, or false if the index was out of range.
	 */
	protected fun removeChild(index: Int): UiComponent {
		_assert(!isDisposed, "This Container is disposed.")

		val child = _children.removeAt(index)
		child.parent = null

		child.invalidated.remove(::childInvalidatedHandler)
		child.disposed.remove(::childDisposedHandler)
		if (child.isActive) {
			child.deactivate()
		}
		invalidate(bubblingFlags)
		child.invalidate(cascadingFlags)
		child.invalidateFocusOrderDeep()
		if (!isValidatingLayout)
			invalidateSize()

		return child
	}

	/**
	 * Removes all children, optionally disposing them.
	 */
	protected fun clearChildren(dispose: Boolean = true) {
		val c = _children
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

	private val childrenIterator = _children.iteratorPool.obtain()

	override fun update() {
		super.update()
		val size = _children.size
		if (size == 0) return
		else if (size == 1) {
			_children[0].update()
		} else {
			val c = childrenIterator
			while (c.hasNext()) {
				c.next().update()
			}
			c.clear()
		}
	}

	private val childConcatenatedColorTransform = Color()
	private val childConcatenatedTransform = Matrix4()

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		// The children list shouldn't be modified during a draw, so no reason to do a safe iteration here.
		for (i in 0.._children.lastIndex) {
			val child = _children[i]
			if (child.visible) {
				childConcatenatedColorTransform.set(tint).mul(child.colorTint)
				childConcatenatedTransform.set(transform).mul(child.transform)
				child.render(clip, childConcatenatedTransform, childConcatenatedColorTransform)
			}
		}
	}

	//-----------------------------------------------------
	// Interactivity utility methods
	//-----------------------------------------------------

	private val rayTmp = Ray()

	override fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean, returnAll: Boolean, out: MutableList<UiComponentRo>, rayCache: RayRo?): MutableList<UiComponentRo> {
		if (!visible || (onlyInteractive && inheritedInteractivityMode == InteractivityMode.NONE)) return out
		val ray = rayCache ?: camera.getPickRay(canvasX, canvasY, viewport, rayTmp)
		if (interactivityMode == InteractivityMode.ALWAYS || intersectsGlobalRay(ray)) {
			if ((returnAll || out.isEmpty())) {
				for (i in _children.lastIndex downTo 0) {
					val child = _children[i]
					val childRayCache = if (child.camera === camera && child.viewport === viewport) ray else null
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
				val index = children.indexOf(oldValue ?: placeholder)
				removeChild(index)
				if (disposeOld)
					oldValue?.dispose()
				addChild(index, newValue ?: placeholder)
			}
		}
	}

	protected val isValidatingLayout: Boolean
		get() = validation.currentFlag == ValidationFlags.LAYOUT ||
				validation.currentFlag == ValidationFlags.SIZE_CONSTRAINTS

	protected open fun childInvalidatedHandler(child: UiComponent, flagsInvalidated: Int) {
		if (flagsInvalidated and child.layoutInvalidatingFlags > 0) {
			// A child has invalidated a flag marked as layout invalidating.
			if (!isValidatingLayout && (child.shouldLayout || flagsInvalidated and ValidationFlags.LAYOUT_ENABLED > 0)) {
				// If we are currently within a layout validation, do not attempt another invalidation.
				// If the child isn't laid out (invisible or includeInLayout is false), don't invalidate the layout
				// unless shouldLayout has just changed.
				invalidateSize()
			}
		}
		val bubblingFlags = flagsInvalidated and bubblingFlags
		if (bubblingFlags > 0) {
			invalidate(bubblingFlags)
		}
	}

	protected open fun childDisposedHandler(child: UiComponent) {
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

	companion object {

		var defaultBubblingFlags = ValidationFlags.HIERARCHY_ASCENDING

		var defaultCascadingFlags = ValidationFlags.HIERARCHY_DESCENDING or
				ValidationFlags.STYLES or
				ValidationFlags.CONCATENATED_TRANSFORM or
				ValidationFlags.INTERACTIVITY_MODE or
				ValidationFlags.CAMERA or
				ValidationFlags.VIEWPORT
	}
}

fun Owned.container(init: ComponentInit<ElementContainerImpl<UiComponent>> = {}): ElementContainerImpl<UiComponent> {
	val c = ElementContainerImpl<UiComponent>(this)
	c.init()
	return c
}
