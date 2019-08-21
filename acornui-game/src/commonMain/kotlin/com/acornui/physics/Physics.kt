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

package com.acornui.physics
import com.acornui.ecs.ComponentBase
import com.acornui.ecs.ComponentType
import com.acornui.geom.Polygon2
import com.acornui.math.Vector2
import com.acornui.math.Vector3

class Physics : ComponentBase() {

	val position = Vector3()
	val velocity = Vector2()
	var maxVelocity = 20f
	val acceleration = Vector2()
	val scale = Vector3(1f, 1f, 1f)

	var rotation = 0f
	var rotationalVelocity = 0f
	var dampening = 1f
	var rotationalDampening = 0.95f

	/**
	 * The radius of the entity. This is used for early-out in collision detection.
	 * This should be before scaling.
	 */
	var radius = 0f
	var collisionZ = 0f

	var restitution = 0.8f

	var canCollide = true

	/**
	 * Two objects of the same collide group will not collide together.
	 * -1 for no collide group.
	 */
	var collideGroup = -1

	var mass = 1f

	/**
	 * If the object is fixed, it cannot be moved.
	 */
	var isFixed = false

	override val type = Physics

	companion object : ComponentType<Physics>
}

class Perimeter(
		val perimeter: Polygon2 = Polygon2()
) : ComponentBase() {

	override val type = Perimeter

	companion object : ComponentType<Perimeter>
}
