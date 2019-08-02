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

package com.acornui.component.performance

import com.acornui.component.RenderContextRo
import com.acornui.component.text.TextFieldImpl
import com.acornui.di.Owned
import com.acornui.graphic.Color
import com.acornui.time.timer

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

	override fun draw(renderContext: RenderContextRo) {
		super.draw(renderContext)
		frames++
	}
}
