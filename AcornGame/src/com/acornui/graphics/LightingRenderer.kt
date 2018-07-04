package com.acornui.graphics

import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.CameraRo
import com.acornui.core.graphics.Window
import com.acornui.gl.core.*
import com.acornui.graphics.lighting.*
import com.acornui.math.Matrix4

/**
 * @author nbilyk
 */
class LightingRenderer(
		override val injector: Injector,
		val numPointLights: Int = 10,
		val numShadowPointLights: Int = 3,
		private val useModel: Boolean = true,

		// Final lighting shader
		private val lightingShader: ShaderProgram = LightingShader(injector.inject(Gl20), numPointLights, numShadowPointLights, useModel = useModel),
		private val directionalShadowMapShader: ShaderProgram = DirectionalShadowShader(injector.inject(Gl20), useModel = useModel),
		private val pointShadowMapShader: ShaderProgram = PointShadowShader(injector.inject(Gl20), useModel = useModel),

		directionalShadowsResolution: Int = 1024,
		pointShadowsResolution: Int = 1024
) : Scoped, Disposable {

	private val gl = inject(Gl20)
	private val glState = inject(GlState)
	private val window = inject(Window)

	private val directionalShadowsFbo = Framebuffer(injector, directionalShadowsResolution, directionalShadowsResolution, hasDepth = true)
	val directionalLightCamera = DirectionalLightCamera()

	// Point lights
	private val pointShadowUniforms = PointShadowUniforms(pointShadowMapShader)
	private val pointLightShadowMaps: Array<CubeMap>
	private val pointShadowsFbo = Framebuffer(injector, pointShadowsResolution, pointShadowsResolution, hasDepth = true)
	private val pointLightCamera = PointLightCamera(window, pointShadowsResolution.toFloat())

	private val lightingShaderUniforms = LightingShaderUniforms(lightingShader, numPointLights, numShadowPointLights)
	private val bias = Matrix4().apply {
		scl(0.5f, 0.5f, 0.5f)
		translate(1f, 1f, 1f)
	}

	var allowShadows: Boolean = true

	//--------------------------------------------
	// DrivableComponent methods
	//--------------------------------------------

	init {
		val previousShader = glState.shader

		// Point lights.
		pointShadowsFbo.begin()
		pointLightShadowMaps = Array(numShadowPointLights) {
			val sides = Array(6, { BufferTexture(gl, glState, pointShadowsResolution, pointShadowsResolution) })
			val cubeMap = CubeMap(sides[0], sides[1], sides[2], sides[3], sides[4], sides[5], gl, glState, writeMode = true)
			cubeMap.refInc()

			//gl.framebufferTexture2D(Gl20.FRAMEBUFFER, Gl20.COLOR_ATTACHMENT0, Gl20.TEXTURE_2D, cubeMap.textureHandle!!, 0)
			cubeMap
		}
		pointShadowsFbo.end()

		glState.shader = lightingShader
		gl.uniform2f(lightingShaderUniforms.u_resolutionInv, 1.0f / directionalShadowsResolution.toFloat(), 1.0f / directionalShadowsResolution.toFloat())

		// Poisson disk
		gl.uniform2f(lightingShader.getRequiredUniformLocation("poissonDisk[0]"), -0.94201624f, -0.39906216f)
		gl.uniform2f(lightingShader.getRequiredUniformLocation("poissonDisk[1]"), 0.94558609f, -0.76890725f)
		gl.uniform2f(lightingShader.getRequiredUniformLocation("poissonDisk[2]"), -0.09418410f, -0.92938870f)
		gl.uniform2f(lightingShader.getRequiredUniformLocation("poissonDisk[3]"), 0.34495938f, 0.29387760f)

		val modelUniform = glState.shader!!.getUniformLocation(ShaderProgram.U_MODEL_TRANS)
		if (modelUniform != null) gl.uniformMatrix4fv(modelUniform, false, Matrix4())

		gl.uniform2f(lightingShaderUniforms.u_resolutionInv, 1.0f / directionalShadowsResolution.toFloat(), 1.0f / directionalShadowsResolution.toFloat())
		gl.uniform1i(lightingShaderUniforms.u_directionalShadowMap, DIRECTIONAL_SHADOW_UNIT)
		for (i in 0..numShadowPointLights - 1) {
			gl.uniform1i(lightingShaderUniforms.u_pointLightShadowMaps[i], POINT_SHADOW_UNIT + i)
		}

		glState.shader = previousShader
	}

	fun render(camera: CameraRo, ambientLight: AmbientLight, directionalLight: DirectionalLight, pointLights: List<PointLight>, renderOcclusion: () -> Unit, renderWorld: () -> Unit) {
		val previousShader = glState.shader
		val currentW = window.width.toInt()
		val currentH = window.height.toInt()
		if (currentW == 0 || currentH == 0) return

		if (allowShadows) {
			renderOcclusion(camera, directionalLight, pointLights, renderOcclusion)
			glState.batch.flush(true)
		}
		prepareLightingShader(ambientLight, directionalLight, pointLights)

		renderWorld()
		glState.batch.flush(true)

		glState.shader = previousShader
	}

	//--------------------------------------------
	// Render steps
	//--------------------------------------------

	/**
	 * Step 3.
	 * Render the directional and point light shadows.
	 */
	private fun renderOcclusion(camera: CameraRo, directionalLight: DirectionalLight, pointLights: List<PointLight>, renderOcclusion: () -> Unit) {
		val previousBlendingEnabled = glState.blendingEnabled
		glState.batch.flush(true)
		glState.blendingEnabled = false
		gl.enable(Gl20.DEPTH_TEST)
		gl.depthFunc(Gl20.LESS)

		directionalLightShadows(camera, directionalLight, renderOcclusion)
		pointLightShadows(pointLights, renderOcclusion)

		// Reset the gl properties
		gl.disable(Gl20.DEPTH_TEST)
		glState.blendingEnabled = previousBlendingEnabled
	}

	private fun directionalLightShadows(camera: CameraRo, directionalLight: DirectionalLight, renderOcclusion: () -> Unit) {
		// Directional light shadows
		glState.shader = directionalShadowMapShader
		directionalShadowsFbo.begin()
		val oldClearColor = window.clearColor
		gl.clearColor(Color.BLUE) // Blue represents a z / w depth of 1.0. (The camera's far position)
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT)
		if (directionalLight.color != Color.BLACK) {
			glState.scissor(1, 1, directionalShadowsFbo.width - 2, directionalShadowsFbo.height - 2) {
				glState.batch.flush(true)
				if (directionalLightCamera.update(directionalLight.direction, camera)) {
					gl.uniformMatrix4fv(directionalShadowMapShader.getRequiredUniformLocation("u_directionalLightMvp"), false, directionalLightCamera.combined)
				}
				renderOcclusion()
			}
		}
		directionalShadowsFbo.end()
		gl.clearColor(oldClearColor)
	}


	private fun pointLightShadows(pointLights: List<PointLight>, renderOcclusion: () -> Unit) {
		glState.shader = pointShadowMapShader
		val u_pointLightMvp = pointShadowMapShader.getRequiredUniformLocation("u_pointLightMvp")
		val oldClearColor = window.clearColor
		gl.clearColor(Color.BLUE) // Blue represents a z / w depth of 1.0. (The camera's far position)
		pointShadowsFbo.begin()

		for (i in 0..minOf(numShadowPointLights - 1, pointLights.lastIndex)) {
			val pointLight = pointLights[i]
			val pointLightShadowMap = pointLightShadowMaps[i]
			glState.setTexture(pointLightShadowMap, POINT_SHADOW_UNIT + i)
			gl.uniform3f(pointShadowUniforms.u_lightPosition, pointLight.position)
			gl.uniform1f(pointShadowUniforms.u_lightRadius, pointLight.radius)

			if (pointLight.radius > 1f) {
				for (j in 0..5) {
					gl.framebufferTexture2D(Gl20.FRAMEBUFFER, Gl20.COLOR_ATTACHMENT0,
							Gl20.TEXTURE_CUBE_MAP_POSITIVE_X + j, pointLightShadowMap.textureHandle!!, 0)
					gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT)
					if (pointLight.shadowSidesEnabled[j]) {

						pointLightCamera.update(pointLight, j)
						gl.uniformMatrix4fv(u_pointLightMvp, false, pointLightCamera.camera.combined)
						renderOcclusion()

						glState.batch.flush(true)
					}
				}
			}
		}
		pointShadowsFbo.end()
		gl.clearColor(oldClearColor)
	}

	private val u_directionalLightMvp = Matrix4()

	/**
	 * Step 4.
	 * Set the light shader's uniforms.
	 */
	private fun prepareLightingShader(ambientLight: AmbientLight, directionalLight: DirectionalLight, pointLights: List<PointLight>) {
		glState.shader = lightingShader

		pointLightProperties(pointLights)

		glState.setTexture(directionalShadowsFbo.texture, DIRECTIONAL_SHADOW_UNIT)
		gl.uniformMatrix4fv(lightingShaderUniforms.u_directionalLightMvp, false, u_directionalLightMvp.set(bias).mul(directionalLightCamera.combined))
		if (lightingShaderUniforms.u_shadowsEnabled != null)
			gl.uniform1i(lightingShaderUniforms.u_shadowsEnabled, if (allowShadows) 1 else 0)
		gl.uniform4f(lightingShaderUniforms.u_ambient, ambientLight.color.r, ambientLight.color.g, ambientLight.color.b, ambientLight.color.a)
		gl.uniform4f(lightingShaderUniforms.u_directional, directionalLight.color.r, directionalLight.color.g, directionalLight.color.b, directionalLight.color.a)
		if (lightingShaderUniforms.u_directionalLightDir != null)
			gl.uniform3f(lightingShaderUniforms.u_directionalLightDir, directionalLight.direction)
		glState.blendMode(BlendMode.NORMAL, premultipliedAlpha = false)
	}

	private fun pointLightProperties(pointLights: List<PointLight>) {
		for (i in 0..numPointLights - 1) {
			val pointLight = if (i < pointLights.size) pointLights[i] else PointLight.EMPTY_POINT_LIGHT
			val pLU = lightingShaderUniforms.u_pointLights[i]
			gl.uniform1f(pLU.radius, pointLight.radius)
			gl.uniform3f(pLU.position, pointLight.position)
			gl.uniform3f(pLU.color, pointLight.color)
		}
	}

	override fun dispose() {
		// Dispose the point lights.
		for (i in 0..pointLightShadowMaps.lastIndex) {
			pointLightShadowMaps[i].refDec()
		}
		pointShadowsFbo.dispose()
		pointShadowMapShader.dispose()

		// Dispose the directional light.
		directionalShadowsFbo.dispose()
		directionalShadowMapShader.dispose()

		// Dispose the lighting shader.
		lightingShader.dispose()
	}

	companion object {

		private const val DIRECTIONAL_SHADOW_UNIT = 1
		private const val POINT_SHADOW_UNIT = 2
	}
}

