package com.acornui.graphic.lighting

import com.acornui.graphic.Color

/**
 * @author nbilyk
 */
class AmbientLight {

	val color: Color = Color.WHITE.copy()
}

fun ambientLight(init: AmbientLight.() -> Unit = {}): AmbientLight {
	val a = AmbientLight()
	a.init()
	return a
}