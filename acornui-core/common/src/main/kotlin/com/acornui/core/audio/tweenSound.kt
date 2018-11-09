package com.acornui.core.audio

import com.acornui.core.tween.Tween
import com.acornui.core.tween.createPropertyTween
import com.acornui.math.Interpolation

fun Music.tweenVolume(duration: Float, ease: Interpolation, toVolume: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "volume", duration, ease, { volume }, { volume = it }, toVolume, delay)
}