/*
 * Copyright 2015 Nicholas Bilyk
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
import com.acornui.math.*

/**
 * @author nbilyk
 */
object MiterCap : CapBuilder {

	private val perpLine = Vector2()
	private val perpControl = Vector2()
	private val dirLine = Vector2()
	private val dirControl = Vector2()
	private val joinedPoint = Vector2()

	override fun createCap(p1: Vector2Ro, p2: Vector2Ro, control: Vector2Ro?, meshRegion: ShaderBatch, lineStyle: LineStyleRo, controlLineThickness: Float, clockwise: Boolean) {
		if (control == null) {
			NoCap.createCap(p1, p2, null, meshRegion, lineStyle, controlLineThickness, clockwise)
			return
		}
		val t = (if (clockwise) lineStyle.thickness else -lineStyle.thickness) * 0.5f
		val t2 = (if (clockwise) controlLineThickness else -controlLineThickness) * 0.5f

		dirLine.set(p2).sub(p1).nor()
		dirControl.set(p1).sub(control).nor()

		// OUTER
		perpLine.set(dirLine.y, -dirLine.x).scl(t).add(p1)
		perpControl.set(dirControl.y, -dirControl.x).scl(t2).add(p1)
		Ray2.intersects(perpLine, dirLine, perpControl, dirControl, joinedPoint)

		meshRegion.putVertex(joinedPoint, -0.001f, colorTint = lineStyle.colorTint)

		// INNER
		perpLine.set(-dirLine.y, dirLine.x).scl(t).add(p1)
		perpControl.set(-dirControl.y, dirControl.x).scl(t2).add(p1)
		Ray2.intersects(perpLine, dirLine, perpControl, dirControl, joinedPoint)

		meshRegion.putVertex(joinedPoint, -0.001f, colorTint = lineStyle.colorTint)
	}
}