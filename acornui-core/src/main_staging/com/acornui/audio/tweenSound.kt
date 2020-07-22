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

package com.acornui.audio

import com.acornui.di.Context
import com.acornui.math.Interpolation
import com.acornui.tween.Tween
import com.acornui.tween.createPropertyTween

fun Context.tweenVolume(music: Music, duration: Double, ease: Interpolation, toVolume: Double, delay: Double = 0.0): Tween {
	return createPropertyTween(this, "volume", duration, ease, { music.volume }, { music.volume = it }, toVolume, delay)
}
