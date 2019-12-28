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

@file:Suppress("UNUSED_PARAMETER", "RedundantLambdaArrow", "ObjectPropertyName", "MemberVisibilityCanBePrivate", "PropertyName")

package com.acornui.component

import com.acornui.ChildRo
import com.acornui.Lifecycle
import com.acornui.LifecycleRo
import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.style.Styleable
import com.acornui.component.style.StyleableRo
import com.acornui.di.Owned
import com.acornui.di.inject
import com.acornui.di.injectOptional
import com.acornui.focus.Focusable
import com.acornui.input.MouseState
import com.acornui.math.MinMaxRo
import com.acornui.math.RayRo
import com.acornui.math.Vector2
import com.acornui.signal.Signal

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ComponentDslMarker

typealias ComponentInit<T> = (@ComponentDslMarker T).() -> Unit

interface UiComponentRo : LifecycleRo, ColorTransformableRo, InteractiveElementRo, Validatable, StyleableRo, ChildRo, Focusable {

	override val disposed: Signal<(UiComponentRo) -> Unit>
	override val activated: Signal<(UiComponentRo) -> Unit>
	override val deactivated: Signal<(UiComponentRo) -> Unit>
	override val invalidated: Signal<(UiComponentRo, Int) -> Unit>

	override val parent: ContainerRo?

	/**
	 * Given a screen position, casts a ray in the direction of the camera, populating the [out] list with the
	 * components that intersect the ray.
	 *
	 * @param canvasX The x coordinate relative to the canvas.
	 * @param canvasY The y coordinate relative to the canvas.
	 * @param onlyInteractive If true, only elements whose interactivity is enabled will be returned.
	 * @param returnAll If true, all intersecting elements will be added to [out], if false, only the top-most element.
	 * The top-most element is determined by child index, not z value.
	 * @param out The array list to populate with elements.
	 * @param rayCache If the ray is already calculated, pass this to avoid re-calculating the pick ray from the camera.
	 */
	fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean, returnAll: Boolean, out: MutableList<UiComponentRo>, rayCache: RayRo? = null): MutableList<UiComponentRo>

	/**
	 * If false, this component will not be rendered, interact with user input, included in layouts, or included in
	 * focus order.
	 */
	val visible: Boolean

	/**
	 * If false, layout containers should not position or size this element.
	 */
	val includeInLayout: Boolean

	/**
	 * If false, containers should not render this element by default.
	 * This is different from [visible] in that this component is expected to have its [UiComponent.render] method
	 * manually called. This component will still invalidate its redraw regions.
	 */
	val includeInRender: Boolean

	/**
	 * Returns true if this component is active, visible, and has opacity.
	 *
	 * This will be true if this component and all its ancestors meet the conditions:
	 * [isActive], [visible], [alpha] > 0f
	 */
	val isRendered: Boolean

	/**
	 * The flags that, if invalidated, will invalidate the parent container's size constraints / layout.
	 */
	val layoutInvalidatingFlags: Int

	/**
	 * The clipping region, in canvas coordinates.
	 * This is used to early-out on rendering if the render contents is outside of this region.
	 * This is not done automatically; it is the responsibility of the component.
	 */
	val canvasClipRegion: MinMaxRo

	companion object {
		var defaultLayoutInvalidatingFlags = ValidationFlags.LAYOUT or
				ValidationFlags.LAYOUT_ENABLED
	}
}

/**
 * Traverses this ChildRo's ancestry, invoking a callback on each parent up the chain.
 * (including this object)
 * @param callback The callback to invoke on each ancestor. If this callback returns true, iteration will continue,
 * if it returns false, iteration will be halted.
 * @return If [callback] returned false, this method returns the element on which the iteration halted.
 */
inline fun UiComponentRo.parentWalk(callback: (UiComponentRo) -> Boolean): UiComponentRo? {
	var p: UiComponentRo? = this
	while (p != null) {
		val shouldContinue = callback(p)
		if (!shouldContinue) return p
		p = p.parent
	}
	return null
}

