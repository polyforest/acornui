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

package com.acornui.graphic

import com.acornui.asset.loadTexture
import com.acornui.async.async
import com.acornui.async.launch
import com.acornui.component.InteractivityMode
import com.acornui.component.UiComponentImpl
import com.acornui.di.Owned
import com.acornui.gl.core.*
import com.acornui.io.floatBuffer
import com.acornui.io.put
import com.acornui.math.Matrix4
import com.acornui.math.Vector3
import com.acornui.observe.ModTagWatch

class Skybox(owner: Owned, private val camera: CameraRo) : UiComponentImpl(owner) {

	private val shader = SkyboxShader(gl)

	/**
	 * Loads the skybox as images.
	 */
	fun load(positiveX: String,
			 negativeX: String,
			 positiveY: String,
			 negativeY: String,
			 positiveZ: String,
			 negativeZ: String) {
		launch {
			val pX = async { loadTexture(positiveX) }
			val nX = async { loadTexture(negativeX) }
			val pY = async { loadTexture(positiveY) }
			val nY = async { loadTexture(negativeY) }
			val pZ = async { loadTexture(positiveZ) }
			val nZ = async { loadTexture(negativeZ) }

			load(pX.await(), nX.await(), pY.await(), nY.await(), pZ.await(), nZ.await())
		}
	}

	private var cubeMap: CubeMap? = null

	fun load(positiveX: Texture,
			 negativeX: Texture,
			 positiveY: Texture,
			 negativeY: Texture,
			 positiveZ: Texture,
			 negativeZ: Texture) {


		// TODO: refDec
		val cubeMap = CubeMap(
				positiveX,
				negativeX,
				positiveY,
				negativeY,
				positiveZ,
				negativeZ,
				gl,
				glState
		)
		cubeMap.filterMin = TextureMinFilter.LINEAR_MIPMAP_LINEAR
		cubeMap.refInc()
		this.cubeMap = cubeMap
	}

	private val vertices = floatArrayOf(
			// positions
			-1f, 1f, -1f,
			-1f, -1f, -1f,
			1f, -1f, -1f,
			1f, -1f, -1f,
			1f, 1f, -1f,
			-1f, 1f, -1f,

			-1f, -1f, 1f,
			-1f, -1f, -1f,
			-1f, 1f, -1f,
			-1f, 1f, -1f,
			-1f, 1f, 1f,
			-1f, -1f, 1f,

			1f, -1f, -1f,
			1f, -1f, 1f,
			1f, 1f, 1f,
			1f, 1f, 1f,
			1f, 1f, -1f,
			1f, -1f, -1f,

			-1f, -1f, 1f,
			-1f, 1f, 1f,
			1f, 1f, 1f,
			1f, 1f, 1f,
			1f, -1f, 1f,
			-1f, -1f, 1f,

			-1f, 1f, -1f,
			1f, 1f, -1f,
			1f, 1f, 1f,
			1f, 1f, 1f,
			-1f, 1f, 1f,
			-1f, 1f, -1f,

			-1f, -1f, -1f,
			-1f, -1f, 1f,
			1f, -1f, -1f,
			1f, -1f, -1f,
			-1f, -1f, 1f,
			1f, -1f, 1f
	)

	private val vertexComponents = floatBuffer(vertices.size)

	init {
		vertexComponents.put(vertices)
		interactivityMode = InteractivityMode.NONE
	}

	private var vertexComponentsBuffer: GlBufferRef? = null

	override fun onActivated() {
		super.onActivated()

		vertexComponentsBuffer = gl.createBuffer()
		gl.bindBuffer(Gl20.ARRAY_BUFFER, vertexComponentsBuffer)
		vertexComponents.flip()
		gl.bufferData(Gl20.ARRAY_BUFFER, vertexComponents.limit shl 2, Gl20.DYNAMIC_DRAW) // Allocate
		gl.bufferDatafv(Gl20.ARRAY_BUFFER, vertexComponents, Gl20.DYNAMIC_DRAW) // Upload
	}

	override fun onDeactivated() {
		super.onDeactivated()
		gl.deleteBuffer(vertexComponentsBuffer!!)
		vertexComponentsBuffer = null
	}

	private val viewProjection = Matrix4()
	private val modTag = ModTagWatch()

	override fun draw() {
		// TODO: Should we account for this component's model transform?
		val cubeMap = cubeMap ?: return
		glState.setTexture(cubeMap)
		glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
		val previousShader = glState.shader
		glState.shader = shader

		if (modTag.set(camera.modTag)) {
			viewProjection.idt()
			viewProjection.setToLookAt(camera.direction, Vector3(camera.up.x, -camera.up.y, camera.up.z))
			val r = window.height / window.width
			if (r > 1f) viewProjection.scale(r, 1f, 1f) // Tall
			else viewProjection.scale(1f, 1f / r, 1f) // Wide
		}

		gl.enable(Gl20.CULL_FACE)
		gl.frontFace(Gl20.CW)
		gl.cullFace(Gl20.BACK)

		shader.uniforms.put(CommonShaderUniforms.U_PROJ_TRANS, viewProjection)

		gl.bindBuffer(Gl20.ARRAY_BUFFER, vertexComponentsBuffer)
		val attributeLocation = shader.getAttributeLocationByUsage(VertexAttributeUsage.POSITION)
		gl.enableVertexAttribArray(attributeLocation)
		gl.vertexAttribPointer(attributeLocation, 3, Gl20.FLOAT, false, stride = 3 * 4, offset = 0)
		gl.drawArrays(Gl20.TRIANGLES, 0, vertices.size / 3)
		glState.shader = previousShader
		gl.disableVertexAttribArray(attributeLocation)
		gl.bindBuffer(Gl20.ARRAY_BUFFER, null)

		gl.disable(Gl20.CULL_FACE)
	}
}

class SkyboxShader(gl: Gl20) : ShaderProgramBase(
		gl,
		vertexShaderSrc = """

$DEFAULT_SHADER_HEADER

attribute vec3 a_position;

varying vec3 v_texCoord;

uniform mat4 u_projTrans;

void main() {
    v_texCoord = a_position;
	vec4 p = u_projTrans * vec4(a_position, 1.0);
    gl_Position =  p.xyzz;
}

""",
		fragmentShaderSrc = """

$DEFAULT_SHADER_HEADER

varying vec3 v_texCoord;

uniform samplerCube u_texture;

void main() {
	gl_FragColor = textureCube(u_texture, vec3(-v_texCoord.x, v_texCoord.y, -v_texCoord.z));
}

""",
		vertexAttributes = hashMapOf(
				VertexAttributeUsage.POSITION to CommonShaderAttributes.A_POSITION
		)) {

	override fun bind() {
		super.bind()
		uniforms.put(CommonShaderUniforms.U_TEXTURE, 0)  // set the fragment shader's texture to unit 0
	}
}