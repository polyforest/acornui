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

import com.acornui.Disposable
import com.acornui.di.Injector
import com.acornui.di.Scoped
import com.acornui.di.inject
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
		private val directionalShadowMapShader: ShaderProgram = DirectionalShadowShader(injector.inject(CachedGl20)),
		private val pointShadowMapShader: ShaderProgram = PointShadowShader(injector.inject(CachedGl20)),

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

	private val gl = inject(CachedGl20)
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

	private val shadowsBatch = shaderBatch()
	private val lightingBatch = shaderBatch()

	//--------------------------------------------
	// DrivableComponent methods
	//--------------------------------------------

	init {
		// Point lights.
		pointShadowsFbo.begin()
		pointLightShadowMaps = Array(numShadowPointLights) {
			val sides = Array(6) { BufferTexture(gl, pointShadowsResolution, pointShadowsResolution) }
			val cubeMap = CubeMap(sides[0], sides[1], sides[2], sides[3], sides[4], sides[5], gl, writeMode = true)
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
		if (!allowShadows) return
		val previousProgram = gl.program
		val previousBlendingEnabled = gl.isEnabled(Gl20.BLEND)
		val previousBatch = gl.batch
		gl.batch = shadowsBatch
		gl.disable(Gl20.BLEND)
		gl.enable(Gl20.DEPTH_TEST)
		gl.depthFunc(Gl20.LESS)

		directionalLightShadows(directionalShadowMapShader, camera, directionalLight, renderOcclusion)
		pointLightShadows(pointShadowMapShader, camera, pointLights, renderOcclusion)

		// Reset the gl properties
		gl.disable(Gl20.DEPTH_TEST)
		if (previousBlendingEnabled) gl.enable(Gl20.BLEND)
		gl.batch = previousBatch
		gl.useProgram(previousProgram)
		gl.batch.flush()
	}

	private fun directionalLightShadows(directionalShadowMapShader: ShaderProgram, camera: CameraRo, directionalLight: DirectionalLight, renderOcclusion: () -> Unit) {
		// Directional light shadows
		gl.useProgram(directionalShadowMapShader.program)
		gl.uniforms.useCamera(camera) {
			directionalShadowsFbo.begin()
			gl.clearAndReset(Color.BLUE, Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT)  // Blue represents a z / w depth of 1.0. (The camera's far position)
			if (directionalLight.color != Color.BLACK) {
				gl.useScissor(1, 1, directionalShadowsFbo.widthPixels - 2, directionalShadowsFbo.heightPixels - 2) {
					if (directionalLightCamera.update(directionalLight.direction, camera)) {
						gl.uniforms.put("u_directionalLightMvp", directionalLightCamera.combined)
					}
					renderOcclusion()
				}
			}
			directionalShadowsFbo.end()
		}
	}


	private fun pointLightShadows(pointShadowMapShader: ShaderProgram, camera: CameraRo, pointLights: List<PointLight>, renderOcclusion: () -> Unit) {
		gl.useProgram(pointShadowMapShader.program)
		val uniforms = gl.uniforms
		uniforms.useCamera(camera) {
			val u_pointLightMvp = uniforms.getRequiredUniformLocation("u_pointLightMvp")
			pointShadowsFbo.begin()
			gl.clearAndReset(Color.BLUE, Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT)  // Blue represents a z / w depth of 1.0. (The camera's far position)

			for (i in 0..minOf(numShadowPointLights - 1, pointLights.lastIndex)) {
				val pointLight = pointLights[i]
				val pointLightShadowMap = pointLightShadowMaps[i]
				gl.bindTexture(pointLightShadowMap, pointShadowUnit + i)
				uniforms.put("u_lightPosition", pointLight.position)
				uniforms.put("u_lightRadius", pointLight.radius)

				if (pointLight.radius > 1f) {
					for (j in 0..5) {
						gl.framebufferTexture2D(Gl20.FRAMEBUFFER, Gl20.COLOR_ATTACHMENT0,
								Gl20.TEXTURE_CUBE_MAP_POSITIVE_X + j, pointLightShadowMap.textureHandle!!, 0)
						gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT)
						if (pointLight.shadowSidesEnabled[j]) {

							pointLightCamera.update(pointLight, j)
							gl.batch.flush()
							uniforms.put(u_pointLightMvp, pointLightCamera.camera.combined)
							renderOcclusion()

							gl.batch.flush()
						}
					}
				}
			}
			pointShadowsFbo.end()
		}
	}

	private val u_directionalLightMvp = Matrix4()

	/**
	 * Bind the shadow buffers and render the world.
	 */
	fun renderWorld(camera: CameraRo, lightingShader: ShaderProgram, ambientLight: AmbientLight, directionalLight: DirectionalLight, pointLights: List<PointLight>, renderWorld: () -> Unit) {
		val previousProgram = gl.program
		gl.useProgram(lightingShader.program)
		gl.uniforms.apply {
			// Prepare uniforms.
			put("u_resolutionInv", 1.0f / directionalShadowsFbo.widthPixels.toFloat(), 1.0f / directionalShadowsFbo.heightPixels.toFloat())
			put("u_directionalShadowMap", directionalShadowUnit)
			for (i in 0..numShadowPointLights - 1) {
				put("u_pointLightShadowMaps[$i]", pointShadowUnit + i)
			}

			pointLightUniforms(pointLights)
			gl.bindTexture(directionalShadowsFbo.texture, directionalShadowUnit)
			put("u_directionalLightMvp", u_directionalLightMvp.set(bias).mul(directionalLightCamera.combined))

			getUniformLocation("u_shadowsEnabled")?.let {
				put(it, if (allowShadows) 1 else 0)
			}
			put("u_ambient", ambientLight.color.r, ambientLight.color.g, ambientLight.color.b, ambientLight.color.a)
			put("u_directional", directionalLight.color.r, directionalLight.color.g, directionalLight.color.b, directionalLight.color.a)
			getUniformLocation("u_directionalLightDir")?.let {
				put(it, directionalLight.direction)
			}

			BlendMode.NORMAL.applyBlending(gl)
			val previousBatch = gl.batch
			gl.batch = lightingBatch
			useCamera(camera) {
				renderWorld()
			}
			gl.batch = previousBatch
			gl.useProgram(previousProgram)
		}
	}

	private fun Uniforms.pointLightUniforms(pointLights: List<PointLight>) {
		for (i in 0..numPointLights - 1) {
			val pointLight = if (i < pointLights.size) pointLights[i] else PointLight.EMPTY_POINT_LIGHT
			put("u_pointLights[$i].radius", pointLight.radius)
			put("u_pointLights[$i].position", pointLight.position)
			putRgb("u_pointLights[$i].color", pointLight.color)
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
