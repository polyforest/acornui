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

@file:Suppress("PropertyName")

package com.acornui.component

import com.acornui.Disposable
import com.acornui.assertionsEnabled
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.Positionable
import com.acornui.component.style.*
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.own
import com.acornui.focus.*
import com.acornui.function.as1
import com.acornui.gl.core.CachedGl20
import com.acornui.graphic.CameraRo
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.Window
import com.acornui.input.InteractionEventRo
import com.acornui.input.InteractionType
import com.acornui.input.InteractivityManager
import com.acornui.input.MouseState
import com.acornui.logging.Log
import com.acornui.math.*
import com.acornui.nonZero
import com.acornui.properties.afterChange
import com.acornui.signal.Signal
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
		owner: Context
) : UiComponent, ContextImpl(owner) {

	//---------------------------------------------------------
	// Lifecycle
	//---------------------------------------------------------

	@Suppress("UNCHECKED_CAST")
	override val disposed = super.disposed as Signal<(UiComponentImpl) -> Unit>

	private val _activated = own(Signal1<UiComponent>())
	final override val activated = _activated.asRo()

	private val _deactivated = own(Signal1<UiComponent>())
	final override val deactivated = _deactivated.asRo()

	private var _isActive: Boolean = false

	override val isActive: Boolean
		get() = _isActive

	final override fun activate() {
		checkDisposed()
		check(!_isActive) { "Already active" }
		_isActive = true
		_activated.dispatch(this)
	}

	protected open fun onActivated() {}

	final override fun deactivate() {
		checkDisposed()
		check(_isActive) { "Not active" }
		_isActive = false
		_deactivated.dispatch(this)
	}

	protected open fun onDeactivated() {
	}

	// Common dependencies

	protected val window by Window

	protected val mouseState by MouseState
	protected val interactivity by InteractivityManager
	protected val gl by CachedGl20
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
	private var _inheritedInteractivityMode = InteractivityMode.ALL
	final override val interactivityModeInherited: InteractivityMode
		get() {
			validate(ValidationFlags.INTERACTIVITY_MODE)
			return _inheritedInteractivityMode
		}

	private var _interactivityMode: InteractivityMode = InteractivityMode.ALL
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

	private val captureSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	private val bubbleSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	private val attachments = HashMap<Any, Any>()

	// ChildRo properties

	override var parent: ContainerRo? = null

	// Sizable properties

	protected val _bounds = Bounds()

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
	final override var focusEnabled by afterChange(false) { _ -> invalidateFocusOrder() }
	final override var focusDelegate by afterChange<UiComponentRo?>(null) { _ -> invalidateFocusOrder() }
	final override var focusOrder by afterChange(0f) { _ -> invalidateFocusOrder() }
	final override var isFocusContainer by afterChange(false) { _ -> invalidateFocusOrderDeep() }
	final override var focusEnabledChildren by afterChange(false) { _ -> invalidateFocusOrderDeep() }

	// View projection properties

	/**
	 * If set, [viewTransform], [projectionTransform], [viewProjectionTransform], and [viewProjectionTransformInv]
	 * matrices will be calculated based on this camera.
	 */
	var cameraOverride: CameraRo? by validationProp(null, ValidationFlags.VIEW_PROJECTION)

	private val _viewProjectionTransformInv = Matrix4()
	override val viewProjectionTransformInv: Matrix4Ro by validationProp(ValidationFlags.VIEW_PROJECTION) {
		_viewProjectionTransformInv.set(viewProjectionTransform).inv()
	}

	private val _viewProjectionTransform = Matrix4()
	override val viewProjectionTransform: Matrix4Ro by validationProp(ValidationFlags.VIEW_PROJECTION) {
		_viewProjectionTransform.set(projectionTransform).mul(viewTransform)
	}

	override val projectionTransform: Matrix4Ro by validationProp(ValidationFlags.VIEW_PROJECTION) {
		cameraOverride?.projectionTransform ?: parent?.projectionTransform ?: Matrix4.IDENTITY
	}

	override val viewTransform: Matrix4Ro by validationProp(ValidationFlags.VIEW_PROJECTION) {
		cameraOverride?.viewTransform ?: parent?.viewTransform ?: Matrix4.IDENTITY
	}

	/**
	 * The clip region, in local coordinates.
	 * If set, this will transform to canvas coordinates and [canvasClipRegion] will be the intersection of the
	 * parent clip region and this region.
	 */
	@Suppress("RemoveExplicitTypeArguments")
	protected var clipRegionLocal: MinMaxRo? by validationProp<MinMaxRo?>(null, ValidationFlags.DRAW_REGION) { it?.copy() }

	/**
	 * If set, [canvasClipRegion] will be set to this, and not the intersection of [clipRegionLocal] and the
	 * parent clipping.
	 */
	@Suppress("RemoveExplicitTypeArguments") // Type inference failed error
	var canvasClipRegionOverride: MinMaxRo? by validationProp<MinMaxRo?>(null, ValidationFlags.DRAW_REGION) { it?.copy() }

	private val _clipRegionIntersection = MinMax()
	override val canvasClipRegion: MinMaxRo by validationProp(ValidationFlags.DRAW_REGION) {
		canvasClipRegionOverride ?: run {
			val parentClipRegion = parent?.canvasClipRegion ?: MinMaxRo.NEGATIVE_INFINITY
			if (clipRegionLocal == null) parentClipRegion
			else localToCanvas(_clipRegionIntersection.set(clipRegionLocal!!)).intersection(parentClipRegion)
		}
	}

	/**
	 * If set, [viewport] will use this explicit value.
	 */
	@Suppress("RemoveExplicitTypeArguments")
	var viewportOverride: RectangleRo? by validationProp<RectangleRo?>(null, ValidationFlags.VIEW_PROJECTION) { it?.copy() }

	override val viewport: RectangleRo by validationProp(ValidationFlags.VIEW_PROJECTION) {
		viewportOverride ?: parent?.viewport ?: RectangleRo.EMPTY
	}

	init {
		validation = validationGraph {
			ValidationFlags.apply {
				addNode(HIERARCHY_DESCENDING, ::updateHierarchyDescending)
				addNode(STYLES, HIERARCHY_DESCENDING, ::updateStyles)
				addNode(LAYOUT_ENABLED, ::updateLayoutEnabled)
				addNode(LAYOUT, STYLES, ::validateLayout)
				addNode(HIERARCHY_ASCENDING, LAYOUT, ::updateHierarchyAscending)
				addNode(TRANSFORM, ::updateTransform)
				addNode(COLOR_TINT, ::updateColorTint)
				addNode(INTERACTIVITY_MODE, ::updateInheritedProperties)
				addNode(VERTICES_GLOBAL, LAYOUT or TRANSFORM or COLOR_TINT, ::updateVerticesGlobal)
				addNode(VIEW_PROJECTION, ::updateViewProjection)
				addNode(DRAW_REGION, LAYOUT or TRANSFORM or VIEW_PROJECTION, ::updateDrawRegion)
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

	override var focusHighlightDelegate: UiComponentRo? by afterChange(null, ::refreshFocusHighlight.as1)

	override var showFocusHighlight by afterChange(false, ::refreshFocusHighlight.as1)

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

	private val topLeft = Vector3()
	private val topRight = Vector3()
	private val bottomRight = Vector3()
	private val bottomLeft = Vector3()

	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
		val bounds = bounds
		localToGlobal(topLeft.set(bounds.left, bounds.top, 0f))
		localToGlobal(topRight.set(bounds.right, bounds.top, 0f))
		localToGlobal(bottomRight.set(bounds.right, bounds.bottom, 0f))
		localToGlobal(bottomLeft.set(bounds.left, bounds.bottom, 0f))
		return globalRay.intersectsTriangle(topLeft, topRight, bottomRight, intersection) ||
				globalRay.intersectsTriangle(topLeft, bottomLeft, bottomRight, intersection)
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
	 * Returns true if visible and the includeInLayout flag is true. If this is false, this layout element will not
	 * be included in layout algorithms.
	 */
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
		return captureSignals.isNotEmpty() || bubbleSignals.isNotEmpty()
	}

	final override fun <T : InteractionEventRo> hasInteraction(type: InteractionType<T>, isCapture: Boolean): Boolean {
		return getInteractionSignal<InteractionEventRo>(type, isCapture) != null
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : InteractionEventRo> getInteractionSignal(type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T>? {
		val handlers = if (isCapture) captureSignals else bubbleSignals
		return handlers[type] as StoppableSignal<T>?
	}

	final override fun <T : InteractionEventRo> addInteractionSignal(type: InteractionType<T>, signal: StoppableSignal<T>, isCapture: Boolean) {
		val handlers = if (isCapture) captureSignals else bubbleSignals
		handlers[type] = signal
	}


	final override fun <T : InteractionEventRo> removeInteractionSignal(type: InteractionType<T>, isCapture: Boolean) {
		val handlers = if (isCapture) captureSignals else bubbleSignals
		handlers.remove(type)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> getAttachment(key: Any): T? {
		return attachments[key] as T?
	}

	final override fun setAttachment(key: Any, value: Any) {
		attachments[key] = value
	}

	/**
	 * Removes an attachment added via [setAttachment]
	 */
	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> removeAttachment(key: Any): T? {
		return attachments.remove(key) as T?
	}

	//-----------------------------------------------
	// ColorTransformable
	//-----------------------------------------------

	/**
	 * The calculated [colorTint] value will be this value multiplied by the parent context's color tint, unless
	 * [colorTintGlobalOverride] is set.
	 */
	private val _colorTint: Color = Color.WHITE.copy()

	/**
	 * If set, the [colorTintGlobal] value won't be calculated as the multiplication of [colorTint] and the parent
	 * context's global color tint; it will be this explicit value.
	 */
	@Suppress("RemoveExplicitTypeArguments")
	var colorTintGlobalOverride: ColorRo? by validationProp<ColorRo?>(null, ValidationFlags.COLOR_TINT) { it?.copy() }

	/**
	 * The local color tint of this component.
	 *
	 * To get the multiplied (global) color tint, see [colorTintGlobal]
	 * The final pixel color value for the default shader is `colorTintGlobal * pixel`
	 */
	override var colorTint: ColorRo
		get() = _colorTint
		set(value) {
			if (_colorTint == value) return
			_colorTint.set(value)
			invalidate(ValidationFlags.COLOR_TINT)
		}

	override fun colorTint(r: Float, g: Float, b: Float, a: Float) {
		_colorTint.set(r, g, b, a)
		invalidate(ValidationFlags.COLOR_TINT)
	}

	private val _colorTintGlobal = Color()

	/**
	 * The color multiplier of this component and all ancestor color tints multiplied together.
	 */
	override val colorTintGlobal: ColorRo
		get() {
			validate(ValidationFlags.COLOR_TINT)
			return _colorTintGlobal
		}

	protected open fun updateInheritedProperties() {
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

	private val rayTmp = Ray()

	override fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean, returnAll: Boolean, out: MutableList<UiComponentRo>, rayCache: RayRo?): MutableList<UiComponentRo> {
		if (!visible || (onlyInteractive && !interactivityEnabled)) return out

		val ray = rayCache ?: getPickRay(canvasX, canvasY, rayTmp)
		if (interactivityMode == InteractivityMode.ALWAYS || intersectsGlobalRay(ray)) {
			out.add(this)
		}
		return out
	}

	//-----------------------------------------------
	// Stylable
	//-----------------------------------------------

	override val styleParent: StylableRo? by lazy {
		var p: Context? = owner
		var s: Stylable? = null
		while (p != null) {
			if (p is Stylable) {
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

	final override val invalidFlags: Int
		get() = validation.invalidFlags

	final override val isValidating: Boolean
		get() = validation.isValidating

	final override fun getValidatedCount(flag: Int): Int = validation.getValidatedCount(flag)

	//-----------------------------------------------
	// Transformable
	//-----------------------------------------------

	override var snapToPixel: Boolean = Positionable.defaultSnapToPixel

	private val _transformLocal = Matrix4()

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
			return _transformLocal
		}

	/**
	 * If true, this component's model view and transform matrices should be pushed to the gpu.
	 * If false, the local transform is assumed to be translation only and [vertexTranslation] will be set.
	 *
	 * Override this to be true if [viewProjectionTransform], [viewTransform], or [transformGlobal] is overridden.
	 */
	protected open val useTransforms: Boolean
		get() = cameraOverride != null ||
				_transformLocal.mode != MatrixMode.IDENTITY &&
				_transformLocal.mode != MatrixMode.TRANSLATION

	private val _vertexTranslation = Vector3()

	/**
	 * Vertices rendered should be local position with this added translation.
	 */
	override val vertexTranslation: Vector3Ro
		get() {
			validate(ValidationFlags.TRANSFORM)
			return _vertexTranslation
		}

	@Suppress("RemoveExplicitTypeArguments")
	override var transformOverride: Matrix4Ro? by validationProp<Matrix4Ro?>(null, ValidationFlags.TRANSFORM) { it?.copy() }

	@Suppress("RemoveExplicitTypeArguments")
	var transformGlobalOverride: Matrix4Ro? by validationProp<Matrix4Ro?>(null, ValidationFlags.TRANSFORM) { it?.copy() }

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

	override var position: Vector3Ro
		get() = _position
		set(value) {
			setPosition(value.x, value.y, value.z)
		}

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

	protected val _transformGlobal = Matrix4()

	/**
	 * The global transform of this component, of all ancestor transforms multiplied together.
	 *
	 * NB: If this is overridden, [useTransforms] must be overridden as well.
	 */
	override val transformGlobal: Matrix4Ro by validationProp(ValidationFlags.TRANSFORM) {
		if (transformGlobalOverride != null) _transformGlobal.set(transformGlobalOverride!!)
		else _transformGlobal.set(parent?.transformGlobal ?: Matrix4.IDENTITY).mul(_transformLocal)
	}

	private val _transformGlobalInv = Matrix4()

	/**
	 * Returns the inverse concatenated transformation matrix.
	 */
	final override val transformGlobalInv: Matrix4Ro by validationProp(ValidationFlags.TRANSFORM) {
		_transformGlobalInv.set(transformGlobal).inv()
	}

	/**
	 * *This is reserved for future use*
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

	/**
	 * Applies all operations to the transformation matrix.
	 * Do not call this directly, use [validate(ValidationFlags.TRANSFORM)]
	 */
	protected open fun updateTransform() {
		val mat = _transformLocal
		if (transformOverride != null) {
			mat.set(transformOverride!!)
		} else {
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
		if (useTransforms)
			_vertexTranslation.clear()
		else
			_vertexTranslation.set(parent?.vertexTranslation ?: Vector3.ZERO).add(mat.translationX, mat.translationY, mat.translationZ)
	}

	protected open fun updateColorTint() {
		if (colorTintGlobalOverride != null) _colorTintGlobal.set(colorTintGlobalOverride!!)
		else _colorTintGlobal.set(parent?.colorTintGlobal ?: Color.WHITE).mul(_colorTint).clamp()
	}

	protected open fun updateVerticesGlobal() {}

	protected open fun updateViewProjection() {}

	protected open fun updateDrawRegion() {}

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

	/**
	 * Invoked when this component has been invalidated.
	 * @param flagsInvalidated The bit flags invalidated. This can be checked like:
	 * `flagsInvalidated containsFlag ValidationFlags.LAYOUT`
	 * @see ValidationFlags
	 */
	protected open fun onInvalidated(flagsInvalidated: Int) {
	}

	final override fun validate(flags: Int): Int {
		return validation.validate(flags)
	}

	override fun update() {
		validate()
	}

	override fun render() {
		if (validation.invalidFlags != 0)
			Log.error("render $this with invalid flags ${ValidationFlags.flagsToString(validation.invalidFlags)}")
		if (visible && colorTint.a > 0f) {
			if (useTransforms) {
				gl.uniforms.useCamera(this, true) {
					draw()
				}
			} else {
				draw()
			}
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
		checkDisposed()
		if (isActive) deactivate()
		super.dispose()
		layoutData = null

		// InteractiveElement
		// Dispose all disposable handlers.
		for (i in captureSignals.values) {
			(i as? Disposable)?.dispose()
		}
		captureSignals.clear()
		for (i in bubbleSignals.values) {
			(i as? Disposable)?.dispose()
		}
		bubbleSignals.clear()
		attachments.clear()
	}

	companion object {
		private val quat = Quaternion()
	}
}