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

package com.acornui.graphic.lighting

import com.acornui.graphic.Color
import com.acornui.math.vec3


/**
 * A light of infinite distance. Good for simulating sunlight.

 * @author nbilyk
 */
class DirectionalLight {

	val color = Color()
	val direction = vec3(0f, 0f, -1f)
}

fun directionalLight(init: DirectionalLight.() -> Unit = {}): DirectionalLight {
	val d = DirectionalLight()
	d.init()
	d.direction.nor()
	return d
}
