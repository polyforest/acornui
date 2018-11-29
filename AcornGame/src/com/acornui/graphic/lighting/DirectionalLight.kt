package com.acornui.graphic.lighting

import com.acornui.graphic.Color
import com.acornui.math.Vector3


/**
 * A light of infinite distance. Good for simulating sunlight.

 * @author nbilyk
 */
class DirectionalLight {

	val color = Color()
	val direction = Vector3(0f, 0f, -1f)
}

fun directionalLight(init: DirectionalLight.() -> Unit = {}): DirectionalLight {
	val d = DirectionalLight()
	d.init()
	d.direction.nor()
	return d
}