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

@file:Suppress("LeakingThis", "UNUSED_PARAMETER")

package com.acornui.component

import com.acornui.assertionsEnabled
import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.SizeConstraintsRo
import com.acornui.component.layout.TransformableRo
import com.acornui.component.style.*
import com.acornui.core.*
import com.acornui.core.assets.AssetManager
import com.acornui.core.di.*
import com.acornui.core.graphics.Camera
import com.acornui.core.graphics.CameraRo
import com.acornui.core.graphics.Window
import com.acornui.core.input.InteractionEvent
import com.acornui.core.input.InteractionType
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.MouseState
import com.acornui.core.time.TimeDriver
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2
import com.acornui.signal.StoppableSignal
import kotlin.collections.set

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ComponentDslMarker

typealias ComponentInit<T> = (@ComponentDslMarker T).() -> Unit

interface UiComponentRo : LifecycleRo, ColorTransformableRo, InteractiveElementRo, Validatable, StyleableRo {

	override val disposed: Signal<(UiComponentRo) -> Unit>
	override val activated: Signal<(UiComponentRo) -> Unit>
	override val deactivated: Signal<(UiComponentRo) -> Unit>
	override val invalidated: Signal<(UiComponentRo, Int) -> Unit>

	override val parent: ContainerRo?

	/**
	 * Given a global position, casts a ray in the direction of the camera, returning the deepest enabled interactive
	 * element at that position.
	 * If there are multiple objects at this position, only the top-most object is returned. (by child index, not z
	 * value)
	 */
	fun getChildUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean): UiComponentRo?

	fun getChildUnderRay(globalRay: RayRo, onlyInteractive: Boolean): UiComponentRo?

	/**
	 * Given a screen position, casts a ray in the direction of the camera, populating the [out] list with the
	 * components
	 *
	 * @param canvasX The x coordinate relative to the canvas.
	 * @param canvasY The y coordinate relative to the canvas.
	 * @param out The array list to populate with elements.
	 */
	fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, out: MutableList<UiComponentRo>, onlyInteractive: Boolean): MutableList<UiComponentRo>

	/**
	 * @param globalRay The ray in global coordinate space to check for intersections.
	 * @param out The array list to populate with the interactive elements that match the criteria.
	 * @param returnAll If true, all intersecting elements will be added to [out], if false, only the top-most element.
	 * @param onlyInteractive If true, only elements where [InteractiveElement.interactivityEnabled] is true.
	 *
	 * This method will not return elements where [UiComponent.visible] is false.
	 */
	fun getChildrenUnderRay(globalRay: RayRo, out: MutableList<UiComponentRo>, onlyInteractive: Boolean, returnAll: Boolean)

	fun getChildrenUnderRay(globalRay: RayRo, out: MutableList<UiComponentRo>, onlyInteractive: Boolean)

	/**
	 * If false, this component will not be rendered, interact with user input, included in layouts, or included in
	 * focus order.
	 */
	val visible: Boolean

	/**
	 * Set this to false to make this layout element not included in layout algorithms.
	 */
	val includeInLayout: Boolean

	/**
	 * Returns true if this component will be rendered. This will be true under the following conditions:
	 * This component is on the stage.
	 * This component and all of its ancestors are visible.
	 * This component does not have an alpha of 0f.
	 */
	fun isRendered(): Boolean
}

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

	fun onAncestorVisibleChanged(uiComponent: UiComponent, value: Boolean)

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
	 * Updates this component, validating it and its children.
	 */
	fun update()

	/**
	 * Render any graphics.
	 */
	fun render()

}

/**
 * The base for every AcornUi component.
 * UiComponent provides lifecycle, validation, interactivity, transformation, and layout.
 *
 * @author nbilyk
 *
 * @param owner The creator of this component. This is used for dependency injection, style inheritance, and when
 * the owner has been disposed, this component will then be disposed.
 * @param native Some back-ends (like DomApplication) will create native components that the acorn display hierarchy
 * controls.
 */
