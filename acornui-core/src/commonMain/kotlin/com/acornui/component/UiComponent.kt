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

@file:Suppress("UNUSED_PARAMETER", "RedundantLambdaArrow", "ObjectPropertyName")

package com.acornui.component

import com.acornui.assertionsEnabled
import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.layout.*
import com.acornui.component.style.*
import com.acornui.core.*
import com.acornui.core.asset.AssetManager
import com.acornui.core.di.*
import com.acornui.core.focus.*
import com.acornui.core.graphic.CameraRo
import com.acornui.core.graphic.Window
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.MouseState
import com.acornui.core.time.TimeDriver
import com.acornui.function.as1
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.reflect.observable
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2
import com.acornui.signal.StoppableSignal
import kotlin.collections.set
import kotlin.properties.Delegates

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ComponentDslMarker

typealias ComponentInit<T> = (@ComponentDslMarker T).() -> Unit

interface UiComponentRo : LifecycleRo, ColorTransformableRo, InteractiveElementRo, Validatable, StyleableRo, ChildRo, Focusable, Renderable {

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
	 * Set this to false to make this layout element not included in layout algorithms.
	 */
	val includeInLayout: Boolean

	/**
	 * Returns true if this component will be rendered. This will be true under the following conditions:
	 * This component is on the stage.
	 * No ancestor has [visible] false.
	 * No ancestor has [alpha] <= 0f.
	 */
	val isRendered: Boolean

	/**
	 * The flags that, if invalidated, will invalidate the parent container's size constraints / layout.
	 */
	val layoutInvalidatingFlags: Int

	/**
	 * The explicit width, as set by width(value)
	 * Typically one would use [width] in order to retrieve the actual width.
	 */
	val explicitWidth: Float?

	/**
	 * The explicit height, as set by height(value)
	 * Typically one would use [height] in order to retrieve actual height.
	 */
	val explicitHeight: Float?

