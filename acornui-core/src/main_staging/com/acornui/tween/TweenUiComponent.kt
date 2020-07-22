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
import com.acornui.time.FrameDriver

fun UiComponent.tweenAlpha(duration: Double, ease: Interpolation, toAlpha: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "alpha", duration, ease, { alpha }, { alpha = it }, toAlpha, delay)
}

fun UiComponent.tweenX(duration: Double, ease: Interpolation, toX: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "x", duration, ease, { x }, { x = it }, toX, delay)
}

fun UiComponent.tweenY(duration: Double, ease: Interpolation, toY: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "y", duration, ease, { y }, { y = it }, toY, delay)
}

fun UiComponent.tweenZ(duration: Double, ease: Interpolation, toZ: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "z", duration, ease, { z }, { z = it }, toZ, delay)
}

fun UiComponent.tweenScaleX(duration: Double, ease: Interpolation, toScaleX: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "scaleX", duration, ease, { scaleX }, { scaleX = it }, toScaleX, delay)
}

fun UiComponent.tweenScaleY(duration: Double, ease: Interpolation, toScaleY: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "scaleY", duration, ease, { scaleY }, { scaleY = it }, toScaleY, delay)
}

fun UiComponent.tweenRotationX(duration: Double, ease: Interpolation, toRotationX: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "rotationX", duration, ease, { rotationX }, { rotationX = it }, toRotationX, delay)
}

fun UiComponent.tweenRotationY(duration: Double, ease: Interpolation, toRotationY: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "rotationY", duration, ease, { rotationY }, { rotationY = it }, toRotationY, delay)
}

fun UiComponent.tweenRotation(duration: Double, ease: Interpolation, toRotation: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "rotation", duration, ease, { rotation }, { rotation = it }, toRotation, delay)
}

fun UiComponent.tweenTint(duration: Double, ease: Interpolation, toTint: Color, delay: Double = 0.0): Tween {
	TweenRegistry.kill(this, "tint", finish = true)
	val fromTint = colorTint.copy()
	val tint = Color()
	val tween = TweenImpl(inject(frameDriverKey), duration, ease, delay, loop = false) {
		previousAlpha: Double, currentAlpha: Double ->
		tint.set(fromTint).lerp(toTint, currentAlpha)
	}
	TweenRegistry.register(this, "tint", tween)
	return tween
}
