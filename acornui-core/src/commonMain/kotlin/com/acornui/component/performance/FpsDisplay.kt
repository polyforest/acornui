package com.acornui.component.performance

import com.acornui.component.text.TextFieldImpl
import com.acornui.core.di.Owned
import com.acornui.core.time.timer
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo

class FpsDisplay(owner: Owned) : TextFieldImpl(owner) {

	private var frames = 0

	var goodFps = 30
	var okFps = 16
	var goodColor = Color.GREEN * 0.5f
	var okColor = Color.ORANGE * 0.75f
	var badColor = Color.RED * 0.75f

	var fps: Int = 0
		private set

	init {
		charStyle.colorTint = Color.WHITE
		val interval = 2f
		timer(interval, -1) {
			fps = (frames.toFloat() / interval).toInt()
			frames = 0
			text = fps.toString()
			colorTint = when {
				fps > goodFps -> goodColor
				fps > okFps -> okColor
				else -> badColor
			}
		}
	}

	override fun render() {
		super.render()
		frames++
	}
}