private class LightingShaderUniforms(
		shader: ShaderProgram,
		numPointLights: Int,
		numShadowPointLights: Int) {
	val u_directionalLightMvp: GlUniformLocationRef
	val u_shadowsEnabled: GlUniformLocationRef?
	val u_resolutionInv: GlUniformLocationRef

	val u_ambient: GlUniformLocationRef

	val u_directional: GlUniformLocationRef
	val u_directionalLightDir: GlUniformLocationRef?
	val u_directionalShadowMap: GlUniformLocationRef

	val u_pointLights: Array<PointLightUniforms>
	val u_pointLightShadowMaps: Array<GlUniformLocationRef>

	init {
		u_directionalLightMvp = shader.getRequiredUniformLocation("u_directionalLightMvp")
		u_shadowsEnabled = shader.getUniformLocation("u_shadowsEnabled")
		u_resolutionInv = shader.getRequiredUniformLocation("u_resolutionInv")

		u_ambient = shader.getRequiredUniformLocation("u_ambient")

		u_directional = shader.getRequiredUniformLocation("u_directional")
		u_directionalLightDir = shader.getUniformLocation("u_directionalLightDir")
		u_directionalShadowMap = shader.getRequiredUniformLocation("u_directionalShadowMap")

		u_pointLights = Array(numPointLights, {
			PointLightUniforms(
					shader.getRequiredUniformLocation("u_pointLights[$it].radius"),
					shader.getRequiredUniformLocation("u_pointLights[$it].position"),
					shader.getRequiredUniformLocation("u_pointLights[$it].color")
			)
		})
		u_pointLightShadowMaps = Array(numShadowPointLights, {
			shader.getRequiredUniformLocation("u_pointLightShadowMaps[$it]")
		})
	}
}

private class PointLightUniforms(
		val radius: GlUniformLocationRef,
		val position: GlUniformLocationRef,
		val color: GlUniformLocationRef)


private class PointShadowUniforms(shader: ShaderProgram) {

	val u_lightPosition: GlUniformLocationRef
	val u_lightRadius: GlUniformLocationRef

	init {
		this.u_lightPosition = shader.getRequiredUniformLocation("u_lightPosition")
		this.u_lightRadius = shader.getRequiredUniformLocation("u_lightRadius")
	}
}