open class UiComponentImpl(
		override final val owner: Owned,
		override final val native: NativeComponent = owner.inject(NativeComponent.FACTORY_KEY)(owner)
) : UiComponent {

	override final val injector = owner.injector

	//---------------------------------------------------------
	// Lifecycle
	//---------------------------------------------------------

	protected val _activated = Signal1<UiComponent>()
	override val activated: Signal<(UiComponent) -> Unit>
		get() = _activated
	protected val _deactivated = Signal1<UiComponent>()
	override val deactivated: Signal<(UiComponent) -> Unit>
		get() = _deactivated
	protected val _disposed = Signal1<UiComponent>()
	override val disposed: Signal<(UiComponent) -> Unit>
		get() = _disposed

	protected var _isDisposed: Boolean = false
	protected var _isActive: Boolean = false

	override val isActive: Boolean
		get() = _isActive

	override val isDisposed: Boolean
		get() = _isDisposed

	final override fun activate() {
		if (_isDisposed)
			throw IllegalStateException("Disposed")
		if (_isActive)
			throw IllegalStateException("Already active")
		_isActive = true
		onActivated()
		_activated.dispatch(this)
	}

	protected open fun onActivated() {}

	final override fun deactivate() {
		if (_isDisposed) throw IllegalStateException("Disposed")
		if (!_isActive) throw IllegalStateException("Not active")
		_isActive = false
		onDeactivated()
		_deactivated.dispatch(this)
	}

	protected open fun onDeactivated() {
	}

	/**
	 * If true, the native component will be auto-sized to the measured bounds from updateLayout.
	 */
	protected var nativeAutoSize = true

	protected val window = inject(Window)
	protected val mouse = inject(MouseState)
	protected val assets = inject(AssetManager)
	protected val interactivity = inject(InteractivityManager)
	protected val timeDriver = inject(TimeDriver)

	private val defaultCamera = inject(Camera)

	// Validatable Properties
	private val _invalidated = own(Signal2<UiComponent, Int>())
	override final val invalidated: Signal<(UiComponent, Int) -> Unit>
		get() = _invalidated

	/**
	 * The root of the validation tree. This is a tree representing how validation flags are resolved.
	 * This may be manipulated, but only on construction.
	 */
	protected var validation: ValidationTree
	private var _isValidating: Boolean = false

	// Transformable properties
	protected val _transform: Matrix4 = Matrix4()
	protected val _concatenatedTransform: Matrix4 = Matrix4()
	protected val _concatenatedTransformInv: Matrix4 = Matrix4()
	protected var _concatenatedTransformInvIsValid: Boolean = false
	protected val _position: Vector3 = Vector3(0f, 0f, 0f)
	protected val _rotation = Vector3(0f, 0f, 0f)
	protected val _scale: Vector3 = Vector3(1f, 1f, 1f)
	protected val _origin: Vector3 = Vector3(0f, 0f, 0f)

	override var cameraOverride: CameraRo? = null

	/**
	 * True if no scaling or rotation has been applied.
	 */
	protected var isSimpleTranslate: Boolean = true

	// LayoutElement properties
	protected var _bounds = Bounds()
	protected var _explicitWidth: Float? = null
	protected var _explicitHeight: Float? = null
	protected var _layoutData: LayoutData? = null
	protected var _includeInLayout: Boolean = true
	protected val _explicitSizeConstraints: SizeConstraints = SizeConstraints()
	protected val _sizeConstraints: SizeConstraints = SizeConstraints()

	// InteractiveElement properties
	protected var _inheritedInteractivityMode = InteractivityMode.ALL
	override final val inheritedInteractivityMode: InteractivityMode
		get() {
			validate(ValidationFlags.INTERACTIVITY_MODE)
			return _inheritedInteractivityMode
		}

	protected var _interactivityMode: InteractivityMode = InteractivityMode.ALL
	override final var interactivityMode: InteractivityMode
		get() = _interactivityMode
		set(value) {
			if (value != _interactivityMode) {
				_interactivityMode = value
				invalidate(ValidationFlags.INTERACTIVITY_MODE)
			}
		}

	override val interactivityEnabled: Boolean
		get() = inheritedInteractivityMode == InteractivityMode.ALL

	override fun <T : InteractionEvent> handlesInteraction(type: InteractionType<T>): Boolean {
		return handlesInteraction(type, true) || handlesInteraction(type, false)
	}

	override fun <T : InteractionEvent> handlesInteraction(type: InteractionType<T>, isCapture: Boolean): Boolean {
		return getInteractionSignal<InteractionEvent>(type, isCapture) != null
	}

	protected val _captureSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	protected val _bubbleSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	protected val _attachments = HashMap<Any, Any>()

	// ColorTransformable properties
	protected val _colorTint: Color = Color.WHITE.copy()
	protected val _concatenatedColorTint: Color = Color.WHITE.copy()

	// UiComponent properties
	protected var _visible: Boolean = true

	// ChildRo properties
	override var parent: ContainerRo? = null

	init {
		owner.disposed.add(this::ownerDisposedHandler)
		val r = this
		validation = validationTree {
			ValidationFlags.apply {
				addNode(STYLES, r::updateStyles)
				addNode(PROPERTIES, STYLES, r::updateProperties)
				addNode(HIERARCHY_ASCENDING, PROPERTIES, r::updateHierarchyAscending)
				addNode(HIERARCHY_DESCENDING, PROPERTIES, r::updateHierarchyDescending)
				addNode(SIZE_CONSTRAINTS, HIERARCHY_DESCENDING or HIERARCHY_ASCENDING or PROPERTIES, r::validateSizeConstraints)
				addNode(LAYOUT, SIZE_CONSTRAINTS, r::validateLayout)
				addNode(TRANSFORM, r::updateTransform)
				addNode(CONCATENATED_TRANSFORM, HIERARCHY_DESCENDING or TRANSFORM, r::updateConcatenatedTransform)
				addNode(COLOR_TRANSFORM, r::updateColorTransform)
				addNode(CONCATENATED_COLOR_TRANSFORM, COLOR_TRANSFORM, r::updateConcatenatedColorTransform)
				addNode(INTERACTIVITY_MODE, r::updateInheritedInteractivityMode)
			}
		}
	}

	private fun ownerDisposedHandler(owner: Owned) {
		dispose()
	}

	//-----------------------------------------------
	// CameraElement
	//-----------------------------------------------

	override fun windowToLocal(windowCoord: Vector2): Vector2 {
		val ray = Ray.obtain()
		globalToLocal(camera.getPickRay(windowCoord.x, windowCoord.y, 0f, 0f, window.width, window.height, ray))
		rayToPlane(ray, windowCoord)
		ray.free()
		return windowCoord
	}

	override fun localToWindow(localCoord: Vector3): Vector3 {
		localToGlobal(localCoord)
		camera.project(localCoord, 0f, 0f, window.width, window.height)
		return localCoord
	}

	//-----------------------------------------------
	// UiComponent
	//-----------------------------------------------

	override final var visible: Boolean
		get() {
			return _visible
		}
		set(value) {
			if (_visible == value) return
			_visible = value
			native.visible = value
			if (value) {
				childWalkLevelOrder<UiComponent> {
					// This is only necessary because browsers cannot measure elements with `display: none`.
					// Otherwise we would just invalidate this component's HIERARCHY_ASCENDING.
					it.onAncestorVisibleChanged(this, value)
					TreeWalk.CONTINUE
				}
			}
			invalidate(ValidationFlags.HIERARCHY_ASCENDING)
		}

	override fun onAncestorVisibleChanged(uiComponent: UiComponent, value: Boolean) {
	}

	//-----------------------------------------------
	// LayoutElement
	//-----------------------------------------------

	override fun containsCanvasPoint(canvasX: Float, canvasY: Float): Boolean {
		if (!isActive) return false
		val ray = Ray.obtain()
		camera.getPickRay(canvasX, canvasY, 0f, 0f, window.width, window.height, ray)
		val b = intersectsGlobalRay(ray)
		ray.free()
		return b
	}

	override fun intersectsGlobalRay(globalRay: RayRo): Boolean {
		val v = Vector3.obtain()
		val ret = intersectsGlobalRay(globalRay, v)
		v.free()
		return ret
	}

	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
		val bounds = bounds
		val topLeft = Vector3.obtain()
		val topRight = Vector3.obtain()
		val bottomRight = Vector3.obtain()
		val bottomLeft = Vector3.obtain()
		topLeft.clear()
		topRight.set(bounds.width, 0f, 0f)
		bottomRight.set(bounds.width, bounds.height, 0f)
		bottomLeft.set(0f, bounds.height, 0f)
		localToGlobal(topLeft)
		localToGlobal(topRight)
		localToGlobal(bottomRight)
		localToGlobal(bottomLeft)

		val intersects = globalRay.intersects(topLeft, topRight, bottomRight, intersection) ||
				globalRay.intersects(topLeft, bottomLeft, bottomRight, intersection)

		topLeft.free()
		topRight.free()
		bottomRight.free()
		bottomLeft.free()
		return intersects
	}

	/**
	 * The actual bounds of this component.
	 */
	override val bounds: BoundsRo
		get() {
			validate(ValidationFlags.LAYOUT)
			return _bounds
		}

	/**
	 * The explicit width, as set by width(value)
	 * Typically one would use width() in order to retrieve the explicit or actual width.
	 */
	override val explicitWidth: Float?
		get() {
			return _explicitWidth
		}

	/**
	 * The explicit height, as set by height(value)
	 * Typically one would use height() in order to retrieve the explicit or actual height.
	 */
	override val explicitHeight: Float?
		get() {
			return _explicitHeight
		}

	override val width: Float
		get() = bounds.width

	override val height: Float
		get() = bounds.height

	override val right: Float
		get() = x + width

	override val bottom: Float
		get() = y + height

	/**
	 * Sets the explicit width. Set to null to use actual width.
	 */
	override fun width(value: Float?) {
		if (_explicitWidth == value) return
		if (value?.isNaN() == true) throw Exception("May not set the size to be NaN")
		_explicitWidth = value
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun height(value: Float?) {
		if (_explicitHeight == value) return
		if (value?.isNaN() == true) throw Exception("May not set the size to be NaN")
		_explicitHeight = value
		invalidate(ValidationFlags.LAYOUT)
	}

	override val shouldLayout: Boolean
		get() {
			return _includeInLayout && _visible
		}

	override final var includeInLayout: Boolean
		get() {
			return _includeInLayout
		}
		set(value) {
			if (_includeInLayout == value) return
			_includeInLayout = value
			invalidate(ValidationFlags.HIERARCHY_ASCENDING)
		}

	override fun isRendered(): Boolean {
		if (!isActive) return false
		if (concatenatedColorTint.a <= 0f) return false
		var p: UiComponentRo? = this
		while (p != null) {
			if (!p.visible) return false
			p = p.parent
		}
		return true
	}

	private fun layoutDataChangedHandler() {
		invalidate(ValidationFlags.LAYOUT)
		Unit
	}

	override var layoutData: LayoutData?
		get() {
			return _layoutData
		}
		set(value) {
			if (_layoutData == value) return
			if (_layoutData != null) {
				_layoutData!!.changed.remove(this::layoutDataChangedHandler)
			}
			_layoutData = value
			if (_layoutData != null) {
				_layoutData!!.changed.add(this::layoutDataChangedHandler)
			}
			invalidate(ValidationFlags.LAYOUT)
		}

	/**
	 * Returns the measured size constraints, bound by the explicit size constraints.
	 */
	override val sizeConstraints: SizeConstraintsRo
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints
		}

	/**
	 * Returns the explicit size constraints.
	 */
	override val explicitSizeConstraints: SizeConstraintsRo
		get() {
			return _explicitSizeConstraints
		}

	override val minWidth: Float?
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints.width.min
		}

	override fun minWidth(value: Float?) {
		_explicitSizeConstraints.width.min = value
		invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	}

	override val minHeight: Float?
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints.height.min
		}

	override fun minHeight(value: Float?) {
		_explicitSizeConstraints.height.min = value
		invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	}

	override val maxWidth: Float?
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints.width.max
		}

	override fun maxWidth(value: Float?) {
		_explicitSizeConstraints.width.max = value
		invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	}

	override val maxHeight: Float?
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints.height.max
		}

	override fun maxHeight(value: Float?) {
		_explicitSizeConstraints.height.max = value
		invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	}

	/**
	 * If set, when the layout is validated, if there was no explicit width,
	 * this value will be used instead.
	 */
	override final var defaultWidth: Float? by validationProp(null, ValidationFlags.LAYOUT)

	/**
	 * If set, when the layout is validated, if there was no explicit height,
	 * this height will be used instead.
	 */
	override var defaultHeight: Float? by validationProp(null, ValidationFlags.LAYOUT)

	/**
	 * Does the same thing as setting width and height individually.
	 */
	override fun setSize(width: Float?, height: Float?) {
		if (width?.isNaN() == true || height?.isNaN() == true) throw Exception("May not set the size to be NaN")
		if (_explicitWidth == width && _explicitHeight == height) return
		_explicitWidth = width
		_explicitHeight = height
		invalidate(ValidationFlags.LAYOUT)
	}

	/**
	 * Do not call this directly, use [validate(ValidationFlags.SIZE_CONSTRAINTS)]
	 */
	protected fun validateSizeConstraints() {
		_sizeConstraints.clear()
		updateSizeConstraints(_sizeConstraints)
		_sizeConstraints.bound(_explicitSizeConstraints)
	}

	/**
	 * Updates the measured size constraints object.
	 */
	protected open fun updateSizeConstraints(out: SizeConstraints) {
	}

	/**
	 * Do not call this directly, use [validate(ValidationFlags.LAYOUT)]
	 */
	protected fun validateLayout() {
		val sC = sizeConstraints
		val w = sC.width.clamp(_explicitWidth ?: defaultWidth)
		val h = sC.height.clamp(_explicitHeight ?: defaultHeight)
		_bounds.set(w ?: 0f, h ?: 0f)
		updateLayout(w, h, _bounds)
		if (assertionsEnabled && (_bounds.width.isNaN() || _bounds.height.isNaN()))
			throw Exception("Bounding measurements should not be NaN")

		if (nativeAutoSize)
			native.setSize(_bounds.width, _bounds.height)
	}

	/**
	 * Updates this LayoutElement's layout.
	 * This method should update the [out] [Rectangle] bounding measurements.
	 *
	 * @param explicitWidth The explicitWidth dimension. Null if the preferred width should be used.
	 * @param explicitHeight The explicitHeight dimension. Null if the preferred height should be used.
	 */
	protected open fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
	}

	//-----------------------------------------------
	// InteractiveElement
	//-----------------------------------------------


	override fun hasInteraction(): Boolean {
		return _captureSignals.isNotEmpty() || _bubbleSignals.isNotEmpty()
	}

	override fun <T : InteractionEvent> hasInteraction(type: InteractionType<T>) = hasInteraction(type, false) // TODO: KT-20451
	override fun <T : InteractionEvent> hasInteraction(type: InteractionType<T>, isCapture: Boolean): Boolean {
		return getInteractionSignal<InteractionEvent>(type, isCapture) != null
	}

	override fun <T : InteractionEvent> getInteractionSignal(type: InteractionType<T>) = getInteractionSignal(type, false) // TODO: KT-20451

	@Suppress("UNCHECKED_CAST")
	override fun <T : InteractionEvent> getInteractionSignal(type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T>? {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		return handlers[type] as StoppableSignal<T>?
	}

	override fun <T : InteractionEvent> addInteractionSignal(type: InteractionType<T>, signal: StoppableSignal<T>, isCapture: Boolean) {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		handlers[type] = signal
	}

	override fun <T : InteractionEvent> addInteractionSignal(type: InteractionType<T>, signal: StoppableSignal<T>) = addInteractionSignal(type, signal, false) // TODO: KT-20451

	override fun <T : InteractionEvent> removeInteractionSignal(type: InteractionType<T>) = removeInteractionSignal(type, false) // TODO: KT-20451
	override fun <T : InteractionEvent> removeInteractionSignal(type: InteractionType<T>, isCapture: Boolean) {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		handlers.remove(type)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> getAttachment(key: Any): T? {
		return _attachments[key] as T?
	}

	override fun setAttachment(key: Any, value: Any) {
		_attachments.put(key, value)
	}

	/**
	 * Removes an attachment added via [setAttachment]
	 */
	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> removeAttachment(key: Any): T? {
		return _attachments.remove(key) as T?
	}

	/**
	 * Sets the [out] vector to the local mouse coordinates.
	 * @return Returns the [out] vector.
	 */
	override fun mousePosition(out: Vector2): Vector2 {
		windowToLocal(mouse.mousePosition(out))
		return out
	}

	override fun mouseIsOver(): Boolean {
		if (!isActive || !mouse.overCanvas()) return false
		val stage = owner.injectOptional(Stage) ?: return false
		val e = stage.getChildUnderPoint(mouse.canvasX(), mouse.canvasY(), onlyInteractive = true) ?: return false
		return e.isDescendantOf(this)
	}

	//-----------------------------------------------
	// ColorTransformable
	//-----------------------------------------------

	/**
	 * The color tint of this component.
	 * The final pixel color value for the default shader is [colorTint * pixel]
	 *
	 * If this is modified directly, be sure to call [invalidate(ValidationFlags.COLOR_TRANSFORM)]
	 */
	override var colorTint: ColorRo
		get() {
			return _colorTint
		}
		set(value) {
			if (_colorTint == value) return
			_colorTint.set(value)
			invalidate(ValidationFlags.COLOR_TRANSFORM)
		}

	override var alpha: Float
		get() {
			return colorTint.a
		}
		set(value) {
			val t = colorTint
			if (t.a == value) return
			colorTint(t.r, t.g, t.b, value)
		}

	override fun colorTint(r: Float, g: Float, b: Float, a: Float) {
		_colorTint.set(r, g, b, a)
		invalidate(ValidationFlags.COLOR_TRANSFORM)
	}

	/**
	 * The color multiplier of this component and all ancestor color tints multiplied together.
	 * Do not set this directly, it will be overwritten on a [ValidationFlags.CONCATENATED_COLOR_TRANSFORM] validation.
	 * Retrieving this value validates [ValidationFlags.CONCATENATED_COLOR_TRANSFORM]
	 */
	override val concatenatedColorTint: ColorRo
		get() {
			validate(ValidationFlags.CONCATENATED_COLOR_TRANSFORM)
			return _concatenatedColorTint
		}

	/**
	 * Concatenates the color transform.
	 * Do not call this directly, use `validate(ValidationFlags.CONCATENATED_COLOR_TRANSFORM)`
	 */
	protected open fun updateColorTransform() {
		native.setColorTint(_colorTint)
	}

	protected open fun updateConcatenatedColorTransform() {
		val p = parent
		if (p == null) {
			_concatenatedColorTint.set(_colorTint)
		} else {
			_concatenatedColorTint.set(p.concatenatedColorTint).mul(_colorTint)
		}
		native.setConcatenatedColorTint(_concatenatedColorTint)
	}

	protected open fun updateInheritedInteractivityMode() {
		_inheritedInteractivityMode = _interactivityMode
		if (parent?.inheritedInteractivityMode == InteractivityMode.NONE)
			_inheritedInteractivityMode = InteractivityMode.NONE

		native.interactivityEnabled = _inheritedInteractivityMode == InteractivityMode.ALL
	}

	protected open fun updateHierarchyAscending() {}
	protected open fun updateHierarchyDescending() {}

	//-----------------------------------------------
	// Interactivity utility methods
	//-----------------------------------------------

	override fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, out: MutableList<UiComponentRo>, onlyInteractive: Boolean): MutableList<UiComponentRo> {
		val ray = Ray.obtain()
		camera.getPickRay(canvasX, canvasY, 0f, 0f, window.width, window.height, ray)
		getChildrenUnderRay(ray, out, onlyInteractive)
		ray.free()
		return out
	}

	override fun getChildUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean): UiComponentRo? {
		val ray = Ray.obtain()
		camera.getPickRay(canvasX, canvasY, 0f, 0f, window.width, window.height, ray)
		val element = getChildUnderRay(ray, onlyInteractive)
		ray.free()
		return element
	}

	override fun getChildUnderRay(globalRay: RayRo, onlyInteractive: Boolean): UiComponentRo? {
		val tmpList = arrayListObtain<UiComponentRo>()
		getChildrenUnderRay(globalRay, tmpList, onlyInteractive = onlyInteractive, returnAll = false)
		val first = tmpList.firstOrNull()
		arrayListPool.free(tmpList)
		return first
	}

	override fun getChildrenUnderRay(globalRay: RayRo, out: MutableList<UiComponentRo>, onlyInteractive: Boolean, returnAll: Boolean) {
		if (!_visible || (onlyInteractive && !interactivityEnabled)) return
		if (intersectsGlobalRay(globalRay)) {
			out.add(this)
		}
	}

	override fun getChildrenUnderRay(globalRay: RayRo, out: MutableList<UiComponentRo>, onlyInteractive: Boolean) = getChildrenUnderRay(globalRay, out, onlyInteractive, true) // TODO: KT-20451

	//-----------------------------------------------
	// Styleable
	//-----------------------------------------------

	override val styleParent: StyleableRo? by lazy {
		var p: Owned? = owner
		var s: Styleable? = null
		while (p != null) {
			if (p is Styleable) {
				s = p
				break
			}
			p = p.owner
		}
		s
	}

	private var _styles: StylesImpl? = null
	private val styles: StylesImpl
		get() {
			if (_styles == null) _styles = own(StylesImpl(this))
			return _styles!!
		}

	override final val styleTags: MutableList<StyleTag>
		get() = styles.styleTags

	override final val styleRules: MutableList<StyleRule<*>>
		get() = styles.styleRules

	override fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) = styles.getRulesByType(type, out)

	protected fun <T : Style> bind(style: T, calculator: StyleCalculator = CascadingStyleCalculator): T {
		styles.bind(style, calculator)
		return style
	}

	protected fun <T : Style> watch(style: T, priority: Float = 0f, callback: (T) -> Unit) = styles.watch(style, priority, callback)
	protected fun unwatch(style: Style) = styles.unwatch(style)

	protected fun unbind(style: StyleRo) = styles.unbind(style)

	override fun invalidateStyles() {
		invalidate(ValidationFlags.STYLES)
	}

	protected open fun updateStyles() {
		_styles?.validateStyles()
	}

	//-----------------------------------------------
	// Validatable properties
	//-----------------------------------------------


	/**
	 * Validates this component's properties. This is done before any layout or transformation
	 * validation happens.
	 * Do not call this directly, use `validate(ValidationFlags.PROPERTIES)`
	 */
	protected open fun updateProperties() {
	}

	//-----------------------------------------------
	// Transformable
	//-----------------------------------------------

	/**
	 * This component's transformation matrix.
	 * Responsible for positioning, scaling, rotation, etc.
	 *
	 * Do not modify this matrix directly, but instead use the exposed transformation properties:
	 * x, y, scaleX, scaleY, rotation
	 */
	override val transform: Matrix4Ro
		get() {
			validate(ValidationFlags.TRANSFORM)
			return _transform
		}

	private var _customTransform: Matrix4Ro? = null
	override var customTransform: Matrix4Ro?
		get() = _customTransform
		set(value) {
			isSimpleTranslate = false
			_customTransform = value
			invalidate(ValidationFlags.TRANSFORM)
		}


	override var rotationX: Float
		get() = _rotation.x
		set(value) {
			if (_rotation.x == value) return
			_rotation.x = value
			if (isSimpleTranslate && value != 0f) {
				isSimpleTranslate = false
			}
			invalidate(ValidationFlags.TRANSFORM)
			return
		}

	override var rotationY: Float
		get() = _rotation.y
		set(value) {
			if (_rotation.y == value) return
			_rotation.y = value
			if (isSimpleTranslate && value != 0f) {
				isSimpleTranslate = false
			}
			invalidate(ValidationFlags.TRANSFORM)
			return
		}

	/**
	 * Rotation around the Z axis
	 */
	override var rotation: Float
		get() = _rotation.z
		set(value) {
			if (_rotation.z == value) return
			_rotation.z = value
			if (isSimpleTranslate && value != 0f) {
				isSimpleTranslate = false
			}
			invalidate(ValidationFlags.TRANSFORM)
		}

	override fun setRotation(x: Float, y: Float, z: Float) {
		if (_rotation.x == x && _rotation.y == y && _rotation.z == z) return
		_rotation.set(x, y, z)
		if (isSimpleTranslate && (x != 0f || y != 0f || z != 0f)) {
			isSimpleTranslate = false
		}
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	//-----------------------------------------------
	// Transformation and translation methods
	//-----------------------------------------------

	override var x: Float
		get() = _position.x
		set(value) {
			if (value == _position.x) return
			_position.x = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var y: Float
		get() = _position.y
		set(value) {
			if (value == _position.y) return
			_position.y = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var z: Float
		get() = _position.z
		set(value) {
			if (value == _position.z) return
			_position.z = value
			if (isSimpleTranslate && value != 0f)
				isSimpleTranslate = false
			invalidate(ValidationFlags.TRANSFORM)
		}

	override val position: Vector3Ro
		get() = _position

	/**
	 * Does the same thing as setting width and height individually.
	 */
	override fun setPosition(x: Float, y: Float, z: Float) {
		if (x == _position.x && y == _position.y && z == _position.z) return
		_position.set(x, y, z)
		if (isSimpleTranslate && z != 0f)
			isSimpleTranslate = false
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	override fun setPosition(x: Float, y: Float) = setPosition(x, y, 0f) // TODO: KT-20451
	override fun setPosition(value: Vector3Ro) = setPosition(value.x, value.y, value.z) // TODO: KT-20451

	override fun moveTo(x: Float, y: Float, z: Float) {
		// Round after a small, but obscure offset, to avoid flip-flopping around the common case of 0.5f
		setPosition((x - 0.0136f).round(), (y - 0.0136f).round(), z)
	}

	override fun moveTo(x: Float, y: Float) = moveTo(x, y, 0f) // TODO: KT-20451
	override fun moveTo(value: Vector3Ro) = moveTo(value.x, value.y, value.z)

	override var scaleX: Float
		get() = _scale.x
		set(value) {
			if (_scale.x == value) return
			_scale.x = value
			if (isSimpleTranslate && value != 1f)
				isSimpleTranslate = false
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var scaleY: Float
		get() = _scale.y
		set(value) {
			if (_scale.y == value) return
			_scale.y = value
			if (isSimpleTranslate && value != 1f)
				isSimpleTranslate = false
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var scaleZ: Float
		get() = _scale.z
		set(value) {
			if (_scale.z == value) return
			_scale.z = value
			if (isSimpleTranslate && value != 1f)
				isSimpleTranslate = false
			invalidate(ValidationFlags.TRANSFORM)
		}

	override fun setScaling(x: Float, y: Float, z: Float) {
		if (_scale.x == x && _scale.y == y && _scale.z == z) return
		_scale.set(x, y, z)
		if (isSimpleTranslate && (x != 1f || y != 1f || z != 1f))
			isSimpleTranslate = false
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	override fun setScaling(x: Float, y: Float) = setScaling(x, y, 1f) // TODO: KT-20451

	override var originX: Float
		get() = _origin.x
		set(value) {
			if (_origin.x == value) return
			_origin.x = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var originY: Float
		get() = _origin.y
		set(value) {
			if (_origin.y == value) return
			_origin.y = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var originZ: Float
		get() = _origin.z
		set(value) {
			if (_origin.z == value) return
			_origin.z = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override fun setOrigin(x: Float, y: Float, z: Float) {
		if (_origin.x == x && _origin.y == y && _origin.z == z) return
		_origin.set(x, y, z)
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	override fun setOrigin(x: Float, y: Float) = setOrigin(x, y, 0f) // TODO: KT-20451

	/**
	 * Converts a coordinate from local coordinate space to global coordinate space.
	 * This will modify the provided coord parameter.
	 * @param localCoord The coordinate local to this Transformable. This will be mutated to become a global coordinate.
	 * @return Returns the coord
	 */
	override fun localToGlobal(localCoord: Vector3): Vector3 {
		if (isSimpleTranslate) {
			val v = concatenatedTransform.values
			localCoord.x += v[12]
			localCoord.y += v[13]
			localCoord.z += v[14]
		} else {
			concatenatedTransform.prj(localCoord)
		}
		return localCoord
	}

	/**
	 * Converts a coordinate from global coordinate space to local coordinate space.
	 * This will modify the provided coord parameter.
	 * @param globalCoord The coordinate in global space. This will be mutated to become a local coordinate.
	 * @return Returns the coord
	 */
	override fun globalToLocal(globalCoord: Vector3): Vector3 {
		concatenatedTransformInv.prj(globalCoord)
		return globalCoord
	}

	/**
	 * Converts a ray from local coordinate space to global coordinate space.
	 * This will modify the provided ray parameter.
	 * @param ray The ray local to this Transformable. This will be mutated to become a global ray.
	 * @return Returns the ray
	 */
	override fun localToGlobal(ray: Ray): Ray {
		ray.mul(concatenatedTransform)
		return ray
	}

	/**
	 * Converts a ray from global coordinate space to local coordinate space.
	 * This will modify the provided ray parameter.
	 *
	 * Note: This is a heavy operation as it performs a Matrix4 inversion.
	 *
	 * @param ray The ray in global space. This will be mutated to become a local coordinate.
	 * @return Returns the ray
	 */
	override fun globalToLocal(ray: Ray): Ray {
		ray.mul(concatenatedTransformInv)
		return ray
	}

	/**
	 * Calculates the intersection coordinates of the provided Ray (in local coordinate space) and this layout
	 * element's plane.
	 * @return Returns true if the provided Ray intersects with this plane, or false if the Ray is parallel.
	 */
	override fun rayToPlane(ray: RayRo, out: Vector2): Boolean {
		if (ray.direction.z == 0f) return false
		val m = -ray.origin.z * ray.directionInv.z
		out.x = ray.origin.x + m * ray.direction.x
		out.y = ray.origin.y + m * ray.direction.y
		return true
	}

	/**
	 * Converts a coordinate from this Transformable's coordinate space to the target coordinate space.
	 */
	override fun convertCoord(coord: Vector3, targetCoordSpace: TransformableRo): Vector3 {
		return targetCoordSpace.globalToLocal(localToGlobal(coord))
	}

	/**
	 * The global transform of this component, of all ancestor transforms multiplied together.
	 * Do not modify this matrix directly, it will be overwritten on a TRANSFORM validation.
	 */
	override val concatenatedTransform: Matrix4Ro
		get() {
			validate(ValidationFlags.CONCATENATED_TRANSFORM)
			return _concatenatedTransform
		}

	/**
	 * Returns the inverse concatenated transformation matrix.
	 */
	override val concatenatedTransformInv: Matrix4Ro
		get() {
			validate(ValidationFlags.CONCATENATED_TRANSFORM)
			if (!_concatenatedTransformInvIsValid) {
				_concatenatedTransformInvIsValid = true
				_concatenatedTransformInv.set(_concatenatedTransform)
				_concatenatedTransformInv.inv()
			}
			return _concatenatedTransformInv
		}

	/**
	 * Applies all operations to the transformation matrix.
	 * Do not call this directly, use [validate(ValidationFlags.TRANSFORM)]
	 */
	protected open fun updateTransform() {
		if (_customTransform != null) {
			_transform.set(_customTransform!!)
			native.setTransform(_transform)
			return
		}
		_transform.idt()
		if (isSimpleTranslate) {
			_transform.trn(_position.x - _origin.x, _position.y - _origin.y, 0f)
			native.setSimpleTranslate(_position.x - _origin.x, _position.y - _origin.y)
		} else {
			_transform.trn(_position)
			_transform.scl(_scale)
			if (!_rotation.isZero()) {
				quat.setEulerAnglesRad(_rotation.y, _rotation.x, _rotation.z)
				_transform.rotate(quat)
			}
			if (!_origin.isZero())
				_transform.translate(-_origin.x, -_origin.y, -_origin.z)
			native.setTransform(_transform)
		}
	}

	/**
	 * Updates this component's concatenatedTransform matrix, which is the parent's concatenatedTransform
	 * multiplied by this component's transform matrix.
	 *
	 * Do not call this directly, use [validate(ValidationFlags.CONCATENATED_TRANSFORM)]
	 */
	protected open fun updateConcatenatedTransform() {
		val p = parent
		if (p != null) {
			if (isSimpleTranslate) {
				_concatenatedTransform.set(p.concatenatedTransform).translate(_position.x - _origin.x, _position.y - _origin.y, _position.z - _origin.z)
			} else {
				_concatenatedTransform.set(p.concatenatedTransform).mul(_transform)
			}
		} else {
			_concatenatedTransform.set(_transform)
		}
		_concatenatedTransformInvIsValid = false
		native.setConcatenatedTransform(_concatenatedTransform)
	}

	/**
	 * Returns the camera to be used for this component.
	 */
	override val camera: CameraRo
		get() {
			if (cameraOverride != null) return cameraOverride!!
			val p = parent
			if (p != null) {
				return p.camera
			} else {
				return defaultCamera
			}
		}

	//-----------------------------------------------
	// Validatable
	//-----------------------------------------------

	override fun invalidate(flags: Int): Int {
		val flagsInvalidated: Int = validation.invalidate(flags)

		if (flagsInvalidated != 0) {
			window.requestRender()
			onInvalidated(flagsInvalidated)
			_invalidated.dispatch(this, flagsInvalidated)
		}
		return flagsInvalidated
	}

	protected open fun onInvalidated(flagsInvalidated: Int) {
	}

	/**
	 * Validates the specified flags for this component.
	 *
	 * @param flags A bit mask for which flags to validate. (Use -1 to validate all)
	 * Example: validate(ValidationFlags.LAYOUT or ValidationFlags.PROPERTIES) to validate both layout an properties.
	 * @param force If true, the provided flags will be validated, even if they are not currently invalid.
	 */
	override fun validate(flags: Int, force: Boolean) {
		if (_isValidating) return
		_isValidating = true
		validation.validate(flags, force)
		_isValidating = false
	}

	//-----------------------------------------------
	// Drivable
	//-----------------------------------------------

	override fun update() {
		validate()
	}

	//-----------------------------------------------
	// Renderable
	//-----------------------------------------------

	/**
	 * Responsible for rendering this component.
	 * Typically, custom components override the [draw] method.
	 */
	override fun render() {
		if (!visible || _concatenatedColorTint.a <= 0f) return // Nothing visible.
		draw()
	}

	/**
	 * The core drawing method for this component.
	 */
	protected open fun draw() {
	}

	//-----------------------------------------------
	// Disposable
	//-----------------------------------------------

	override fun dispose() {
		if (_isDisposed)
			throw IllegalStateException("Already disposed")
		if (isActive) deactivate()
		_disposed.dispatch(this)
		_disposed.dispose()
		_activated.dispose()
		_deactivated.dispose()
		owner.disposed.remove(this::ownerDisposedHandler)
		_isDisposed = true
		if (assertionsEnabled) {
			parentWalk {
				if (!owner.owns(it)) {
					throw Exception("this component must be removed before disposing.")
				}
				true
			}
		}

		layoutData = null

		// InteractiveElement
		// Dispose all disposable handlers.
		for (i in _captureSignals.values) {
			(i as? Disposable)?.dispose()
		}
		_captureSignals.clear()
		for (i in _bubbleSignals.values) {
			(i as? Disposable)?.dispose()
		}
		_bubbleSignals.clear()
		// Dispose all disposable attachments.
		for (i in _attachments.values) {
			(i as? Disposable)?.dispose()
		}
		_attachments.clear()
		native.dispose()
	}

	companion object {
		private val quat: Quaternion = Quaternion()
	}
}

interface NativeComponent : Disposable {

	var interactivityEnabled: Boolean

	/**
	 * Sets the visibility of this component.
	 */
	var visible: Boolean

	/**
	 * Return the actual bounds of the component.
	 * This is typically either the explicit dimensions, if they were set, or the measured dimensions.
	 */
	val bounds: BoundsRo

	/**
	 * Sets the explicit dimensions of this component. Null values represent using measured dimensions.
	 */
	fun setSize(width: Float?, height: Float?)

	/**
	 * Sets the transformation matrix.
	 * Do not use both this method and [setSimpleTranslate]
	 */
	fun setTransform(value: Matrix4Ro)

	/**
	 * An alternate to [setTransform] that applies only a simple translation.
	 * Do not use both this method and [setTransform]
	 */
	fun setSimpleTranslate(x: Float, y: Float)

	/**
	 * Sets the concatenated global transformation matrix.
	 */
	fun setConcatenatedTransform(value: Matrix4Ro)

	/**
	 * Sets the color tint.
	 */
	fun setColorTint(value: ColorRo)

	/**
	 * Sets the combined global color tint.
	 */
	fun setConcatenatedColorTint(value: ColorRo)

	companion object {
		val FACTORY_KEY = dKey<(owner: Owned) -> NativeComponent>()
	}
}

object NativeComponentDummy : NativeComponent {

	override var interactivityEnabled: Boolean = true

	override var visible: Boolean = true

	override val bounds: BoundsRo
		get() = throw UnsupportedOperationException("NativeComponentDummy does not have bounds.")

	override fun setSize(width: Float?, height: Float?) {
	}

	override fun setTransform(value: Matrix4Ro) {
	}

	override fun setSimpleTranslate(x: Float, y: Float) {
	}

	override fun setConcatenatedTransform(value: Matrix4Ro) {
	}

	override fun setColorTint(value: ColorRo) {
	}

	override fun setConcatenatedColorTint(value: ColorRo) {
	}

	override fun dispose() {
	}
}