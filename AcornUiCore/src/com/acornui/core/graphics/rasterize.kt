/*
 * Copyright 2018 PolyForest
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

package com.acornui.core.graphics

import com.acornui.component.TextureComponent
import com.acornui.component.UiComponent
import com.acornui.component.textureC
import com.acornui.core.config
import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped
import com.acornui.gl.core.Framebuffer
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.math.MathUtils
import com.acornui.math.MinMax
import com.acornui.math.ceil

// TODO: reconsider overriding the camera and adding to the new Scene component.

/**
 * Does a one time rasterization of the given target to a texture.
 */
fun Scoped.rasterize(target: UiComponent, hasDepth: Boolean = config.gl.depth, hasStencil: Boolean = config.gl.stencil): Rasterized {
	if (target.parent != null)
		throw Exception("rasterize should be called only on orphan components.")

	target.update()
	val bounds = MinMax()
	bounds.set(0f, 0f, target.width, target.height)
	target.localToGlobal(bounds)
	val w = bounds.width
	val h = bounds.height
	if (w <= 0f || h <= 0f) throw Exception("Cannot rasterize a component with an empty width or height.")
	val framebuffer = Framebuffer(injector, MathUtils.nextPowerOfTwo(w.ceil()), MathUtils.nextPowerOfTwo(h.ceil()), hasDepth, hasStencil)
	target.cameraOverride = FramebufferOrthographicCamera().apply {
		setViewport(framebuffer.width.toFloat(), framebuffer.height.toFloat())
		setPosition(position.x + bounds.xMin, position.y + bounds.yMin)
	}
	framebuffer.texture.filterMag = TextureMagFilter.LINEAR
	framebuffer.texture.filterMin = TextureMinFilter.LINEAR_MIPMAP_NEAREST

	framebuffer.drawTo {
		target.render(MinMax(0f, 0f, w, h))
	}

	return Rasterized(framebuffer.texture, w / framebuffer.width.toFloat(), h / framebuffer.height.toFloat(), -bounds.xMin, -bounds.yMin)
}

class Rasterized(val texture: Texture, val u: Float, val v: Float, val originX: Float, val originY: Float) {
	fun createTextureComponent(owner: Owned): TextureComponent {
		return owner.textureC(texture) {
			setUv(0f, 0f, u, v, isRotated = false)
			setOrigin(originX, originY)
		}
	}
}