fun UiComponentRo.root(): UiComponentRo {
	var root: UiComponentRo = this
	var p: UiComponentRo? = this
	while (p != null) {
		root = p
		p = p.parent
	}
	return root
}


/**
 * Populates a [MutableList] with this component's ancestry.
 * @return Returns the [out] ArrayList
 */
fun UiComponentRo.ancestry(out: MutableList<UiComponentRo>): MutableList<UiComponentRo> {
	out.clear()
	parentWalk {
		out.add(it)
		true
	}
	return out
}

/**
 * Returns true if this [ChildRo] is the ancestor of the given [child].
 * X is considered to be an ancestor of Y if doing a parent walk starting from Y, X is then reached.
 * This will return true if X === Y
 */
fun UiComponentRo.isAncestorOf(child: UiComponentRo): Boolean {
	var isAncestor = false
	child.parentWalk {
		isAncestor = it === this
		!isAncestor
	}
	return isAncestor
}

fun UiComponentRo.isDescendantOf(ancestor: UiComponentRo): Boolean = ancestor.isAncestorOf(this)

interface UiComponent : UiComponentRo, Lifecycle, ColorTransformable, InteractiveElement, Styleable {

	override val disposed: Signal<(UiComponent) -> Unit>
	override val activated: Signal<(UiComponent) -> Unit>
	override val deactivated: Signal<(UiComponent) -> Unit>

	override val owner: Owned

	/**
	 * The parent on the display graph.
	 * This should only be set by the container.
	 */
	override var parent: ContainerRo?

	override val invalidated: Signal<(UiComponent, Int) -> Unit>

	override var visible: Boolean

	override var includeInLayout: Boolean
	override var includeInRender: Boolean

	override var layoutInvalidatingFlags: Int

	override var focusEnabled: Boolean
	override var focusOrder: Float
	override var isFocusContainer: Boolean

	/**
	 * If set, when the layout is validated, if there was no explicit width,
	 * this value will be used instead.
	 */
	var defaultWidth: Float?

	/**
	 * If set, when the layout is validated, if there was no explicit height,
	 * this height will be used instead.
	 */
	var defaultHeight: Float?

	/**
	 * If set, when the layout is validated, this baseline will be used instead of the measured value.
	 */
	var baselineOverride: Float?

	/**
	 * Updates this component, validating it and its children.
	 */
	fun update()

	/**
	 * Renders this component.
	 */
	fun render()
}

/**
 * Given a global position, casts a ray in the direction of the camera, returning the deepest enabled interactive
 * element at that position.
 * If there are multiple objects at this position, only the top-most object is returned. (by child index, not z
 * value)
 */
fun UiComponentRo.getChildUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean): UiComponentRo? {
	val out = arrayListObtain<UiComponentRo>()
	getChildrenUnderPoint(canvasX, canvasY, onlyInteractive, false, out)
	val first = out.firstOrNull()
	arrayListPool.free(out)
	return first
}

/**
 * Returns true if the current mouse position is over this component.
 */
fun UiComponentRo.mouseIsOver(): Boolean {
	val mouseState = inject(MouseState)
	if (!isActive || !mouseState.overCanvas) return false
	val stage = injectOptional(Stage) ?: return false
	val e = stage.getChildUnderPoint(mouseState.mouseX, mouseState.mouseY, onlyInteractive = true) ?: return false
	return e.isDescendantOf(this)
}

/**
 * Sets the [out] vector to the local mouse coordinates.
 * @return Returns the [out] vector.
 */
fun UiComponentRo.mousePosition(out: Vector2): Vector2 {
	val mouseState = inject(MouseState)
	canvasToLocal(out.set(mouseState.mouseX, mouseState.mouseY))
	return out
}

/**
 * Sets the [out] vector to the first touch coordinates.
 * @return Returns the [out] vector.
 */
fun UiComponentRo.touchPosition(out: Vector2): Vector2 {
	val mouseState = inject(MouseState)
	canvasToLocal(out.set(mouseState.touchX, mouseState.touchY))
	return out
}