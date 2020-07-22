///*
// * Copyright 2020 Poly Forest, LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.acornui.component.scroll
//
//import com.acornui.Disposable
//import com.acornui.number.closeTo
//import com.acornui.component.*
//import com.acornui.component.layout.HAlign
//import com.acornui.component.layout.LayoutElementRo
//import com.acornui.component.layout.VAlign
//import com.acornui.component.layout.algorithm.BasicLayoutData
//import com.acornui.component.layout.algorithm.LayoutDataProvider
//import com.acornui.component.style.*
//import com.acornui.cursor.StandardCursor
//import com.acornui.cursor.cursor
//import com.acornui.di.Context
//
//import com.acornui.dom.isFabricated
//import com.acornui.dom.isHandled
//import com.acornui.function.as1
//import com.acornui.input.*
//import com.acornui.math.Bounds
//import com.acornui.math.Easing
//import com.acornui.math.clamp
//import com.acornui.math.Vector2Ro
//import com.acornui.math.vec2
//import com.acornui.properties.afterChange
//import com.acornui.signal.Signal1
//import com.acornui.time.callLater
//import org.w3c.dom.events.MouseEvent
//import kotlin.contracts.InvocationKind
//import kotlin.contracts.contract
//import kotlin.time.Duration
//import kotlin.time.seconds
//
//abstract class ScrollbarBase(owner: Context) : ContainerImpl(owner), Scrollbar {
//
//	override fun createLayoutData() = ScrollbarLayoutData()
//
//	private val _scrollModel = own(ScrollModelImpl())
//	val scrollModel: ClampedScrollModel = _scrollModel
//
//	private val _changed = own(Signal1<ScrollbarBase>())
//	final override val changed = _changed.asRo()
//
//	val style = bind(ScrollbarStyle())
//
//	/**
//	 * The value to add or subtract to the scroll model on step up or step down button press.
//	 */
//	var stepSize = 5.0
//
//	/**
//	 * The value to multiply against the scroll model to convert to points.
//	 * In other words, how many points per 1 unit on the scroll model.
//	 */
//	var modelToPoints by afterChange(1.0) {
//		new ->
//		check(!(new.isNaN() || new.isInfinite())) { "modelToPoints may not be NaN" }
//		invalidate(ValidationFlags.LAYOUT)
//	}
//
//	private val thumbOffset = vec2()
//	private val positionTmp = vec2()
//
//	// Children
//	protected var decrementButton: UiComponent? = null
//	protected var incrementButton: UiComponent? = null
//	protected var thumb: UiComponent? = null
//	protected var track: UiComponent? = null
//
//	private var previousScrollValue = -1.0
//	private var alphaTick: Disposable? = null
//	private var fadeTimer: Duration = Duration.ZERO
//
//	private val mouseOrTouchState = own(MouseOrTouchState(this))
//
//	init {
//		addClass(Scrollbar)
//
//		scrollModel.changed.add {
//			if (it.max > it.min) cursor(StandardCursor.POINTER) else cursor(StandardCursor.DEFAULT)
//			updateThumbLayout(width, height, thumb!!)
//		}
//
//		callLater {
//			// Add this handler in a call later
//			scrollModel.changed.add {
//				if (!previousScrollValue.closeTo(scrollModel.value)) {
//					// Only make the scroll bar opaque if the scroll model's value has changed by a non-negligible amount.
//					previousScrollValue = it.value
//					activeAnim()
//				}
//			}
//		}
//
//		watch(style) {
//			track?.dispose()
//			track = addChild(it.track(this))
//			track!!.interactivityMode = InteractivityMode.NONE
//
//			decrementButton?.dispose()
//			decrementButton = addOptionalChild(it.decrementButton(this))
//			decrementButton?.enableDownRepeat()
//			decrementButton?.mouseDown()?.add(::decrementPressHandler)
//
//			incrementButton?.dispose()
//			incrementButton = addOptionalChild(it.incrementButton(this))
//			incrementButton?.enableDownRepeat()
//			incrementButton?.mouseDown()?.add(::incrementPressHandler)
//
//			thumb?.dispose()
//			thumb = addChild(it.thumb(this))
//			val thumb = thumb!!
//			thumb.layoutInvalidatingFlags = ValidationFlags.LAYOUT
//			thumb.focusEnabled = false
//			thumb.dragStart().add(::dragStartHandler.as1)
//			thumb.drag().add(::thumbDragHandler)
//
//			onAlphaTick(0.0) // Set the thumb's alpha
//		}
//		mouseMove().add(::activeAnim.as1)
//		touchMove().add(::activeAnim.as1)
//		mouseDown().add(::activeAnim.as1)
//		touchStart().add(::activeAnim.as1)
//
//		enableDownRepeat()
//
//		mouseDown().add(::pageModePressHandler)
//		drag().add(::dragHandler)
//	}
//
//	private fun onAlphaTick(dT: Double) {
//		fadeTimer = if (mouseOrTouchState.isOver)
//			style.fadeOutDelay + style.fadeOutDuration
//		else
//			fadeTimer - dT.toDouble().seconds
//
//		val thumb = thumb ?: return
//		val p = clamp(1.0 - (fadeTimer / style.fadeOutDuration).toDouble(), 0.0, 1.0)
//		thumb.alpha = style.fadeOutEasing.apply(1.0, style.inactiveAlpha, p)
//
//		if (fadeTimer <= Duration.ZERO) {
//			alphaTick?.dispose()
//			alphaTick = null
//		}
//	}
//
//	private fun activeAnim() {
//		fadeTimer = style.fadeOutDelay + style.fadeOutDuration
//		if (alphaTick == null && style.inactiveAlpha != 1.0)
//			alphaTick = onTick(::onAlphaTick)
//	}
//
//	private fun decrementPressHandler(event: MouseEvent) {
//		event.handle()
//		stepDec()
//	}
//
//	private fun incrementPressHandler(event: MouseEvent) {
//		event.handle()
//		stepInc()
//	}
//
//	private var pageDirection = false
//
//	/**
//	 * When pressing down on the track
//	 */
//	private fun pageModePressHandler(event: MouseEvent) {
//		if (!style.pageMode || event.isHandled) return
//		val track = track ?: return
//		val thumb = thumb ?: return
//		event.handle()
//		positionTmp.set(event.clientX + track.x - thumb.width * 0.5, event.clientY + track.y - thumb.height * 0.5)
//		val newValue = getModelValue(positionTmp)
//		val isPositive = newValue > scrollModel.value
//		if (!event.isFabricated)
//			pageDirection = isPositive
//		if (pageDirection == isPositive)
//			userChange(scrollModel.value + if (isPositive) pageSize else -pageSize)
//	}
//
//	private fun dragHandler(event: DragEvent) {
//		if (style.pageMode || event.isHandled) return
//		event.handle()
//		val track = track ?: return
//		val thumb = thumb ?: return
//		positionTmp.set(event.positionLocal.x + track.x - thumb.width * 0.5, event.positionLocal.y + track.y - thumb.height * 0.5)
//		val newValue = getModelValue(positionTmp)
//		userChange(newValue)
//	}
//
//	private fun dragStartHandler() {
//		val thumb = thumb!!
//		mousePosition(thumbOffset).sub(thumb.x, thumb.y)
//	}
//
//	private fun thumbDragHandler(event: DragEvent) {
//		event.handle()
//		mousePosition(positionTmp).sub(thumbOffset)
//		scrollModel.rawValue = getModelValue(positionTmp)
//	}
//
//	open fun stepDec() {
//		userChange(scrollModel.rawValue - stepSize / modelToPoints)
//	}
//
//	open fun stepInc() {
//		userChange(scrollModel.rawValue + stepSize / modelToPoints)
//	}
//
//	open fun pageUp() {
//		userChange(scrollModel.rawValue - pageSize)
//	}
//
//	open fun pageDown() {
//		userChange(scrollModel.rawValue + pageSize)
//	}
//
//	protected abstract fun getModelValue(position: Vector2Ro): Double
//	protected abstract val minTrack: Double
//	protected abstract val maxTrack: Double
//
//	private var _explicitPageSize: Double? = null
//
//	/**
//	 * Returns the explicit page size if it was set, or the size of the track divided by the modelToPoints ratio.
//	 */
//	var pageSize: Double
//		get() {
//			if (_explicitPageSize != null) return _explicitPageSize!!
//			return (maxTrack - minTrack) / modelToPoints
//		}
//		set(value) {
//			_explicitPageSize = value
//		}
//
//	/**
//	 * Sets the explicit page size. If set to null, the measured page size will be used.
//	 * Measured size: `((maxTrack - minTrack) / modelToPoints)`
//	 */
//	fun pageSize(value: Double?) {
//		_explicitPageSize = value
//	}
//
//	val naturalWidth: Double
//		get() {
//			validate(ValidationFlags.STYLES)
//			return style.naturalWidth
//		}
//
//	val naturalHeight: Double
//		get() {
//			validate(ValidationFlags.STYLES)
//			return style.naturalHeight
//		}
//
//	override val value: Double
//		get() = scrollModel.value
//
//	/**
//	 * Set the data as a user change, dispatching the [changed] signal.
//	 */
//	fun userChange(newValue: Double) {
//		scrollModel.rawValue = newValue
//		_changed.dispatch(this)
//	}
//
//	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
//		val decrementButton = decrementButton
//		val incrementButton = incrementButton
//		val track = track!!
//		val thumb = thumb!!
//		val w = explicitWidth ?: maxOf(minWidth, style.naturalWidth)
//		val h = explicitHeight ?: maxOf(minHeight, style.naturalHeight)
//
//		val decrementButtonLd = decrementButton?.layoutDataCast
//		decrementButton?.size(decrementButtonLd?.getPreferredWidth(null), decrementButtonLd?.getPreferredHeight(null))
//
//		val incrementButtonLd = incrementButton?.layoutDataCast
//		incrementButton?.size(incrementButtonLd?.getPreferredWidth(null), incrementButtonLd?.getPreferredHeight(null))
//
//		updatePartsLayout(w, h, decrementButton, incrementButton, track)
//
//		updateThumbLayout(w, h, thumb)
//		out.set(w, h)
//	}
//
//	/**
//	 * Update the size and position of the increment, decrement, and track.
//	 * @param width The explicit or natural width.
//	 * @param height The explicit or natural height.
//	 * @param decrementButton The decrement button skin part.
//	 * @param incrementButton The increment button skin part.
//	 * @param track The track skin part.
//	 */
//	protected abstract fun updatePartsLayout(width: Double, height: Double, decrementButton: UiComponent?, incrementButton: UiComponent?, track: UiComponent)
//
//	/**
//	 * Update the size and position of the thumb.
//	 * This is called from both [updateLayout] and when the scroll model is changed.
//	 *
//	 * @param width The explicit or natural width.
//	 * @param height The explicit or natural height.
//	 * @param thumb The thumb skin part.
//	 */
//	protected abstract fun updateThumbLayout(width: Double, height: Double, thumb: UiComponent)
//
//	@Suppress("UNCHECKED_CAST")
//	protected val LayoutElementRo.layoutDataCast: ScrollbarLayoutData?
//		get() = this.layoutData as ScrollbarLayoutData?
//
//}
//
//class ScrollbarStyle : ObservableBase() {
//
//	override val type: StyleType<ScrollbarStyle> = ScrollbarStyle
//
//	/**
//	 * If no explicit width is set, or [UiComponentImpl.defaultWidth] is set, this width will be used.
//	 */
//	var naturalWidth by prop(10.0)
//
//	/**
//	 * If no explicit height is set, or [UiComponentImpl.defaultHeight] is set, this height will be used.
//	 */
//	var naturalHeight by prop(10.0)
//
//	var decrementButton by prop<ScrollbarSkinPartOptional>(noSkinOptional)
//	var incrementButton by prop<ScrollbarSkinPartOptional>(noSkinOptional)
//	var track by prop<ScrollbarSkinPart>(noSkin)
//	var thumb by prop<ScrollbarSkinPart>(noSkin)
//
//	/**
//	 * If true (default), clicking the track steps [ScrollbarBase.pageSize].
//	 * If false, pressing/dragging the track snaps scroll value to the cursor position.
//	 *
//	 * NB: If this is true, the skin for the track should enable down repeat.
//	 * [com.acornui.input.interaction.enableDownRepeat].
//	 */
//	var pageMode by prop(true)
//
//	/**
//	 * When the scroll bar is inactive, the track will tween to this alpha value.
//	 * The scroll bar will be considered active if the scroll model value changes or there is user interactivity.
//	 */
//	var inactiveAlpha by prop(1.0)
//
//	/**
//	 * The duration to tween the alpha to [inactiveAlpha].
//	 */
//	var fadeOutDuration by prop(0.5.seconds)
//
//	/**
//	 * The delay after no interactivity before fading out.
//	 */
//	var fadeOutDelay by prop(0.7.seconds)
//
//	/**
//	 * The easing to apply when transitioning into inactive alpha.
//	 */
//	var fadeOutEasing by prop(Easing.pow2Out)
//
//	companion object : StyleType<ScrollbarStyle>
//}
//
//interface Scrollbar : Context, LayoutDataProvider<ScrollbarLayoutData>, InputComponent<Double> {
//	companion object : StyleTag
//}
//
//typealias ScrollbarSkinPart = Scrollbar.() -> UiComponent
//typealias ScrollbarSkinPartOptional = Scrollbar.() -> UiComponent?
//
//class ScrollbarLayoutData : BasicLayoutData() {
//
//	/**
//	 * For horizontal scroll bars, this is the skin part's vertical alignment.
//	 *
//	 * NB: [VAlign.BASELINE] is not supported here.
//	 */
//	var verticalAlign: VAlign by bindable(VAlign.MIDDLE)
//
//	/**
//	 * For vertical scroll bars, this is the skin part's horizontal alignment.
//	 */
//	var horizontalAlign: HAlign by bindable(HAlign.CENTER)
//
//}
//
//inline fun scrollbarStyle(init: ComponentInit<ScrollbarStyle> = {}): ScrollbarStyle {
//	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
//	return ScrollbarStyle().apply(init)
//}