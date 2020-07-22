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
import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.io.Progress
import com.acornui.io.progressReporterKey
import com.acornui.io.remaining
import com.acornui.math.*
import com.acornui.popup.PopUpInfo
import com.acornui.popup.addPopUp
import com.acornui.popup.removePopUp
import com.acornui.time.onTick
import com.acornui.time.tick
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * A progress bar made from simple rectangles.
 */
class ProgressBarRect(owner: Context) : ContainerImpl(owner) {

	val style = bind(ProgressBarRectStyle())

	val backRect = addChild(rect {
		includeInLayout = false
	})

	val frontRect = addChild(rect{
		includeInLayout = false
	})

	init {
		addClass(ProgressBarRect)
		watch(style) {
			backRect.style.backgroundColor = it.bgColor
			backRect.style.borderColors = it.borderColors
			backRect.style.borderThicknesses = it.borderThicknesses
			backRect.style.borderRadii = it.borderRadii

			frontRect.style.backgroundColor = it.fillColor
		}
	}

	private var _progress: Double = 0.0

	/**
	 * The current progress, between 0.0 and 1.0
	 */
	var progress: Double
		get() = _progress
		set(value) {
			if (_progress == value) return
			_progress = value
			invalidate(ValidationFlags.LAYOUT)
		}

	private var _watched: Disposable? = null
	private var targetP: Double = 0.0

	fun watch(target: Progress) {
		_watched?.dispose()
		_watched = onTick {
			targetP = if (target.total == Duration.ZERO) 0.0
			else (target.loaded / target.total).toDouble()
			progress += (targetP - progress) * 0.1.0
		}
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val s = style
		val w = explicitWidth ?: s.defaultWidth
		val h = explicitHeight ?: s.defaultHeight

		backRect.size(w, h)

		val fillMaxW = s.borderThicknesses.reduceWidth(w)
		val fillH = s.borderThicknesses.reduceHeight(h)
		frontRect.size(fillMaxW * _progress, fillH)
		frontRect.position(s.borderThicknesses.left, s.borderThicknesses.top)
		out.set(w, h)
	}

	fun reset() {
		progress = 0.0
	}

	companion object : StyleTag
}

class ProgressBarRectStyle : ObservableBase() {

	override val type: StyleType<ProgressBarRectStyle> = ProgressBarRectStyle

	var defaultWidth by prop(100.0)
	var defaultHeight by prop(6.0)
	var borderThicknesses: PadRo by prop(Pad(2.0))
	var borderRadii: CornersRo by prop(Corners())
	var borderColors: BorderColorsRo by prop(BorderColors(Color.CLEAR))
	var bgColor: ColorRo by prop(Color.CLEAR)
	var fillColor: ColorRo by prop(Color.CLEAR)

	companion object : StyleType<ProgressBarRectStyle>
}

inline fun Context.progressBarRect(init: ComponentInit<ProgressBarRect> = {}): ProgressBarRect  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val p = ProgressBarRect(this)
	p.init()
	return p
}

/**
 * The factory for creating the loading bar.  This must be set before [showAssetLoadingBar] is ever called
 * (The component is only created once.)
 */
var progressBar: Context.()->UiComponent = {
	val progressBar = progressBarRect()
	progressBar.watch(inject(progressReporterKey))
	progressBar
}

private var progressBarPopUp: PopUpInfo<UiComponent>? = null
fun Context.showAssetLoadingBar(progress: Progress = inject(progressReporterKey), onCompleted: () -> Unit = {}) {
	if (progress.remaining < 0.4.seconds) return onCompleted() // Close enough

	if (progressBarPopUp == null) {
		// We only want a single progress bar pop up.
		val progressBar = inject(Stage).progressBar()
		progressBarPopUp = PopUpInfo(progressBar, priority = 1000.0, dispose = false, onCloseRequested = { false })
	}

	val popUp = progressBarPopUp!!
	addPopUp(popUp)

	tick {
		if (!progress.isLoading) {
			removePopUp(progressBarPopUp!!)
			onCompleted()
			dispose()
		}
	}
}
