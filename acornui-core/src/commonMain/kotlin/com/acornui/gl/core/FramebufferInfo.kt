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

package com.acornui.gl.core

import com.acornui.ceilInt
import com.acornui.math.BoxRo
import com.acornui.math.IntRectangle
import com.acornui.math.RectangleRo
import com.acornui.recycle.Clearable

interface FramebufferInfoRo {

	val framebuffer: GlFramebufferRef?

	/**
	 * The width of the frame buffer.
	 */
	val width: Int

	/**
	 * The height of the frame buffer.
	 */
	val height: Int

	/**
	 * The x scale of canvas coordinates to screen coordinates.
	 */
	val scaleX: Float

	/**
	 * The y scale of canvas coordinates to screen coordinates.
	 */
	val scaleY: Float

	/**
	 * True if 0,0 is the top left of the framebuffer.
	 */
	val yDown: Boolean
		get() = framebuffer != null

	fun copy(): FramebufferInfo {
		return FramebufferInfo(framebuffer, width, height, scaleX, scaleY)
	}
}

data class FramebufferInfo(
		override var framebuffer: GlFramebufferRef?,
		override var width: Int,
		override var height: Int,
		override var scaleX: Float,
		override var scaleY: Float

) : FramebufferInfoRo, Clearable {

	constructor() : this(null, 0, 0, 1f, 1f)

	fun set(value: FramebufferInfoRo): FramebufferInfo {
		framebuffer = value.framebuffer
		width = value.width
		height = value.height
		scaleX = value.scaleX
		scaleY = value.scaleY
		return this
	}

	fun set(
			framebuffer: GlFramebufferRef?,
			width: Int,
			height: Int,
			scaleX: Float,
			scaleY: Float
	): FramebufferInfo {
		this.framebuffer = framebuffer
		this.width = width
		this.height = height
		this.scaleX = scaleX
		this.scaleY = scaleY
		return this
	}

	fun equals(framebuffer: GlFramebufferRef?,
			   width: Int,
			   height: Int,
			   scaleX: Float,
			   scaleY: Float): Boolean {
		return this.framebuffer == framebuffer && this.width == width && this.height == height && this.scaleX == scaleX && this.scaleY == scaleY
	}

	override fun clear() {
		framebuffer = null
		width = 0
		height = 0
		scaleX = 1f
		scaleY = 1f
	}
}

	/**
	 * Takes the box in canvas coordinates (unscaled, yDown, floats), and converts it to screen coordinates
	 * (dpi scaled, yUp, ints)
	 */
fun FramebufferInfoRo.canvasToScreen(box: RectangleRo, out: IntRectangle): IntRectangle {
	val sX = scaleX
	val sY = scaleY
	val newX = (box.x * sX + 0.001f).toInt()
	val newY = (box.y * sY + 0.001f).toInt()
	val newR = ceilInt(box.right * sX - 0.001f)
	val newB = ceilInt(box.bottom * sY - 0.001f)
	return out.set(newX, if (yDown) newY else height - newB, newR - newX, newB - newY)
}