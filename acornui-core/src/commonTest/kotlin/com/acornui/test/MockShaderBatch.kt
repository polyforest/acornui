package com.acornui.test

import com.acornui.gl.core.ShaderBatch
import com.acornui.gl.core.VertexAttributes

object MockShaderBatch : ShaderBatch {

	override fun resetRenderCount() {
	}

	override val renderCount: Int = 0

	override fun begin(drawMode: Int) {
	}

	override fun flush() {
	}

	override fun putVertex(positionX: Float, positionY: Float, positionZ: Float, normalX: Float, normalY: Float, normalZ: Float, colorR: Float, colorG: Float, colorB: Float, colorA: Float, u: Float, v: Float) {
	}

	override val vertexComponentsCount: Int = 0
	override val vertexAttributes: VertexAttributes = VertexAttributes(emptyList())

	override fun putVertexComponent(value: Float) {
	}

	override val indicesCount: Int = 0
	override val highestIndex: Short = 0

	override fun putIndex(index: Short) {
	}
}
