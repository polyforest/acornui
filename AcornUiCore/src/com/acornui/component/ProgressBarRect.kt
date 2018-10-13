package com.acornui.component

import com.acornui.action.Progress
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.core.Disposable
import com.acornui.core.assets.AssetManager
import com.acornui.core.assets.onLoadersEmpty
import com.acornui.core.assets.secondsRemaining
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.popup.PopUpInfo
import com.acornui.core.popup.addPopUp
import com.acornui.core.popup.removePopUp
import com.acornui.core.time.onTick
import com.acornui.core.time.timer
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*

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
	var borderColors: BorderColorsRo by prop(BorderColors(Color.BLUE))
	var bgColor: ColorRo by prop(Color.GREEN.copy())
	var fillColor: ColorRo by prop(Color.RED.copy())

	companion object : StyleType<ProgressBarRectStyle>
}

fun Owned.progressBarRect(init: ComponentInit<ProgressBarRect> = {}): ProgressBarRect {
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
	progressBar.watch(inject(AssetManager))
	progressBar
}

private var progressBarPopUp: PopUpInfo<UiComponent>? = null
fun Owned.showAssetLoadingBar(onCompleted: () -> Unit = {}) {
	val assetManager = inject(AssetManager)
	if (assetManager.secondsRemaining < 0.5f) return onCompleted() // Close enough
	assetManager.onLoadersEmpty(onCompleted)

	if (progressBarPopUp == null) {
		// We only want a single progress bar pop up.
		val progressBar = progressBar()
		progressBarPopUp = PopUpInfo(progressBar, priority = 1000f, dispose = false, onCloseRequested = { false })
	}

	val popUp = progressBarPopUp!!
	addPopUp(popUp)

	lateinit var loadersEmptyHandler: ()->Unit
	loadersEmptyHandler = {
		if (assetManager.currentLoaders.isEmpty()) {
			removePopUp(progressBarPopUp!!)
			onCompleted()
		} else {
			timer(0.25f, callback = loadersEmptyHandler)
		}
		Unit
	}
	assetManager.onLoadersEmpty(loadersEmptyHandler)
}
