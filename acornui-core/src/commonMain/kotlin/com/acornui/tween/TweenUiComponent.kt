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

package com.acornui.tween

import com.acornui.component.UiComponent
import com.acornui.component.alpha
import com.acornui.graphic.Color
import com.acornui.math.Interpolation
import com.acornui.time.FrameDriverRo

fun UiComponent.tweenAlpha(duration: Float, ease: Interpolation, toAlpha: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "alpha", duration, ease, { alpha }, { alpha = it }, toAlpha, delay)
}

fun UiComponent.tweenX(duration: Float, ease: Interpolation, toX: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "x", duration, ease, { x }, { x = it }, toX, delay)
}

fun UiComponent.tweenY(duration: Float, ease: Interpolation, toY: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "y", duration, ease, { y }, { y = it }, toY, delay)
}

fun UiComponent.tweenZ(duration: Float, ease: Interpolation, toZ: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "z", duration, ease, { z }, { z = it }, toZ, delay)
}

fun UiComponent.tweenScaleX(duration: Float, ease: Interpolation, toScaleX: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "scaleX", duration, ease, { scaleX }, { scaleX = it }, toScaleX, delay)
}

fun UiComponent.tweenScaleY(duration: Float, ease: Interpolation, toScaleY: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "scaleY", duration, ease, { scaleY }, { scaleY = it }, toScaleY, delay)
}

fun UiComponent.tweenRotationX(duration: Float, ease: Interpolation, toRotationX: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "rotationX", duration, ease, { rotationX }, { rotationX = it }, toRotationX, delay)
}

fun UiComponent.tweenRotationY(duration: Float, ease: Interpolation, toRotationY: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "rotationY", duration, ease, { rotationY }, { rotationY = it }, toRotationY, delay)
}

fun UiComponent.tweenRotation(duration: Float, ease: Interpolation, toRotation: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "rotation", duration, ease, { rotation }, { rotation = it }, toRotation, delay)
}

fun UiComponent.tweenTint(duration: Float, ease: Interpolation, toTint: Color, delay: Float = 0f): Tween {
	TweenRegistry.kill(this, "tint", finish = true)
	val fromTint = colorTint.copy()
	val tint = Color()
	val tween = TweenImpl(inject(FrameDriverRo), duration, ease, delay, loop = false) {
		previousAlpha: Float, currentAlpha: Float ->
		tint.set(fromTint).lerp(toTint, currentAlpha)
	}
	TweenRegistry.register(this, "tint", tween)
	return tween
}
