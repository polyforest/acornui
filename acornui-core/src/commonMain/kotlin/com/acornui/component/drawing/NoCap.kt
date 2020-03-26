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

package com.acornui.component.drawing

import com.acornui.gl.core.ShaderBatch
import com.acornui.gl.core.putVertex
import com.acornui.math.Vector2Ro
import com.acornui.math.vec2

/**
 * @author nbilyk
 */
object NoCap : CapBuilder {

	private val perpLine = vec2()
	private val dirLine = vec2()

	override fun createCap(p1: Vector2Ro, p2: Vector2Ro, control: Vector2Ro?, meshRegion: ShaderBatch, lineStyle: LineStyleRo, controlLineThickness: Float, clockwise: Boolean) {
		val t = (if (clockwise) lineStyle.thickness else -lineStyle.thickness) * 0.5f
		// Cap A
		dirLine.set(p2).sub(p1).nor()

		// OUTER
		perpLine.set(dirLine.y, -dirLine.x).scl(t).add(p1)
		meshRegion.putVertex(perpLine, -0.001f, colorTint = lineStyle.colorTint)

		// INNER
		perpLine.set(-dirLine.y, dirLine.x).scl(t).add(p1)
		meshRegion.putVertex(perpLine, -0.001f, colorTint = lineStyle.colorTint)
	}
}
