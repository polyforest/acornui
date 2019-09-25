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
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.di.Owned
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.io.GlobalProgressReporter
import com.acornui.io.Progress
import com.acornui.io.isLoading
import com.acornui.io.secondsRemaining
import com.acornui.math.*
import com.acornui.popup.PopUpInfo
import com.acornui.popup.addPopUp
import com.acornui.popup.removePopUp
import com.acornui.time.onTick
import com.acornui.time.tick
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A progress bar made from simple rectangles.
 */
class ProgressBarRect(owner: Owned) : ContainerImpl(owner) {

	val style = bind(ProgressBarRectStyle())

	val backRect = addChild(rect {
		includeInLayout = false
	})

	val frontRect = addChild(rect{
		includeInLayout = false
	})

	init {
		styleTags.add(ProgressBarRect)
		watch(style) {
			backRect.style.backgroundColor = it.bgColor
			backRect.style.borderColors = it.borderColors
			backRect.style.borderThicknesses = it.borderThicknesses
			backRect.style.borderRadii = it.borderRadii

			frontRect.style.backgroundColor = it.fillColor
		}
	}

	private var _progress: Float = 0f

	/**
	 * The current progress, between 0f and 1f
	 */
	var progress: Float
		get() = _progress
		set(value) {
			if (_progress == value) return
			_progress = value
			invalidate(ValidationFlags.LAYOUT)
		}

	private var _watched: Disposable? = null
	private var targetP: Float = 0f

	fun watch(target: Progress) {
		_watched?.dispose()
		_watched = onTick {
			targetP = if (target.secondsTotal == 0f) 0f
			else target.secondsLoaded / target.secondsTotal
			progress += (targetP - progress) * 0.1f
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val s = style
		val w = explicitWidth ?: s.defaultWidth
		val h = explicitHeight ?: s.defaultHeight

		backRect.setSize(w, h)

		val fillMaxW = s.borderThicknesses.reduceWidth2(w)
		val fillH = s.borderThicknesses.reduceHeight2(h)
		frontRect.setSize(fillMaxW * _progress, fillH)
		frontRect.setPosition(s.borderThicknesses.left, s.borderThicknesses.top)
		out.set(w, h)
	}

	fun reset() {
		progress = 0f
	}

	companion object : StyleTag
}

class ProgressBarRectStyle : StyleBase() {

	override val type: StyleType<ProgressBarRectStyle> = ProgressBarRectStyle

	var defaultWidth by prop(100f)
	var defaultHeight by prop(6f)
	var borderThicknesses: PadRo by prop(Pad(2f))
	var borderRadii: CornersRo by prop(Corners())
	var borderColors: BorderColorsRo by prop(BorderColors(Color.CLEAR))
	var bgColor: ColorRo by prop(Color.CLEAR)
	var fillColor: ColorRo by prop(Color.CLEAR)

	companion object : StyleType<ProgressBarRectStyle>
}

inline fun Owned.progressBarRect(init: ComponentInit<ProgressBarRect> = {}): ProgressBarRect  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val p = ProgressBarRect(this)
	p.init()
	return p
}

/**
 * The factory for creating the loading bar.  This must be set before [showAssetLoadingBar] is ever called
 * (The component is only created once.)
 */
var progressBar: Owned.()->UiComponent = {
	val progressBar = progressBarRect()
	progressBar.watch(GlobalProgressReporter)
	progressBar
}

private var progressBarPopUp: PopUpInfo<UiComponent>? = null
fun Scoped.showAssetLoadingBar(progress: Progress = GlobalProgressReporter, onCompleted: () -> Unit = {}) {
	if (progress.secondsRemaining < 0.5f) return onCompleted() // Close enough

	if (progressBarPopUp == null) {
		// We only want a single progress bar pop up.
		val progressBar = inject(Stage).progressBar()
		progressBarPopUp = PopUpInfo(progressBar, priority = 1000f, dispose = false, onCloseRequested = { false })
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
