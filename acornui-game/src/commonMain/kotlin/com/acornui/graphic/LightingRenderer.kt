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

@file:Suppress("LocalVariableName", "PrivatePropertyName")

package com.acornui.graphic

import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.CameraRo
import com.acornui.core.graphic.Window
import com.acornui.gl.core.*
import com.acornui.graphic.lighting.*
import com.acornui.math.Matrix4

/**
 * @author nbilyk
 */
class LightingRenderer(
		override val injector: Injector,
		val numPointLights: Int = 10,
		val numShadowPointLights: Int = 3,
		private val directionalShadowMapShader: ShaderProgram = DirectionalShadowShader(injector.inject(Gl20)),
		private val pointShadowMapShader: ShaderProgram = PointShadowShader(injector.inject(Gl20)),

		directionalShadowsResolution: Int = 1024,
		pointShadowsResolution: Int = 1024,

		/**
		 * True if the shadow frame buffers should use the stencil and depth attachment.
		 * Note that if this is true, the stencil buffer will only work if the GL_OES_packed_depth_stencil or
		 * GL_EXT_packed_depth_stencil extension is true.
		 */
		hasStencil: Boolean = false
) : Scoped, Disposable {

	var directionalShadowUnit = 1
	var pointShadowUnit = 2

	private val gl = inject(Gl20)
	private val glState = inject(GlState)
	private val window = inject(Window)

	private val directionalShadowsFbo = Framebuffer(injector, directionalShadowsResolution, directionalShadowsResolution, hasDepth = true, hasStencil = hasStencil)
	val directionalLightCamera = DirectionalLightCamera()

	// Point lights
	private val pointLightShadowMaps: Array<CubeMap>
	private val pointShadowsFbo = Framebuffer(injector, pointShadowsResolution, pointShadowsResolution, hasDepth = true, hasStencil = hasStencil)
	private val pointLightCamera = PointLightCamera(window, pointShadowsResolution.toFloat())

	private val bias = Matrix4().apply {
		scl(0.5f, 0.5f, 0.5f)
		translate(1f, 1f, 1f)
	}

	/**
	 * Set to false to disable rendering shadows.
	 */
	@Suppress("MemberVisibilityCanBePrivate")
	var allowShadows: Boolean = true

	private val shadowsBatch = shaderBatch(uiVertexAttributes)
	private val lightingBatch = shaderBatch(standardVertexAttributes)

	//--------------------------------------------
	// DrivableComponent methods
	//--------------------------------------------

	init {
		// Point lights.
		pointShadowsFbo.begin()
		pointLightShadowMaps = Array(numShadowPointLights) {
			val sides = Array(6) { BufferTexture(gl, glState, pointShadowsResolution, pointShadowsResolution) }
			val cubeMap = CubeMap(sides[0], sides[1], sides[2], sides[3], sides[4], sides[5], gl, glState, writeMode = true)
			cubeMap.refInc()
			cubeMap
		}
		pointShadowsFbo.end()
	}

	//--------------------------------------------
	// Render steps
	//--------------------------------------------

	/**
	 * Render the directional and point light shadows to their shadow buffers.
	 */
	fun renderOcclusion(camera: CameraRo, directionalLight: DirectionalLight, pointLights: List<PointLight>, renderOcclusion: () -> Unit) {
		val currentW = window.width.toInt()
		val currentH = window.height.toInt()
		if (currentW == 0 || currentH == 0) return
		if (!allowShadows) return
		val previousShader = glState.shader

		val previousBlendingEnabled = glState.blendingEnabled
		val previousBatch = glState.batch
		glState.batch = shadowsBatch
		glState.blendingEnabled = false
		gl.enable(Gl20.DEPTH_TEST)
		gl.depthFunc(Gl20.LESS)

		directionalLightShadows(directionalShadowMapShader, camera, directionalLight, renderOcclusion)
		pointLightShadows(pointShadowMapShader, pointLights, renderOcclusion)

		// Reset the gl properties
		gl.disable(Gl20.DEPTH_TEST)
		glState.blendingEnabled = previousBlendingEnabled
		glState.batch = previousBatch
		glState.shader = previousShader
		glState.batch.flush()
	}

	private fun directionalLightShadows(directionalShadowMapShader: ShaderProgram, camera: CameraRo, directionalLight: DirectionalLight, renderOcclusion: () -> Unit) {
		// Directional light shadows
		glState.shader = directionalShadowMapShader
		directionalShadowsFbo.begin()
		val oldClearColor = window.clearColor
		gl.clearColor(Color.BLUE) // Blue represents a z / w depth of 1.0. (The camera's far position)
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT)
		if (directionalLight.color != Color.BLACK) {
			glState.useScissor(1, 1, directionalShadowsFbo.width - 2, directionalShadowsFbo.height - 2) {
				if (directionalLightCamera.update(directionalLight.direction, camera)) {
					gl.uniformMatrix4fv(directionalShadowMapShader.getRequiredUniformLocation("u_directionalLightMvp"), false, directionalLightCamera.combined)
				}
				renderOcclusion()
			}
		}
		directionalShadowsFbo.end()
		gl.clearColor(oldClearColor)
	}


	private fun pointLightShadows(pointShadowMapShader: ShaderProgram, pointLights: List<PointLight>, renderOcclusion: () -> Unit) {
		glState.shader = pointShadowMapShader
		val u_pointLightMvp = pointShadowMapShader.getRequiredUniformLocation("u_pointLightMvp")
		val oldClearColor = window.clearColor
		gl.clearColor(Color.BLUE) // Blue represents a z / w depth of 1.0. (The camera's far position)
		pointShadowsFbo.begin()

		for (i in 0..minOf(numShadowPointLights - 1, pointLights.lastIndex)) {
			val pointLight = pointLights[i]
			val pointLightShadowMap = pointLightShadowMaps[i]
			glState.setTexture(pointLightShadowMap, pointShadowUnit + i)
			gl.uniform3f(pointShadowMapShader.getRequiredUniformLocation("u_lightPosition"), pointLight.position)
			gl.uniform1f(pointShadowMapShader.getRequiredUniformLocation("u_lightRadius"), pointLight.radius)

			if (pointLight.radius > 1f) {
				for (j in 0..5) {
					gl.framebufferTexture2D(Gl20.FRAMEBUFFER, Gl20.COLOR_ATTACHMENT0,
							Gl20.TEXTURE_CUBE_MAP_POSITIVE_X + j, pointLightShadowMap.textureHandle!!, 0)
					gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT)
					if (pointLight.shadowSidesEnabled[j]) {

						pointLightCamera.update(pointLight, j)
						gl.uniformMatrix4fv(u_pointLightMvp, false, pointLightCamera.camera.combined)
						renderOcclusion()

						glState.batch.flush()
					}
				}
			}
		}
		pointShadowsFbo.end()
		gl.clearColor(oldClearColor)
	}

	private val u_directionalLightMvp = Matrix4()

	/**
	 * Bind the shadow buffers and render the world.
	 */
	fun renderWorld(lightingShader: ShaderProgram, ambientLight: AmbientLight, directionalLight: DirectionalLight, pointLights: List<PointLight>, renderWorld: () -> Unit) {
		val previousShader = glState.shader
		glState.shader = lightingShader

		// Prepare uniforms.
		gl.uniform2f(lightingShader.getRequiredUniformLocation("u_resolutionInv"), 1.0f / directionalShadowsFbo.width.toFloat(), 1.0f / directionalShadowsFbo.height.toFloat())
		gl.uniform1i(lightingShader.getRequiredUniformLocation("u_directionalShadowMap"), directionalShadowUnit)
		for (i in 0..numShadowPointLights - 1) {
			gl.uniform1i(lightingShader.getRequiredUniformLocation("u_pointLightShadowMaps[$i]"), pointShadowUnit + i)
		}

		pointLightUniforms(lightingShader, pointLights)

		glState.setTexture(directionalShadowsFbo.texture, directionalShadowUnit)
		gl.uniformMatrix4fv(lightingShader.getRequiredUniformLocation("u_directionalLightMvp"), false, u_directionalLightMvp.set(bias).mul(directionalLightCamera.combined))
		lightingShader.getUniformLocation("u_shadowsEnabled")?.let {
			gl.uniform1i(it, if (allowShadows) 1 else 0)
		}
		gl.uniform4f(lightingShader.getRequiredUniformLocation("u_ambient"), ambientLight.color.r, ambientLight.color.g, ambientLight.color.b, ambientLight.color.a)
		gl.uniform4f(lightingShader.getRequiredUniformLocation("u_directional"), directionalLight.color.r, directionalLight.color.g, directionalLight.color.b, directionalLight.color.a)
		lightingShader.getUniformLocation("u_directionalLightDir")?.let {
			gl.uniform3f(it, directionalLight.direction)
		}
		glState.blendMode(BlendMode.NORMAL, premultipliedAlpha = false)

		val previousBatch = glState.batch
		glState.batch = lightingBatch
		renderWorld()
		glState.batch.flush()
		glState.batch = previousBatch
		glState.shader = previousShader
	}

	private fun pointLightUniforms(lightingShader: ShaderProgram, pointLights: List<PointLight>) {
		for (i in 0..numPointLights - 1) {
			val pointLight = if (i < pointLights.size) pointLights[i] else PointLight.EMPTY_POINT_LIGHT
			gl.uniform1f(lightingShader.getRequiredUniformLocation("u_pointLights[$i].radius"), pointLight.radius)
			gl.uniform3f(lightingShader.getRequiredUniformLocation("u_pointLights[$i].position"), pointLight.position)
			gl.uniform3f(lightingShader.getRequiredUniformLocation("u_pointLights[$i].color"), pointLight.color)
		}
	}

	override fun dispose() {
		// Dispose the point lights.
		for (i in 0..pointLightShadowMaps.lastIndex) {
			pointLightShadowMaps[i].refDec()
		}
		// Dispose the shadow buffers.
		pointShadowsFbo.dispose()
		directionalShadowsFbo.dispose()
	}

}
