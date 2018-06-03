package com.acornui.physics
import com.acornui.ecs.ComponentBase
import com.acornui.ecs.SerializableComponentType
import com.acornui.geom.Polygon2
import com.acornui.math.Vector2
import com.acornui.math.Vector3
import com.acornui.math.vector2
import com.acornui.math.vector3
import com.acornui.serialization.*

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

	companion object : SerializableComponentType<Physics> {

		override val name: String = "Physics"

		override fun read(reader: Reader): Physics {
			val o = Physics()
			o.acceleration.set(reader.vector2("acceleration")!!)
			o.position.set(reader.vector3("position")!!)
			o.rotation = reader.float("rotation")!!
			o.rotationalVelocity = reader.float("rotationalVelocity")!!
			o.rotationalDampening = reader.float("rotationalDampening")!!
			o.velocity.set(reader.vector2("velocity")!!)
			o.maxVelocity = reader.float("maxVelocity")!!
			o.dampening = reader.float("dampening")!!
			o.radius = reader.float("radius")!!
			o.collisionZ = reader.float("collisionZ")!!
			o.canCollide = reader.bool("canCollide")!!
			o.mass = reader.float("mass")!!
			return o
		}

		override fun Physics.write(writer: Writer) {
			writer.vector2("acceleration", acceleration)
			writer.vector3("position", position)
			writer.float("rotation", rotation)
			writer.float("rotationalVelocity", rotationalVelocity)
			writer.float("rotationalDampening", rotationalDampening)
			writer.vector2("velocity", velocity)
			writer.float("maxVelocity", maxVelocity)
			writer.float("dampening", dampening)
			writer.float("radius", radius)
			writer.float("collisionZ", collisionZ)
			writer.bool("canCollide", canCollide)
			writer.float("mass", mass)
		}
	}
}

class Perimeter(
		val perimeter: Polygon2 = Polygon2()
) : ComponentBase() {

	override val type = Perimeter

	companion object : SerializableComponentType<Perimeter> {
		override val name: String = "Perimeter"

		override fun read(reader: Reader): Perimeter {
			val o = Perimeter(
					perimeter = reader.obj("perimeter", Polygon2)!!
			)
			return o
		}

		override fun Perimeter.write(writer: Writer) {
			writer.obj("perimeter", perimeter, Polygon2)
		}
	}
}