package com.acornui.graphic.lighting

import com.acornui.core.graphic.PerspectiveCamera
import com.acornui.core.graphic.Window
import com.acornui.math.MathUtils
import com.acornui.math.Vector3

/**
 * @author nbilyk
 */
class PointLightCamera(window: Window, resolution: Float) {

	val camera = PerspectiveCamera()

	init {
		camera.fieldOfView = 90f * MathUtils.degRad
		camera.viewportWidth = resolution
		camera.viewportHeight = resolution
	}

	fun update(pointLight: PointLight, direction: Int) {
		if (pointLight.radius < 0.0001f) return
		camera.setPosition(pointLight.position)
		camera.setDirection(CUBEMAP_DIRECTIONS[direction], keepUpOrthonormal = false)
		camera.setUp(CUBEMAP_UP[direction])
	}

	companion object {
		// positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ
		private val CUBEMAP_DIRECTIONS = arrayOf(Vector3(1f, 0f, 0f), Vector3(-1f, 0f, 0f), Vector3(0f, 1f, 0f), Vector3(0f, -1f, 0f), Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f))
		private val CUBEMAP_UP = arrayOf(Vector3(0f, -1f, 0f), Vector3(0f, -1f, 0f), Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f), Vector3(0f, -1f, 0f), Vector3(0f, -1f, 0f))
	}
}
