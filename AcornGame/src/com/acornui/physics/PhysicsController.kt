package com.acornui.physics

import com.acornui.core.Disposable
import com.acornui.core.Updatable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.mvc.Command
import com.acornui.core.mvc.CommandType
import com.acornui.core.mvc.commander
import com.acornui.core.mvc.invokeCommand
import com.acornui.ecs.Entity
import com.acornui.ecs.componentList
import com.acornui.geom.CollisionInfo
import com.acornui.geom.CollisionInfoRo
import com.acornui.geom.Polygon2
import com.acornui.math.Matrix3
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro

class PhysicsController(
		override val injector: Injector,
		entities: List<Entity>
) : Updatable, Scoped, Disposable {

	private val cmd = commander()
	private val physicsVos = ArrayList<Physics>()

	init {
		componentList(entities, cmd, physicsVos, Physics)
	}

	override fun update(tickTime: Float) {
		for (i in 0..physicsVos.lastIndex) {
			val p = physicsVos[i]
			p.position.add(p.velocity.x, p.velocity.y, 0f)
			p.velocity.add(p.acceleration)
			p.velocity.scl(p.dampening)
			p.velocity.limit(p.maxVelocity)
			p.rotation += p.rotationalVelocity
			p.rotationalVelocity *= p.rotationalDampening
		}

		for (i in 0..physicsVos.lastIndex) {
			val pA = physicsVos[i]
			for (j in (i + 1)..physicsVos.lastIndex) {
				val pB = physicsVos[j]
				checkCollision(pA, pB)
			}
		}
	}

	/**
	 * Checks if the two entities are colliding, and if they are, resolves their collision.
	 */
	private fun checkCollision(pA: Physics, pB: Physics) {
		if (!pA.canCollide || !pB.canCollide || (pA.isFixed && pB.isFixed) || (pA.collideGroup != -1 && pA.collideGroup == pB.collideGroup)) return
		val impactSpeed = collision.impactSpeed
		posDelta.set(pA.position.x, pA.position.y).sub(pB.position.x, pB.position.y)
		val dist2 = posDelta.len2()
		val r2 = pA.radius * maxOf(pA.scale.x, pA.scale.y) + pB.radius * maxOf(pB.scale.x, pB.scale.y) // The combined radii
		if (dist2 < r2 * r2 - 0.0001f) {
			tmpMat.idt()
			tmpMat.trn(pA.position)
			tmpMat.scl(pA.scale)
			tmpMat.rotate(pA.rotation)
			val perimA = pA.getSibling(Perimeter)!!.perimeter
			worldPerim1.set(perimA).mul(tmpMat).invalidate()

			tmpMat.idt()
			tmpMat.trn(pB.position)
			tmpMat.scl(pB.scale)
			tmpMat.rotate(pB.rotation)
			val perimB = pB.getSibling(Perimeter)!!.perimeter
			worldPerim2.set(perimB).mul(tmpMat).invalidate()

			if (worldPerim2.intersects(worldPerim1, mTd) && !mTd.isZero()) {
				worldPerim2.getContactInfo(worldPerim1, mTd, collisionInfo)

				val iMA = 1f / pA.mass
				val iMB = 1f / pB.mass
				val mA = if (pB.isFixed) 1f else iMA / (iMA + iMB)
				val mB = if (pA.isFixed) 1f else iMB / (iMA + iMB)

				// Resolve the collision by pushing away by the the minimum translation distance scaling by mass ratio.
				if (!pA.isFixed) pA.position.add(mTd.x * mA, mTd.y * mA, 0f)
				if (!pB.isFixed) pB.position.sub(mTd.x * mB, mTd.y * mB, 0f)

				mTd.nor()

				val cA = collisionInfo.midA
				val cB = collisionInfo.midB
				rA.set(cA).sub(pA.position.x, pA.position.y)
				rB.set(cB).sub(pB.position.x, pB.position.y)

				impactSpeed.set(pA.velocity).add(-rA.y * pA.rotationalVelocity, rA.x * pA.rotationalVelocity).sub(pB.velocity).sub(-rB.y * pB.rotationalVelocity, rB.x * pB.rotationalVelocity)

				val vN = impactSpeed.dot(mTd)
				if (vN < 0.0f) {
					val restitution = pA.restitution * pB.restitution
					// Intersecting and moving toward each other.
					impulse.set(mTd).scl(-(1.0f + restitution) / (iMA + iMB))
					if (!pA.isFixed) pA.velocity.add(impulse.x * iMA * vN, impulse.y * iMA * vN)
					if (!pB.isFixed) pB.velocity.sub(impulse.x * iMB * vN, impulse.y * iMB * vN)
					if (!pA.isFixed) pA.rotationalVelocity -= (rA.x * mTd.y * vN - rA.y * mTd.x * vN) * iMA
					if (!pB.isFixed) pB.rotationalVelocity += (rB.x * mTd.y * vN - rB.y * mTd.x * vN) * iMB

					collision.z = (pA.collisionZ + pB.collisionZ) * 0.5f
					collision.impactDirection.set(mTd)
					collision.impactStrength = vN
					collision.entityA = pA.parentEntity
					collision.entityB = pB.parentEntity
					invokeCommand(collision)
				}
			}
		}
	}

	override fun dispose() {
		cmd.dispose()
	}

	companion object {
		private val collisionInfo = CollisionInfo()
		private val collision = Collision(collisionInfo)

		private val mTd = Vector2()
		private val posDelta = Vector2()
		private val rA = Vector2()
		private val rB = Vector2()
		private val impulse = Vector2()

		private val worldPerim1 = Polygon2()
		private val worldPerim2 = Polygon2()
		private val tmpMat = Matrix3()
	}
}

interface CollisionRo : Command {

	val collisionInfo: CollisionInfoRo

	val z: Float
	val impactSpeed: Vector2Ro
	val impactDirection: Vector2Ro
	val impactStrength: Float

	val entityA: Entity?
	val entityB: Entity?

	companion object : CommandType<CollisionRo>
}

class Collision(
		override val collisionInfo: CollisionInfo
) : CollisionRo {

	override val type = CollisionRo

	override var z = 0f
	override val impactSpeed = Vector2()
	override val impactDirection = Vector2()
	override var impactStrength = 0f

	override var entityA: Entity? = null
	override var entityB: Entity? = null
}