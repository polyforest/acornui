/*
 * Copyright 2018 PolyForest
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

package com.acornui.component.text

import com.acornui.component.ValidationFlags
import com.acornui.component.ValidationTree
import com.acornui.component.layout.LayoutData
import com.acornui.component.style.*
import com.acornui.component.validationProp
import com.acornui.core.DisposedException
import com.acornui.core.di.Injector
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.core.graphic.CameraRo
import com.acornui.core.graphic.Window
import com.acornui.core.graphic.project
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.math.MathUtils.offsetRound
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.signal.Signal2

abstract class TextNodeBase(final override val owner: Owned) : TextNode {

	//-----------------------------------------------------
	// Owned methods
	//-----------------------------------------------------

	override val injector: Injector
		get() = owner.injector

	private var _isDisposed = false
	override val isDisposed: Boolean
		get() = _isDisposed

	private val _disposed = Signal1<TextNode>()
	override val disposed = _disposed.asRo()

	private val ownerDisposedHandler = {
		owner: Owned ->
		dispose()
	}

	//-----------------------------------------------------

	protected val gl = inject(Gl20)
	protected val glState = inject(GlState)
	protected val window = inject(Window)

	protected val validation = ValidationTree()

	protected val _bounds = Bounds()
	protected var _explicitWidth: Float? = null
	protected var _explicitHeight: Float? = null

	override var textNodeParent: TextNodeRo? by validationProp(null, ValidationFlags.STYLES or ValidationFlags.HIERARCHY_DESCENDING)
	override var textField: TextField? by validationProp(null, ValidationFlags.STYLES)

	override var allowClipping: Boolean by validationProp(true, ValidationFlags.LAYOUT)

	override fun update() = validate()

	private val _invalidated = own(Signal2<TextNode, Int>())
	override val invalidated = _invalidated.asRo()
	override val invalidFlags: Int
		get() = validation.invalidFlags

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

	override fun validate(flags: Int) {
		if (isDisposed) return
		validation.validate(flags)
	}

	//-----------------------------------------------------
	// Styleable methods
	//-----------------------------------------------------

	protected val styles = Styles(this)

	override val styleTags: MutableList<StyleTag>
		get() = styles.styleTags
	override val styleRules: MutableList<StyleRule<*>>
		get() = styles.styleRules

	override fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) =
			styles.getRulesByType(type, out)

	override val styleParent: StyleableRo?
		get() = textNodeParent ?: textField

	protected fun <T : Style> bind(style: T, calculator: StyleCalculator = CascadingStyleCalculator): T {
		styles.bind(style, calculator)
		return style
	}

	override fun invalidateStyles() {
		invalidate(ValidationFlags.STYLES)
	}

	override val right: Float
		get() = x + width
	override val bottom: Float
		get() = y + height

	override var layoutData: LayoutData? = null

	override val width: Float
		get() = bounds.width

	override val height: Float
		get() = bounds.height

	override val bounds: BoundsRo
		get() {
			validate(ValidationFlags.LAYOUT)
			return _bounds
		}

	override val explicitWidth: Float?
		get() = _explicitWidth
	override val explicitHeight: Float?
		get() = _explicitHeight


	//-----------------------------------------------------
	// Positionable methods
	//-----------------------------------------------------

	private val _position = Vector3()

	override var x: Float
		get() = _position.x
		set(value) {
			if (value == _position.x) return
			_position.x = value
			invalidate(ValidationFlags.CONCATENATED_TRANSFORM)
		}

	override var y: Float
		get() = _position.y
		set(value) {
			if (value == _position.y) return
			_position.y = value
			invalidate(ValidationFlags.CONCATENATED_TRANSFORM)
		}

	override var z: Float
		get() = _position.z
		set(value) {
			if (value == _position.z) return
			_position.z = value
			invalidate(ValidationFlags.CONCATENATED_TRANSFORM)
		}

	override val position: Vector3Ro
		get() = _position

	/**
	 * Does the same thing as setting width and height individually.
	 */
	override fun setPosition(x: Float, y: Float, z: Float) {
		if (x == _position.x && y == _position.y && z == _position.z) return
		_position.set(x, y, z)
		invalidate(ValidationFlags.CONCATENATED_TRANSFORM)
		return
	}

	//-----------------------------------------------------
	// Sizable methods
	//-----------------------------------------------------

	override fun setSize(width: Float?, height: Float?) {
		if (_explicitWidth == width && _explicitHeight == height) return
		_explicitWidth = width
		_explicitHeight = height
		invalidate(ValidationFlags.LAYOUT)
	}

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

	final override fun moveTo(x: Float, y: Float, z: Float) {
		setPosition(offsetRound(x), offsetRound(y), z)
	}

	protected open fun updateStyles() {
		styles.validateStyles()
	}

	/**
	 * Do not call this directly, use [validate(ValidationFlags.LAYOUT)]
	 */
	protected fun validateLayout() {
		val w = _explicitWidth
		val h = _explicitHeight
		_bounds.set(w ?: 0f, h ?: 0f)
		updateLayout(w, h, _bounds)
	}

	/**
	 * Updates this component's layout.
	 * This method should update the [out] [Rectangle] bounding measurements.
	 *
	 * @param explicitWidth The explicitWidth dimension. Null if the preferred width should be used.
	 * @param explicitHeight The explicitHeight dimension. Null if the preferred height should be used.
	 */
	abstract fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds)

	protected val _concatenatedTransform = Matrix4()
	override val concatenatedTransform: Matrix4Ro
		get() = _concatenatedTransform

	protected open fun updateConcatenatedTransform() {
		val parentTransform: Matrix4Ro = textNodeParent?.concatenatedTransform ?: textField?.concatenatedTransform ?: Matrix4.IDENTITY
		_concatenatedTransform.set(parentTransform).translate(x, y, z)
	}

	protected val concatenatedColorTint: ColorRo
		get() = textField?.concatenatedColorTint ?: Color.WHITE

	protected val camera: CameraRo
		get() = textField!!.camera

	protected fun localToCanvas(localCoord: Vector3): Vector3 {
		val tF = textField ?: return localCoord
		localToGlobal(localCoord)
		tF.camera.project(localCoord, tF.viewport)
		return localCoord
	}

	init {
		val r = this
		validation.apply {
			addNode(ValidationFlags.HIERARCHY_ASCENDING) {}
			addNode(ValidationFlags.HIERARCHY_DESCENDING) {}
			addNode(ValidationFlags.STYLES, r::updateStyles)
			addNode(ValidationFlags.LAYOUT, ValidationFlags.STYLES, r::validateLayout)
			addNode(ValidationFlags.CONCATENATED_TRANSFORM, r::updateConcatenatedTransform)
		}
		owner.disposed.add(ownerDisposedHandler)
	}

	override fun dispose() {
		if (_isDisposed) throw DisposedException()
		_isDisposed = true
		owner.disposed.remove(ownerDisposedHandler)
		_disposed.dispatch(this)
		_disposed.dispose()
	}
}