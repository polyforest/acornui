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

import com.acornui.Disposable
import com.acornui.collection.ActiveList
import com.acornui.component.layout.algorithm.CanvasLayoutData
import com.acornui.component.layout.algorithm.canvasLayoutData
import com.acornui.component.layout.size
import com.acornui.component.style.StyleTag
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.dependencyFactory
import com.acornui.function.as1
import com.acornui.function.as2
import com.acornui.input.interaction.rollOut
import com.acornui.input.interaction.rollOver
import com.acornui.input.mouseDown
import com.acornui.input.touchStart
import com.acornui.mainContext
import com.acornui.math.Bounds
import com.acornui.math.Easing
import com.acornui.math.vec2
import com.acornui.popup.PopUpInfo
import com.acornui.popup.PopUpManager
import com.acornui.popup.PopUpPriority
import com.acornui.time.tick
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TooltipAttachment(val target: UiComponent) : ContextImpl(target) {

	val tooltipManager = inject(TooltipManager)

	private var isOver: Boolean = false
	private var timerHandle: Disposable? = null
	private var showCountdown = showDelay
	private var hideCountdown = hideDelay
	private val dT = mainContext.looper.frameTime.inSeconds.toDouble()

	init {
		target.rollOver(isCapture = true).add(::targetRollOverHandler.as1)
		target.rollOut(isCapture = true).add(::targetRollOutHandler.as1)
		target.mouseDown(isCapture = true).add(::resetShowCountdown.as1)
		target.touchStart(isCapture = true).add(::resetShowCountdown.as1)
	}

	private fun targetRollOverHandler() {
		hideCountdown = hideDelay
		isOver = true
		if (timerHandle == null) {
			timerHandle = tick {
				if (isOver) {
					showCountdown -= dT
					if (showCountdown <= 0.0) {
						if (activeTip == null)
							activeTip = tooltip
						activeTip?.view?.alpha = minOf(1.0, fadeInEasing.apply(-showCountdown / fadeInDuration))
					}
				} else {
					hideCountdown -= dT
					if (hideCountdown <= 0.0) {
						activeTip?.view?.alpha = maxOf(0.0, 1.0 - fadeOutEasing.apply(-hideCountdown / fadeOutDuration))
						if (hideCountdown <= -fadeOutDuration) {
							activeTip = null
							timerHandle?.dispose()
							timerHandle = null
							showCountdown = showDelay
						}
					}
				}
			}
		}
	}

	private fun targetRollOutHandler() {
		isOver = false
	}

	private fun resetShowCountdown() {
		showCountdown = showDelay
	}

	private var _activeTip: Tooltip<*>? = null
	private var activeTip: Tooltip<*>?
		get() = _activeTip
		set(value) {
			val oldTip = _activeTip
			if (oldTip != null)
				tooltipManager.tooltips.remove(oldTip)
			_activeTip = value
			if (value != null)
				tooltipManager.tooltips.add(value)
		}

	var tooltip: Tooltip<*>? = null
		set(value) {
			val oldValue = field
			if (oldValue != value) {
				field = value
				if (activeTip != null)
					activeTip = value
			}
		}

	override fun dispose() {
		super.dispose()
		target.rollOver(isCapture = true).remove(::targetRollOverHandler.as1)
		target.rollOut(isCapture = true).remove(::targetRollOutHandler.as1)
		target.mouseDown(isCapture = true).remove(::resetShowCountdown.as1)
		target.touchStart(isCapture = true).remove(::resetShowCountdown.as1)

		activeTip = null
		timerHandle?.dispose()
		timerHandle = null
	}

	companion object {
		var showDelay = 1.0
		var hideDelay = 0.5
		var fadeInDuration = 0.2
		var fadeOutDuration = 0.3
		var fadeInEasing = Easing.pow2In
		var fadeOutEasing = Easing.pow2Out
	}

}

interface TooltipManager {

	val tooltips: MutableList<Tooltip<*>>

	fun createTooltip(value: String): Tooltip<String>

	companion object : Context.Key<TooltipManager>
}

class TooltipManagerImpl(owner: Context) : ContextImpl(owner), TooltipManager {


	private val popUpManager by PopUpManager
	private val stage by Stage

	private val _tooltips = ActiveList<Tooltip<*>>()
	override val tooltips: MutableList<Tooltip<*>> = _tooltips

	private val toolTipLayoutData: CanvasLayoutData = canvasLayoutData()

	private var enterFrameHandle: Disposable? = null

	private val defaultTooltipView: ItemRenderer<String> = TooltipView(this)

	init {
		_tooltips.addBinding {
			@Suppress("UNCHECKED_CAST")
			activeTooltip = _tooltips.lastOrNull() as Tooltip<Any?>?
		}
	}

	override fun createTooltip(value: String): Tooltip<String> = Tooltip(value, defaultTooltipView)

	private var activeTooltip: Tooltip<Any?>? = null
		set(value) {
			if (field != value) {
				val previousView = field?.view
				field = value
				val newView = value?.view
				newView?.data = value?.data
				if (previousView != newView) {
					if (previousView != null && !previousView.isDisposed) {
						popUpManager.removePopUp(previousView)
					}
					if (newView != null && !newView.isDisposed) {
						popUpManager.addPopUp(PopUpInfo(newView, isModal = false, focus = false, dispose = false, priority = value.priority, layoutData = toolTipLayoutData))
					}
				}
				followMouse = value != null
			}
		}

	private var followMouse: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				enterFrameHandle = if (value) {
					stage.tick(callback = ::frameHandler.as2)
				} else {
					enterFrameHandle?.dispose()
					null
				}
			}
		}

	private val mousePosition = vec2()
	private val cursorWidth = 13.0
	private val cursorHeight = 12.0
	private val gap = 5.0

	private fun frameHandler() {
		val view = activeTooltip?.view ?: return
		view.parent?.mousePosition(mousePosition)
		toolTipLayoutData.left = mousePosition.x + cursorWidth + gap
		toolTipLayoutData.top = mousePosition.y + cursorHeight + gap
	}

	override fun dispose() {
		activeTooltip = null
		_tooltips.clear()
	}
}

data class Tooltip<E>(
		val data: E,
		val view: ItemRenderer<E>,
		val priority: Double = PopUpPriority.TOOLTIP
)

class TooltipView(owner: Context) : ContainerImpl(owner), ItemRenderer<String> {

	val style = bind(PanelStyle())

	private var background: UiComponent? = null
	private val textField = addChild(text())

	init {
		interactivityMode = InteractivityMode.NONE
		addClassAll(listOf(Companion, ItemRenderer))

		watch(style) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))
		}
	}

	override var data: String?
		get() = textField.text
		set(value) {
			textField.text = value ?: ""
		}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val padding = style.padding
		val childWidth = padding.reduceWidth(explicitWidth)
		val childHeight = padding.reduceHeight(explicitHeight)
		textField.size(childWidth, childHeight)
		textField.position(padding.left, padding.top)
		out.set(padding.expandWidth(textField.width), padding.expandHeight(textField.height))
		background?.size(out)
	}

	companion object : StyleTag
}

inline fun Context.tooltipView(init: ComponentInit<TooltipView> = {}): TooltipView {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TooltipView(this).apply(init)
}

fun UiComponent.tooltipAttachment(): TooltipAttachment {
	return createOrReuseAttachment(TooltipAttachment) { TooltipAttachment(this) }
}

fun UiComponent.tooltip(value: String?) {
	val attachment = tooltipAttachment()
	attachment.tooltip = if (value == null) null else attachment.tooltipManager.createTooltip(value)
}
