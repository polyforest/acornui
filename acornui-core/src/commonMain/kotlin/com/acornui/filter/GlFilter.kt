/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.filter

import com.acornui.component.Sprite
import com.acornui.component.StackLayoutContainer
import com.acornui.core.di.Owned
import com.acornui.gl.core.Framebuffer
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.ShaderProgram
import com.acornui.math.*

// TODO: WIP

/**
 * Draws the contents to a framebuffer with the specified shader, then draws to screen.
 */
class GlFilter(
		owner: Owned,
		val shader: ShaderProgram,
		val hasStencil: Boolean = true,
		val hasDepth: Boolean = true
) : StackLayoutContainer(owner) {

	/**
	 * The framebuffer size will be this container's size plus this padding to the next power of two.
	 */
	val sizePadding = Pad()
	
	var maxSize = 1024

	private var framebuffer: Framebuffer? = null
	//private val framebuffer = Framebuffer(injector, directionalShadowsResolution, directionalShadowsResolution, hasStencil = true, hasDepth = false)

	private val tempTransform = Matrix4()
	private val sprite = Sprite()
	
	override fun draw(clip: MinMaxRo) {
		tempTransform.set(_concatenatedTransform)
		
		
		val fbWidth = minOf(maxSize, MathUtils.nextPowerOfTwo(sizePadding.expandWidth2(width).ceil()))
		val fbHeight = minOf(maxSize, MathUtils.nextPowerOfTwo(sizePadding.expandHeight2(height).ceil()))
		val framebuffer = framebuffer
		if (framebuffer == null || fbWidth > framebuffer.width || fbHeight > framebuffer.height) {
			resizeFramebuffer(fbWidth, fbHeight)
		}
		val previousShader = glState.shader
		framebuffer!!.begin()

		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
		super.draw(clip)
		framebuffer.end()

		glState.shader = shader

		sprite.texture = framebuffer.texture
		sprite.updateWorldVertices(concatenatedTransform, sprite.naturalWidth, sprite.naturalHeight, z = 0f)
		glState.setCamera(camera)
		sprite.render(glState, concatenatedColorTint)

		glState.shader = previousShader
	}

	private fun resizeFramebuffer(width: Int, height: Int) {
		framebuffer?.dispose()
		framebuffer = Framebuffer(injector, width, height, hasStencil, hasDepth)
	}
	
}