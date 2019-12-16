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

package com.acornui.component

import com.acornui.*
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.Positionable
import com.acornui.component.layout.intersectsGlobalRay
import com.acornui.component.style.*
import com.acornui.di.*
import com.acornui.focus.*
import com.acornui.function.as1
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.canvasToScreen
import com.acornui.graphic.CameraRo
import com.acornui.graphic.ColorRo
import com.acornui.graphic.Window
import com.acornui.input.InteractionEventRo
import com.acornui.input.InteractionType
import com.acornui.input.InteractivityManager
import com.acornui.input.MouseState
import com.acornui.logging.Log
import com.acornui.math.*
import com.acornui.reflect.observable
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2
import com.acornui.signal.StoppableSignal
import kotlin.properties.Delegates

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
		check(!_isActive) { "Already active" }
		_isActive = true

		invalidate(ValidationFlags.REDRAW_REGIONS)
		_activated.dispatch(this)
	}

	protected open fun onActivated() {}

	final override fun deactivate() {
		if (_isDisposed)
			throw DisposedException()
		check(_isActive) { "Not active" }
		_isActive = false

		if (draws && !parentDraws) {
			renderContext.redraw.invalidate(previousDrawRegionScreen)
			previousDrawRegionScreen.clear()
		}
		_deactivated.dispatch(this)
	}

	protected open fun onDeactivated() {
	}

	// Common dependencies
	protected val window by Window

	@Deprecated("use mouseState", ReplaceWith("mouseState"), DeprecationLevel.ERROR)
	protected val mouse by MouseState
	protected val mouseState by MouseState

	protected val interactivity by InteractivityManager
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
	protected val _position = Vector3(0f, 0f, 0f)
	protected val _rotation = Vector3(0f, 0f, 0f)
	protected val _scale = Vector3(1f, 1f, 1f)
	protected val _origin = Vector3(0f, 0f, 0f)

	// InteractiveElement properties
	protected var _inheritedInteractivityMode = InteractivityMode.ALL
	final override val interactivityModeInherited: InteractivityMode
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
		get() = interactivityModeInherited == InteractivityMode.ALL || interactivityModeInherited == InteractivityMode.ALWAYS

	override fun <T : InteractionEventRo> handlesInteraction(type: InteractionType<T>): Boolean {
		return handlesInteraction(type, true) || handlesInteraction(type, false)
	}

	override fun <T : InteractionEventRo> handlesInteraction(type: InteractionType<T>, isCapture: Boolean): Boolean {
		return getInteractionSignal<InteractionEventRo>(type, isCapture) != null
	}

	private val _captureSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	private val _bubbleSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	private val _attachments = HashMap<Any, Any>()

	// ChildRo properties
	override var parent by observable<ContainerRo?>(null) { value ->
		_renderContext.parentContext = value?.renderContext ?: defaultRenderContext
	}

	// Sizable properties
	protected val _bounds = Bounds()
	protected val _drawRegion = Box()
	protected val _drawRegionCanvas = MinMax()
	protected val _drawRegionScreen = IntRectangle()
	protected val previousDrawRegionScreen = IntRectangle()

	/**
	 * The explicit width, as set by width(value)
	 * Typically one would use `width` in order to retrieve the explicit or actual width.
	 */
	final override var explicitWidth: Float? = null
		private set

	/**
	 * The explicit height, as set by height(value)
	 * Typically one would use `height` in order to retrieve the explicit or actual height.
	 */
	final override var explicitHeight: Float? = null
		private set


	// Focusable properties
	protected val focusManager by FocusManager
	final override var focusEnabled by observable(false) { _ -> invalidateFocusOrder() }
	final override var focusDelegate by observable<UiComponentRo?>(null) { _ -> invalidateFocusOrder() }
	final override var focusOrder by observable(0f) { _ -> invalidateFocusOrder() }
	final override var isFocusContainer by observable(false) { _ -> invalidateFocusOrderDeep() }
	final override var focusEnabledChildren by observable(false) { _ -> invalidateFocusOrderDeep() }

	// Render context properties

	// TODO: Make render context observable and invalidate render context flag on change.
	protected val defaultRenderContext = inject(RenderContextRo)
	protected val _renderContext = RenderContext(defaultRenderContext)
	final override val renderContext: RenderContextRo = _renderContext

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

	/**
	 * @see RenderContext.cameraOverride
	 */
	var cameraOverride: CameraRo?
		get() = _renderContext.cameraOverride
		set(value) {
			_renderContext.cameraOverride = value
			invalidate(ValidationFlags.RENDER_CONTEXT)
		}

	/**
	 * @see RenderContext.draws
	 */
	var draws: Boolean
		get() = _renderContext.drawsSelf
		set(value) {
			_renderContext.drawsSelf = value
			invalidate(ValidationFlags.RENDER_CONTEXT)
		}

	private val parentDraws: Boolean
		get() = _renderContext.parentContext.draws

	private val rayTmp = Ray()

	init {
		owner.disposed.add(::dispose.as1)
		validation = validationGraph {
			ValidationFlags.apply {
				addNode(HIERARCHY_DESCENDING, ::updateHierarchyDescending)
				addNode(STYLES, HIERARCHY_DESCENDING, ::updateStyles)
				addNode(LAYOUT_ENABLED, ::updateLayoutEnabled)
				addNode(LAYOUT, STYLES, ::validateLayout)
				addNode(HIERARCHY_ASCENDING, LAYOUT, ::updateHierarchyAscending)
				addNode(TRANSFORM, ::updateTransform)
				addNode(INTERACTIVITY_MODE, ::updateInheritedInteractivityMode)
				addNode(RENDER_CONTEXT, TRANSFORM or STYLES, ::updateRenderContext)
				addNode(REDRAW_REGIONS, LAYOUT or RENDER_CONTEXT or VERTICES, 0, checkAllFound = false, onValidate = ::updateRedrawRegions)
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

	final override var visible: Boolean by validationProp(true, ValidationFlags.LAYOUT_ENABLED or ValidationFlags.RENDER_CONTEXT)

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

	override var focusHighlightDelegate: UiComponentRo? by observable(null, ::refreshFocusHighlight.as1)

	override var showFocusHighlight by observable(false, ::refreshFocusHighlight.as1)

	private var focusTarget: UiComponentRo? = null
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
		val bounds = _bounds // Accessing _bounds instead of bounds to avoid a validation.
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

	override val shouldLayout: Boolean
		get() = includeInLayout && visible

	override var layoutInvalidatingFlags: Int = UiComponentRo.defaultLayoutInvalidatingFlags

	final override var includeInLayout: Boolean by validationProp(true, ValidationFlags.LAYOUT_ENABLED)
	final override var includeInRender: Boolean = true

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

	override var minWidth: Float by validationProp(0f, ValidationFlags.LAYOUT)
	override var minHeight: Float by validationProp(0f, ValidationFlags.LAYOUT)
	override var maxWidth: Float by validationProp(Float.MAX_VALUE, ValidationFlags.LAYOUT)
	override var maxHeight: Float by validationProp(Float.MAX_VALUE, ValidationFlags.LAYOUT)

	final override var defaultWidth: Float? by validationProp(null, ValidationFlags.LAYOUT)

	final override var defaultHeight: Float? by validationProp(null, ValidationFlags.LAYOUT)

	final override var baselineOverride: Float? by validationProp(null, ValidationFlags.LAYOUT)

	/**
	 * Does the same thing as setting width and height individually.
	 */
	final override fun setSize(width: Float?, height: Float?) {
		if (width?.isNaN() == true || height?.isNaN() == true) throw Exception("May not set the size to be NaN")
		val oldW = explicitWidth
		val oldH = explicitHeight
		if (oldW == width && oldH == height) return
		explicitWidth = width
		explicitHeight = height
		onSizeSet(oldW, oldH, width, height)
		invalidate(ValidationFlags.LAYOUT)
	}

	protected open fun onSizeSet(oldWidth: Float?, oldHeight: Float?, newWidth: Float?, newHeight: Float?) {}

	/**
	 * Do not call this directly, use [validate(ValidationFlags.LAYOUT)]
	 */
	private fun validateLayout() {
		val w = MathUtils.clamp(explicitWidth ?: defaultWidth, minWidth, maxWidth)
		val h = MathUtils.clamp(explicitHeight ?: defaultHeight, minHeight, maxHeight)
		_bounds.set(w ?: 0f, h ?: 0f)
		updateLayout(w, h, _bounds)
		if (baselineOverride != null)
			_bounds.baseline = baselineOverride!!
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
		canvasToLocal(out.set(mouseState.canvasX, mouseState.canvasY))
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
	 *
	 * To get the multiplied (global) color tint, see [renderContext]
	 * The final pixel color value for the default shader is `renderContext.colorTint * pixel`
	 */
	override var colorTint: ColorRo
		get() = _renderContext.colorTintLocal
		set(value) {
			if (_renderContext.colorTintLocal == value) return
			colorTint(value.r, value.g, value.b, value.a)
		}

	override fun colorTint(r: Float, g: Float, b: Float, a: Float) {
		_renderContext.colorTintLocal.set(r, g, b, a)
		invalidate(ValidationFlags.RENDER_CONTEXT)
	}

	/**
	 * The color multiplier of this component and all ancestor color tints multiplied together.
	 */
	final override val concatenatedColorTint: ColorRo
		get() = renderContext.colorTint

	protected open fun updateInheritedInteractivityMode() {
		_inheritedInteractivityMode = _interactivityMode
		if (parent?.interactivityModeInherited == InteractivityMode.NONE)
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

	override val isValidating: Boolean
		get() = validation.isValidating

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
			return _renderContext.modelTransformLocal
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
	final override val modelTransform: Matrix4Ro
		get() = renderContext.modelTransform

	/**
	 * Returns the inverse concatenated transformation matrix.
	 */
	final override val modelTransformInv: Matrix4Ro
		get() = renderContext.modelTransformInv

	/**
	 * Applies all operations to the transformation matrix.
	 * Do not call this directly, use [validate(ValidationFlags.TRANSFORM)]
	 */
	protected open fun updateTransform() {
		if (_renderContext.modelTransformOverride != null) {
			_renderContext.modelTransformLocal.idt()
			return
		}
		val mat = _renderContext.modelTransformLocal
		if (_customTransform != null) {
			mat.set(_customTransform!!)
			return
		}
		mat.idt()
		mat.trn(_position)
		if (!_rotation.isZero()) {
			quat.setEulerAngles(_rotation.x, _rotation.y, _rotation.z)
			mat.rotate(quat)
		}
		mat.scale(_scale)
		if (!_origin.isZero())
			mat.translate(-_origin.x, -_origin.y, -_origin.z)
	}

	protected open fun updateRenderContext() {
		_renderContext.invalidate() // Mark the context's cache as invalid
	}

	protected open fun updateRedrawRegions() {
		updateDrawRegionCanvas(_drawRegionCanvas.inf())
		val drawRegionScreen = glState.framebuffer.canvasToScreen(drawRegionCanvas, _drawRegionScreen)
		val redraw = renderContext.redraw
		if (redraw.enabled) {
			// Invalidate the previously drawn region
			renderContext.redraw.invalidate(previousDrawRegionScreen)
			previousDrawRegionScreen.clear()
			// Invalidate the new draw region
			if (draws && !parentDraws && visible && alpha > 0f) {
				renderContext.redraw.invalidate(drawRegionScreen)
			}
		}
	}

	/**
	 * Updates this component's local draw region.
	 *
	 * By default, this will be the same area as the [bounds] with `0f` depth.
	 * This may be overridden to modify the area that is drawn to the screen.
	 *
	 * @param out The bounding box to set to the current draw region.
	 */
	protected open fun updateDrawRegionLocal(out: Box) {
		out.set(0f, 0f, 0f, _bounds.width, _bounds.height, 0f)
	}

	protected open fun updateDrawRegionCanvas(out: MinMax) {
		val drawRegion = _drawRegion.inf()
		updateDrawRegionLocal(drawRegion)
		if (drawRegion.isNotEmpty()) {
			out.set(localToCanvas(drawRegion).clamp(renderContext.clipRegion))
		}
	}

	//-----------------------------------------------
	// Validatable
	//-----------------------------------------------

	final override fun invalidate(flags: Int): Int {
		val flagsInvalidated: Int = validation.invalidate(flags)

		if (flagsInvalidated != 0) {
			check(!_invalidated.isDispatching) {
				"invalidated already dispatching. Flags invalidated <${ValidationFlags.flagsToString(flags)}>. Possible cyclic validation dependency."
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
	 * Example: validate(ValidationFlags.LAYOUT or ValidationFlags.PROPERTIES) to validate both layout and properties.
	 */
	override fun validate(flags: Int) {
		validation.validate(flags)
	}

	override fun update() = validate()

	final override val drawRegionCanvas: RectangleRo
		get() {
			validate(ValidationFlags.REDRAW_REGIONS)
			return _drawRegionCanvas
		}

	final override val drawRegionScreen: IntRectangleRo
		get() {
			validate(ValidationFlags.REDRAW_REGIONS)
			return _drawRegionScreen
		}

	/**
	 * True if this component is visible, has opacity, and passes a redraw check with the render context [RedrawRegions].
	 */
	protected open val needsRedraw: Boolean
		get() {
			val renderContext = _renderContext
			return (renderContext.parentContext.draws ||
					renderContext.redraw.needsRedraw(drawRegionScreen)) &&
					visible &&
					colorTint.a > 0f
		}

	override fun render() {
		if (validation.invalidFlags != 0)
			Log.error("render $this with invalid flags ${ValidationFlags.flagsToString(validation.invalidFlags)}")
		if (needsRedraw) {
			if (draws && !parentDraws)
				previousDrawRegionScreen.set(drawRegionScreen)
			draw()
		}
	}

	/**
	 * Renders this component.
	 * This will only be called if [alpha] is greater than zero.
	 */
	open fun draw() {
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
		private val quat = Quaternion()
	}
}