	companion object {
		var defaultLayoutInvalidatingFlags = ValidationFlags.HIERARCHY_ASCENDING or
				ValidationFlags.LAYOUT or
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
	 * Updates this component, validating it and its children.
	 */
	fun update()

}

/**
 * The base for every AcornUi component.
 * UiComponent provides lifecycle, validation, interactivity, transformation, and layout.
 *
 * @author nbilyk
 *
 * @param owner The creator of this component. This is used for dependency injection, style inheritance, and when
 * the owner has been disposed, this component will then be disposed.
 * controls.
 */
open class UiComponentImpl(
		final override val owner: Owned
) : UiComponent {

	final override val injector = owner.injector

	//---------------------------------------------------------
	// Lifecycle
	//---------------------------------------------------------

	private val _activated = Signal1<UiComponent>()
	final override val activated = _activated.asRo()

	private val _deactivated = Signal1<UiComponent>()
	final override val deactivated = _deactivated.asRo()

	private val _disposed = Signal1<UiComponent>()
	final override val disposed = _disposed.asRo()

	private var _isDisposed: Boolean = false
	private var _isActive: Boolean = false

	override val isActive: Boolean
		get() = _isActive

	override val isDisposed: Boolean
		get() = _isDisposed

	final override fun activate() {
		if (_isDisposed)
			throw DisposedException()
		if (_isActive)
			throw IllegalStateException("Already active")
		_isActive = true
		_activated.dispatch(this)
	}

	protected open fun onActivated() {}

	final override fun deactivate() {
		if (_isDisposed)
			throw DisposedException()
		if (!_isActive)
			throw IllegalStateException("Not active")
		_isActive = false
		_deactivated.dispatch(this)
	}

	protected open fun onDeactivated() {
	}

	// Common dependencies
	protected val window by Window

	@Deprecated("use mouseState", ReplaceWith("mouseState"), DeprecationLevel.ERROR)
	protected val mouse by MouseState
	protected val mouseState by MouseState

	protected val assets by AssetManager
	protected val interactivity by InteractivityManager
	protected val timeDriver by TimeDriver
	protected val gl by Gl20
	protected val glState by GlState
	protected val stage by Stage

	// Validatable Properties
	private val _invalidated = own(Signal2<UiComponent, Int>())
	final override val invalidated = _invalidated.asRo()

	/**
	 * The root of the validation tree. This is a tree representing how validation flags are resolved.
	 * This may be manipulated, but only on construction.
	 */
	protected var validation: ValidationGraph

	// Transformable properties
	protected val _transform = Matrix4()
	protected val _position = Vector3(0f, 0f, 0f)
	protected val _rotation = Vector3(0f, 0f, 0f)
	protected val _scale = Vector3(1f, 1f, 1f)
	protected val _origin = Vector3(0f, 0f, 0f)

	// LayoutElement properties
	protected val _bounds = Bounds()
	protected val _explicitSizeConstraints = SizeConstraints()
	protected val _sizeConstraints = SizeConstraints()

	// InteractiveElement properties
	protected var _inheritedInteractivityMode = InteractivityMode.ALL
	final override val inheritedInteractivityMode: InteractivityMode
		get() {
			validate(ValidationFlags.INTERACTIVITY_MODE)
			return _inheritedInteractivityMode
		}

	protected var _interactivityMode: InteractivityMode = InteractivityMode.ALL
	final override var interactivityMode: InteractivityMode
		get() = _interactivityMode
		set(value) {
			if (value != _interactivityMode) {
				_interactivityMode = value
				when (value) {
					InteractivityMode.NONE -> blur()
					InteractivityMode.CHILDREN -> blurSelf()
					else -> {
					}
				}
				invalidate(ValidationFlags.INTERACTIVITY_MODE)
			}
		}

	override val interactivityEnabled: Boolean
		get() = inheritedInteractivityMode == InteractivityMode.ALL || inheritedInteractivityMode == InteractivityMode.ALWAYS

	override fun <T : InteractionEventRo> handlesInteraction(type: InteractionType<T>): Boolean {
		return handlesInteraction(type, true) || handlesInteraction(type, false)
	}

	override fun <T : InteractionEventRo> handlesInteraction(type: InteractionType<T>, isCapture: Boolean): Boolean {
		return getInteractionSignal<InteractionEventRo>(type, isCapture) != null
	}

	private val _captureSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	private val _bubbleSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	private val _attachments = HashMap<Any, Any>()

	// ColorTransformable properties
	protected val _colorTint: Color = Color.WHITE.copy()

	// ChildRo properties
	override var parent: ContainerRo? = null

	// Focusable properties
	protected val focusManager by FocusManager
	final override var focusEnabled: Boolean by observable(false) { _ -> invalidateFocusOrder() }
	final override var focusOrder by observable(0f) { _ -> invalidateFocusOrder() }
	final override var isFocusContainer by observable(false) { _ -> invalidateFocusOrderDeep() }
	final override var focusEnabledChildren by observable(false) { _ -> invalidateFocusOrderDeep() }

	// Render context properties

	protected val defaultRenderContext = inject(RenderContextRo)
	protected val _renderContext = RenderContext(defaultRenderContext)

	override val naturalRenderContext: RenderContextRo
		get() = _renderContext

	override val viewProjectionTransformInv: Matrix4Ro
		get() = renderContext.viewProjectionTransformInv

	override val viewProjectionTransform: Matrix4Ro
		get() = renderContext.viewProjectionTransform

	override val viewTransform: Matrix4Ro
		get() = renderContext.viewTransform

	override val projectionTransform: Matrix4Ro
		get() = renderContext.projectionTransform

	override val canvasTransform: RectangleRo
		get() = renderContext.canvasTransform

	override var renderContextOverride: RenderContextRo? = null
		set(value) {
			if (value != field) {
				invalidate(ValidationFlags.RENDER_CONTEXT)
				field = value
				update()
			}
		}

	var cameraOverride: CameraRo?
		get() = _renderContext.cameraOverride
		set(value) {
			_renderContext.cameraOverride = value
			invalidate(ValidationFlags.RENDER_CONTEXT)
		}

	//


	private val rayTmp = Ray()

	init {
		owner.disposed.add(::dispose.as1)
		validation = validationGraph {
			ValidationFlags.apply {
				addNode(STYLES, ::updateStyles)
				addNode(HIERARCHY_ASCENDING, ::updateHierarchyAscending)
				addNode(HIERARCHY_DESCENDING, ::updateHierarchyDescending)
				addNode(LAYOUT_ENABLED, ::updateLayoutEnabled)
				addNode(SIZE_CONSTRAINTS, STYLES, ::validateSizeConstraints)
				addNode(LAYOUT, SIZE_CONSTRAINTS, ::validateLayout)
				addNode(TRANSFORM, ::updateTransform)
				addNode(INTERACTIVITY_MODE, ::updateInheritedInteractivityMode)
				addNode(RENDER_CONTEXT, TRANSFORM, ::updateRenderContext)
				addNode(BITMAP_CACHE) {}
			}
		}

		_activated.add(::invalidateFocusOrder.as1)
		_activated.add(::onActivated.as1)
		_deactivated.add(::invalidateFocusOrder.as1)
		_deactivated.add(::onDeactivated.as1)
	}

	//-----------------------------------------------
	// UiComponent
	//-----------------------------------------------

	final override var visible: Boolean by validationProp(true, ValidationFlags.LAYOUT_ENABLED)

	//-----------------------------------------------
	// Focusable
	//-----------------------------------------------

	override val focusableStyle: FocusableStyle by lazy {
		bind(FocusableStyle()).apply {
			watch(this) {
				refreshFocusHighlight()
			}
		}
	}

	/**
	 * If set, the provided delegate will be highlighted instead of this component.
	 * The highlighter will still be obtained from this component's [focusableStyle].
	 */
	var focusHighlightDelegate: UiComponent? by observable(null, ::refreshFocusHighlight.as1)

	override var showFocusHighlight by observable(false, ::refreshFocusHighlight.as1)

	private var focusTarget: UiComponent? = null
	private var focusHighlighter: FocusHighlighter? = null

	private fun refreshFocusHighlight() {
		validate(ValidationFlags.STYLES)
		if (focusTarget != null)
			focusHighlighter?.unhighlight(focusTarget!!)
		if (showFocusHighlight) {
			focusTarget = focusHighlightDelegate ?: this
			focusHighlighter = focusableStyle.highlighter
			focusHighlighter?.highlight(focusTarget!!)
		} else {
			focusTarget = null
			focusHighlighter = null
		}
	}

	//-----------------------------------------------
	// LayoutElement
	//-----------------------------------------------

	override fun containsCanvasPoint(canvasX: Float, canvasY: Float): Boolean {
		if (!isActive) return false
		val ray = Ray.obtain()
		getPickRay(canvasX, canvasY, ray)
		val b = intersectsGlobalRay(ray)
		Ray.free(ray)
		return b
	}

	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
		val bounds = bounds
		// TODO: make these temp vars
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

		val intersects = globalRay.intersectsTriangle(topLeft, topRight, bottomRight, intersection) ||
				globalRay.intersectsTriangle(topLeft, bottomLeft, bottomRight, intersection)

		Vector3.free(topLeft)
		Vector3.free(topRight)
		Vector3.free(bottomRight)
		Vector3.free(bottomLeft)
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
	final override var explicitWidth: Float? = null
		private set

	/**
	 * The explicit height, as set by height(value)
	 * Typically one would use height() in order to retrieve the explicit or actual height.
	 */
	final override var explicitHeight: Float? = null
		private set

	/**
	 * Sets the explicit width. Set to null to use actual width.
	 */
	override fun width(value: Float?) {
		if (explicitWidth == value) return
		if (value?.isNaN() == true) throw Exception("May not set the size to be NaN")
		explicitWidth = value
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun height(value: Float?) {
		if (explicitHeight == value) return
		if (value?.isNaN() == true) throw Exception("May not set the size to be NaN")
		explicitHeight = value
		invalidate(ValidationFlags.LAYOUT)
	}

	override val shouldLayout: Boolean
		get() {
			return includeInLayout && visible
		}

	override var layoutInvalidatingFlags: Int = UiComponentRo.defaultLayoutInvalidatingFlags

	final override var includeInLayout: Boolean by validationProp(true, ValidationFlags.LAYOUT_ENABLED)

	override val isRendered: Boolean
		get() {
			if (!isActive) return false
			var p: UiComponentRo? = this
			while (p != null) {
				if (!p.visible || p.alpha <= 0f) return false
				p = p.parent
			}
			return true
		}

	private fun layoutDataChangedHandler() {
		invalidate(ValidationFlags.LAYOUT)
		Unit
	}

	final override var layoutData: LayoutData? by Delegates.observable<LayoutData?>(null) { _, old, new ->
		old?.changed?.remove(::layoutDataChangedHandler)
		new?.changed?.add(::layoutDataChangedHandler)
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
		get() = _explicitSizeConstraints

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
	final override var defaultWidth: Float? by validationProp(null, ValidationFlags.LAYOUT)

	/**
	 * If set, when the layout is validated, if there was no explicit height,
	 * this height will be used instead.
	 */
	final override var defaultHeight: Float? by validationProp(null, ValidationFlags.LAYOUT)

	/**
	 * Does the same thing as setting width and height individually.
	 */
	override fun setSize(width: Float?, height: Float?) {
		if (width?.isNaN() == true || height?.isNaN() == true) throw Exception("May not set the size to be NaN")
		if (explicitWidth == width && explicitHeight == height) return
		explicitWidth = width
		explicitHeight = height
		invalidate(ValidationFlags.LAYOUT)
	}

	/**
	 * Do not call this directly, use [validate(ValidationFlags.SIZE_CONSTRAINTS)]
	 */
	private fun validateSizeConstraints() {
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
	private fun validateLayout() {
		val sC = sizeConstraints
		val w = sC.width.clamp(explicitWidth ?: defaultWidth)
		val h = sC.height.clamp(explicitHeight ?: defaultHeight)
		_bounds.set(w ?: 0f, h ?: 0f)
		updateLayout(w, h, _bounds)
		if (assertionsEnabled && (_bounds.width.isNaN() || _bounds.height.isNaN()))
			throw Exception("Bounding measurements should not be NaN")
	}

	/**
	 * Updates this component's layout.
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


	final override fun hasInteraction(): Boolean {
		return _captureSignals.isNotEmpty() || _bubbleSignals.isNotEmpty()
	}

	final override fun <T : InteractionEventRo> hasInteraction(type: InteractionType<T>, isCapture: Boolean): Boolean {
		return getInteractionSignal<InteractionEventRo>(type, isCapture) != null
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : InteractionEventRo> getInteractionSignal(type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T>? {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		return handlers[type] as StoppableSignal<T>?
	}

	final override fun <T : InteractionEventRo> addInteractionSignal(type: InteractionType<T>, signal: StoppableSignal<T>, isCapture: Boolean) {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		handlers[type] = signal
	}


	final override fun <T : InteractionEventRo> removeInteractionSignal(type: InteractionType<T>, isCapture: Boolean) {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		handlers.remove(type)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> getAttachment(key: Any): T? {
		return _attachments[key] as T?
	}

	final override fun setAttachment(key: Any, value: Any) {
		_attachments[key] = value
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
		canvasToLocal(mouseState.mousePosition(out))
		return out
	}

	override fun mouseIsOver(): Boolean {
		if (!isActive || !mouseState.overCanvas) return false
		val stage = owner.injectOptional(Stage) ?: return false
		val e = stage.getChildUnderPoint(mouseState.canvasX, mouseState.canvasY, onlyInteractive = true) ?: return false
		return e.isDescendantOf(this)
	}

	//-----------------------------------------------
	// ColorTransformable
	//-----------------------------------------------

	/**
	 * The color tint of this component.
	 * The final pixel color value for the default shader is [colorTint * pixel]
	 */
	override var colorTint: ColorRo
		get() {
			return _colorTint
		}
		set(value) {
			if (_colorTint == value) return
			colorTint(value.r, value.g, value.b, value.a)
		}

	override fun colorTint(r: Float, g: Float, b: Float, a: Float) {
		_colorTint.set(r, g, b, a)
		invalidate(ValidationFlags.RENDER_CONTEXT)
	}

	/**
	 * The color multiplier of this component and all ancestor color tints multiplied together.
	 */
	override val concatenatedColorTint: ColorRo
		get() {
			validate(ValidationFlags.RENDER_CONTEXT)
			return renderContext.colorTint
		}

	protected open fun updateInheritedInteractivityMode() {
		_inheritedInteractivityMode = _interactivityMode
		if (parent?.inheritedInteractivityMode == InteractivityMode.NONE)
			_inheritedInteractivityMode = InteractivityMode.NONE
	}

	protected open fun updateHierarchyAscending() {}
	protected open fun updateHierarchyDescending() {}
	protected open fun updateLayoutEnabled() {}

	//-----------------------------------------------
	// Interactivity utility methods
	//-----------------------------------------------

	override fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean, returnAll: Boolean, out: MutableList<UiComponentRo>, rayCache: RayRo?): MutableList<UiComponentRo> {
		if (!visible || (onlyInteractive && !interactivityEnabled)) return out

		val ray = rayCache ?: getPickRay(canvasX, canvasY, rayTmp)
		if (interactivityMode == InteractivityMode.ALWAYS || intersectsGlobalRay(ray)) {
			out.add(this)
		}
		return out
	}

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
		s ?: stage
	}

	private var _styles: Styles? = null
	private val styles: Styles
		get() {
			if (_styles == null) _styles = own(Styles(this))
			return _styles!!
		}

	final override val styleTags: MutableList<StyleTag>
		get() = styles.styleTags

	final override val styleRules: MutableList<StyleRule<*>>
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

	override val invalidFlags: Int
		get() = validation.invalidFlags

	//-----------------------------------------------
	// Transformable
	//-----------------------------------------------

	override var snapToPixel: Boolean = Positionable.defaultSnapToPixel

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
			_customTransform = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var rotationX: Float
		get() = _rotation.x
		set(value) {
			if (_rotation.x == value) return
			_rotation.x = value
			invalidate(ValidationFlags.TRANSFORM)
			return
		}

	override var rotationY: Float
		get() = _rotation.y
		set(value) {
			if (_rotation.y == value) return
			_rotation.y = value
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
			invalidate(ValidationFlags.TRANSFORM)
		}

	override fun setRotation(x: Float, y: Float, z: Float) {
		if (_rotation.x == x && _rotation.y == y && _rotation.z == z) return
		_rotation.set(x, y, z)
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
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	override var scaleX: Float
		get() = _scale.x
		set(value) {
			val v = value.nonZero()
			if (_scale.x == v) return
			_scale.x = v
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var scaleY: Float
		get() = _scale.y
		set(value) {
			val v = value.nonZero()
			if (_scale.y == v) return
			_scale.y = v
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var scaleZ: Float
		get() = _scale.z
		set(value) {
			val v = value.nonZero()
			if (_scale.z == v) return
			_scale.z = v
			invalidate(ValidationFlags.TRANSFORM)
		}

	override fun setScaling(x: Float, y: Float, z: Float) {
		val x2 = x.nonZero()
		val y2 = y.nonZero()
		val z2 = z.nonZero()
		if (_scale.x == x2 && _scale.y == y2 && _scale.z == z2) return
		_scale.set(x2, y2, z2)
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

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

	/**
	 * The global transform of this component, of all ancestor transforms multiplied together.
	 * Do not modify this matrix directly, it will be overwritten on a TRANSFORM validation.
	 */
	override val modelTransform: Matrix4Ro
		get() {
			validate(ValidationFlags.RENDER_CONTEXT)
			return renderContext.modelTransform
		}

	/**
	 * Returns the inverse concatenated transformation matrix.
	 */
	override val modelTransformInv: Matrix4Ro
		get() {
			validate(ValidationFlags.RENDER_CONTEXT)
			return renderContext.modelTransformInv
		}

	/**
	 * Applies all operations to the transformation matrix.
	 * Do not call this directly, use [validate(ValidationFlags.TRANSFORM)]
	 */
	protected open fun updateTransform() {
		if (_customTransform != null) {
			_transform.set(_customTransform!!)
			return
		}
		_transform.idt()
		_transform.trn(_position)
		if (!_rotation.isZero()) {
			quat.setEulerAngles(_rotation.x, _rotation.y, _rotation.z)
			_transform.rotate(quat)
		}
		_transform.scale(_scale)
		if (!_origin.isZero())
			_transform.translate(-_origin.x, -_origin.y, -_origin.z)
	}

	protected open fun updateRenderContext() {
		_renderContext.parentContext = parent?.renderContext ?: defaultRenderContext
		_renderContext.modelTransformLocal = _transform
		_renderContext.colorTintLocal = _colorTint
		_renderContext.validate()
	}

	//-----------------------------------------------
	// Validatable
	//-----------------------------------------------

	override fun invalidate(flags: Int): Int {
		val flagsInvalidated: Int = validation.invalidate(flags)

		if (flagsInvalidated != 0) {
			if (_invalidated.isDispatching) {
				throw Exception("invalidated already dispatching. ${flagsInvalidated.toFlagsString()}. Possible cyclic validation dependency.")
			}
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
	 */
	override fun validate(flags: Int) {
		if (isDisposed) return
		validation.validate(flags)
	}

	override fun update() = validate()

	//-----------------------------------------------
	// Renderable
	//-----------------------------------------------

	private val _drawRegion = MinMax()

	/**
	 * The local drawing region of this renderable component.
	 * Use `localToCanvas` to convert this region to canvas coordinates.
	 * The draw region is not used for a typical [render], but may be used for render filters or components that
	 * need to set a region for a frame buffer.
	 *
	 * The draw region does not have a validation flag associated with it; invalidating any property will cause
	 * the draw region to be recalculated upon the next request.
	 *
	 * @see Renderable.renderContext
	 * @see com.acornui.component.localToCanvas
	 */
	override val drawRegion: MinMaxRo
		get() {
			updateDrawRegion(_drawRegion)
			return _drawRegion
		}

	/**
	 * Updates this component's draw region.
	 *
	 * By default, this will be the same area as the [bounds].
	 * This may be overriden to modify the area that
	 *
	 * @param out The region that will be set as the [drawRegion].
	 */
	protected open fun updateDrawRegion(out: MinMax) {
		out.set(0f, 0f, _bounds.width, _bounds.height)
	}

	/**
	 * Renders any graphics.
	 * [render] does not check the [visible] flag; that is the responsibility of the caller.
	 *
	 * Canvas coordinates are 0,0 top left, and bottom right is the canvas width/height without dpi scaling.
	 *
	 * You may convert the window coordinate clip region to local coordinates via [canvasToLocal], but in general it is
	 * faster to convert the local coordinates to window coordinates [localToCanvas], as no matrix inversion is
	 * required.
	 */
	override fun render() {
		// Nothing visible.
		val renderContext = renderContext
		if (renderContext.colorTint.a <= 0f)
			return
		draw(renderContext.clipRegion, renderContext.modelTransform, renderContext.colorTint)
	}

	protected fun useCamera(useModel: Boolean = false) {
		val renderContext = renderContext
		if (useModel) glState.setCamera(renderContext.viewProjectionTransform, renderContext.viewTransform, renderContext.modelTransform)
		else glState.setCamera(renderContext.viewProjectionTransform, renderContext.viewTransform)
	}

	/**
	 * The core drawing method for this component.
	 */
	protected open fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
	}

	//-----------------------------------------------
	// Disposable
	//-----------------------------------------------

	override fun dispose() {
		if (_isDisposed)
			throw DisposedException()
		if (isActive) deactivate()
		_disposed.dispatch(this)
		_disposed.dispose()
		_activated.dispose()
		_deactivated.dispose()
		owner.disposed.remove(::dispose.as1)
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
	}

	companion object {
		private val quat: Quaternion = Quaternion()
	}
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
