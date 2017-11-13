package com.acornui.particle


import com.acornui.collection.arrayCopy
import com.acornui.core.Disposable
import com.acornui.graphics.Color
import com.acornui.math.Box
import com.acornui.math.MathUtils
import com.acornui.math.Rectangle

class ParticleEmitter : Disposable {

	var spriteWidth = 1f
	var spriteHeight = 1f

	val delayValue = RangedNumericValue()
	val lifeOffsetValue = ScaledNumericValue()
	val durationValue = RangedNumericValue()
	val lifeValue = ScaledNumericValue()
	val emissionValue = ScaledNumericValue()

	val scale = ScaledNumericValue()
	val rotation = ScaledNumericValue()
	val velocity = ScaledNumericValue()
	val angle = ScaledNumericValue()
	val wind = ScaledNumericValue()
	val gravity = ScaledNumericValue()
	val transparency = ScaledNumericValue()
	val tint = GradientColorValue()
	val xOffsetValue = ScaledNumericValue()
	val yOffsetValue = ScaledNumericValue()

	val spawnWidthValue = ScaledNumericValue()
	val spawnHeightValue = ScaledNumericValue()

	val spawnShape = SpawnShapeValue()

	private var accumulator: Float = 0f

	var particles: Array<Particle?>? = null

	var minParticleCount: Int = 0

	private var _maxParticleCount = 0

	var maxParticleCount: Int
		get() = _maxParticleCount
		set(value) {
			if (_maxParticleCount == value) return
			_maxParticleCount = value
			active = BooleanArray(value)
			activeCount = 0
			particles = arrayOfNulls<Particle?>(value)
		}

	var x: Float = 0f
	var y: Float = 0f
	var z: Float = 0f
	var name: String? = null
	var imagePath: String? = null
	var activeCount: Int = 0

	var active: BooleanArray? = null

	private var firstUpdate: Boolean = false
	private var flipX: Boolean = false
	private var flipY: Boolean = false
	private var updateFlags: Int = 0
	private var allowCompletion: Boolean = false
	private var bounds: Box? = null

	private var emission: Int = 0
	private var emissionDiff: Int = 0
	private var emissionDelta: Int = 0
	private var lifeOffset: Int = 0
	private var lifeOffsetDiff: Int = 0
	private var life: Int = 0
	private var lifeDiff: Int = 0
	private var spawnWidth: Float = 0f
	private var spawnWidthDiff: Float = 0f
	private var spawnHeight: Float = 0f
	private var spawnHeightDiff: Float = 0f
	var duration = 1f
	var durationTimer: Float = 0f
	private var delay: Float = 0f
	private var delayTimer: Float = 0f

	var isAttached: Boolean = false
	var isContinuous: Boolean = false
	var isAligned: Boolean = false
	var isBehind: Boolean = false
	var isAdditive = true
	var isPremultipliedAlpha = false

	fun set(emitter: ParticleEmitter): ParticleEmitter {
		name = emitter.name
		imagePath = emitter.imagePath
		maxParticleCount = emitter.maxParticleCount
		minParticleCount = emitter.minParticleCount
		delayValue.set(emitter.delayValue)
		durationValue.set(emitter.durationValue)
		emissionValue.set(emitter.emissionValue)
		lifeValue.set(emitter.lifeValue)
		lifeOffsetValue.set(emitter.lifeOffsetValue)
		scale.set(emitter.scale)
		rotation.set(emitter.rotation)
		velocity.set(emitter.velocity)
		angle.set(emitter.angle)
		wind.set(emitter.wind)
		gravity.set(emitter.gravity)
		transparency.set(emitter.transparency)
		tint.set(emitter.tint)
		xOffsetValue.set(emitter.xOffsetValue)
		yOffsetValue.set(emitter.yOffsetValue)
		spawnWidthValue.set(emitter.spawnWidthValue)
		spawnHeightValue.set(emitter.spawnHeightValue)
		spawnShape.set(emitter.spawnShape)
		isAttached = emitter.isAttached
		isContinuous = emitter.isContinuous
		isAligned = emitter.isAligned
		isBehind = emitter.isBehind
		isAdditive = emitter.isAdditive
		return this
	}

	fun addParticle() {
		val activeCount = this.activeCount
		if (activeCount == maxParticleCount) return
		val active = this.active ?: return

		for (i in 0..active.lastIndex) {
			if (!active[i]) {
				activateParticle(i)
				active[i] = true
				this.activeCount = activeCount + 1
				break
			}
		}
	}

	fun addParticles(count: Int) {
		var count = count
		count = minOf(count, maxParticleCount - activeCount)
		if (count == 0) return
		val active = this.active
		var index = 0
		val n = active!!.size
		outer@ for (i in 0..count - 1) {
			while (index < n) {
				if (!active[index]) {
					activateParticle(index)
					active[index++] = true
					continue@outer
				}
				index++
			}
			break
		}
		this.activeCount += count
	}

	fun update(dT: Float) {
		val active = active ?: return
		accumulator += dT * 1000
		if (accumulator < 1) return
		val deltaMillis = accumulator.toInt()
		accumulator -= deltaMillis.toFloat()

		if (delayTimer < delay) {
			delayTimer += deltaMillis.toFloat()
		} else {
			var done = false
			if (firstUpdate) {
				firstUpdate = false
				addParticle()
			}

			if (durationTimer < duration)
				durationTimer += deltaMillis.toFloat()
			else {
				if (!isContinuous || allowCompletion)
					done = true
				else
					restart()
			}

			if (!done) {
				emissionDelta += deltaMillis
				var emissionTime = emission + emissionDiff * emissionValue.getScale(durationTimer / duration)
				if (emissionTime > 0) {
					emissionTime = 1000 / emissionTime
					if (emissionDelta >= emissionTime) {
						var emitCount = (emissionDelta / emissionTime).toInt()
						emitCount = minOf(emitCount, maxParticleCount - activeCount)
						emissionDelta -= (emitCount * emissionTime).toInt()
						emissionDelta %= emissionTime.toInt()
						addParticles(emitCount)
					}
				}
				if (activeCount < minParticleCount) addParticles(minParticleCount - activeCount)
			}
		}

		val particles = this.particles!!
		for (i in 0..active.lastIndex) {
			if (!active[i]) continue
			if (!updateParticle(particles[i]!!, dT, deltaMillis)) {
				active[i] = false
				activeCount--
			}
		}
	}

	fun start() {
		firstUpdate = true
		allowCompletion = false
		restart()
	}

	fun reset() {
		emissionDelta = 0
		durationTimer = duration
		val active = this.active!!
		for (i in 0..active.lastIndex) {
			active[i] = false
		}
		activeCount = 0
		start()
	}

	private fun restart() {
		delay = if (delayValue.isActive) delayValue.newLowValue() else 0f
		delayTimer = 0f

		durationTimer -= duration
		duration = durationValue.newLowValue()

		emission = emissionValue.newLowValue().toInt()
		emissionDiff = emissionValue.newHighValue().toInt()
		if (!emissionValue.isRelative) emissionDiff -= emission

		life = lifeValue.newLowValue().toInt()
		lifeDiff = lifeValue.newHighValue().toInt()
		if (!lifeValue.isRelative) lifeDiff -= life

		lifeOffset = if (lifeOffsetValue.isActive) lifeOffsetValue.newLowValue().toInt() else 0
		lifeOffsetDiff = lifeOffsetValue.newHighValue().toInt()
		if (!lifeOffsetValue.isRelative) lifeOffsetDiff -= lifeOffset

		spawnWidth = spawnWidthValue.newLowValue()
		spawnWidthDiff = spawnWidthValue.newHighValue()
		if (!spawnWidthValue.isRelative) spawnWidthDiff -= spawnWidth

		spawnHeight = spawnHeightValue.newLowValue()
		spawnHeightDiff = spawnHeightValue.newHighValue()
		if (!spawnHeightValue.isRelative) spawnHeightDiff -= spawnHeight

		updateFlags = 0
		if (angle.isActive && angle.timeline.size > 1) updateFlags = updateFlags or UPDATE_ANGLE
		if (velocity.isActive) updateFlags = updateFlags or UPDATE_VELOCITY
		if (scale.timeline.size > 1) updateFlags = updateFlags or UPDATE_SCALE
		if (rotation.isActive && rotation.timeline.size > 1) updateFlags = updateFlags or UPDATE_ROTATION
		if (wind.isActive) updateFlags = updateFlags or UPDATE_WIND
		if (gravity.isActive) updateFlags = updateFlags or UPDATE_GRAVITY
		if (tint.timeline.size > 1) updateFlags = updateFlags or UPDATE_TINT
	}

	private fun activateParticle(index: Int) {
		val particles = particles!!
		var particle: Particle? = particles[index]
		if (particle == null) {
			particle = Particle()
			particles[index] = particle
			particle.setFlip(flipX, flipY)
		}

		val percent = durationTimer / duration
		val updateFlags = this.updateFlags

		particle.life = life + (lifeDiff * lifeValue.getScale(percent)).toInt()
		particle.currentLife = particle.life

		if (velocity.isActive) {
			particle.velocity = velocity.newLowValue()
			particle.velocityDiff = velocity.newHighValue()
			if (!velocity.isRelative) particle.velocityDiff -= particle.velocity
		}

		particle.angle = angle.newLowValue()
		particle.angleDiff = angle.newHighValue()
		if (!angle.isRelative) particle.angleDiff -= particle.angle
		var angle = 0f
		if (updateFlags and UPDATE_ANGLE == 0) {
			angle = particle.angle + particle.angleDiff * this.angle.getScale(0f)
			particle.angle = angle
			particle.angleCos = MathUtils.cos(angle * MathUtils.degRad)
			particle.angleSin = MathUtils.sin(angle * MathUtils.degRad)
		}

		particle.scale = scale.newLowValue() / spriteWidth
		particle.scaleDiff = scale.newHighValue() / spriteWidth
		if (!scale.isRelative) particle.scaleDiff -= particle.scale
		particle.scale = particle.scale + particle.scaleDiff * scale.getScale(0f)

		if (rotation.isActive) {
			particle.rotation = rotation.newLowValue()
			particle.rotationDiff = rotation.newHighValue()
			if (!rotation.isRelative) particle.rotationDiff -= particle.rotation
			var rotation = particle.rotation + particle.rotationDiff * this.rotation.getScale(0f)
			if (isAligned) rotation += angle
			particle.rotation = (rotation)
		}

		if (wind.isActive) {
			particle.wind = wind.newLowValue()
			particle.windDiff = wind.newHighValue()
			if (!wind.isRelative) particle.windDiff -= particle.wind
		}

		if (gravity.isActive) {
			particle.gravity = gravity.newLowValue()
			particle.gravityDiff = gravity.newHighValue()
			if (!gravity.isRelative) particle.gravityDiff -= particle.gravity
		}

		var color = particle.tint
		if (color == null) {
			color = FloatArray(3)
			particle.tint = color
		}
		val temp = tint.getColor(0f)
		color[0] = temp[0]
		color[1] = temp[1]
		color[2] = temp[2]

		particle.transparency = transparency.newLowValue()
		particle.transparencyDiff = transparency.newHighValue() - particle.transparency

		// Spawn.
		var x = this.x
		if (xOffsetValue.isActive) x += xOffsetValue.newLowValue()
		var y = this.y
		if (yOffsetValue.isActive) y += yOffsetValue.newLowValue()
		when (spawnShape.shape) {
			SpawnShape.square -> {
				val width = spawnWidth + spawnWidthDiff * spawnWidthValue.getScale(percent)
				val height = spawnHeight + spawnHeightDiff * spawnHeightValue.getScale(percent)
				x += MathUtils.random(width) - width / 2
				y += MathUtils.random(height) - height / 2
			}
			SpawnShape.ellipse -> {
				val width = spawnWidth + spawnWidthDiff * spawnWidthValue.getScale(percent)
				val height = spawnHeight + spawnHeightDiff * spawnHeightValue.getScale(percent)
				val radiusX = width / 2f
				val radiusY = height / 2f
				if (radiusX != 0f && radiusY != 0f) {
					val scaleY = radiusX / radiusY
					if (spawnShape.isEdges) {
						val spawnAngle = when (spawnShape.side) {
							SpawnEllipseSide.top -> -MathUtils.random(179f)
							SpawnEllipseSide.bottom -> MathUtils.random(179f)
							else -> MathUtils.random(360f)
						}
						val cosDeg = MathUtils.cos(spawnAngle * MathUtils.degRad)
						val sinDeg = MathUtils.sin(spawnAngle * MathUtils.degRad)
						x += cosDeg * radiusX
						y += sinDeg * radiusX / scaleY
						if (updateFlags and UPDATE_ANGLE == 0) {
							particle.angle = spawnAngle
							particle.angleCos = cosDeg
							particle.angleSin = sinDeg
						}
					} else {
						val radius2 = radiusX * radiusX
						while (true) {
							val px = MathUtils.random(width) - radiusX
							val py = MathUtils.random(width) - radiusX
							if (px * px + py * py <= radius2) {
								x += px
								y += py / scaleY
								break
							}
						}
					}
				}
			}
			SpawnShape.line -> {
				val width = spawnWidth + spawnWidthDiff * spawnWidthValue.getScale(percent)
				val height = spawnHeight + spawnHeightDiff * spawnHeightValue.getScale(percent)
				if (width != 0f) {
					val lineX = width * MathUtils.random()
					x += lineX
					y += lineX * (height / width.toFloat())
				} else
					y += height * MathUtils.random()
			}

			SpawnShape.point -> {
				// No-op
			}

		}

		particle.x = x
		particle.y = y
		particle.z = z

		var offsetTime = (lifeOffset + lifeOffsetDiff * lifeOffsetValue.getScale(percent)).toInt()
		if (offsetTime > 0) {
			if (offsetTime >= particle.currentLife) offsetTime = particle.currentLife - 1
			updateParticle(particle, offsetTime / 1000f, offsetTime)
		}
	}

	private fun updateParticle(particle: Particle, delta: Float, deltaMillis: Int): Boolean {
		val life = particle.currentLife - deltaMillis
		if (life <= 0) return false
		particle.currentLife = life

		val percent = 1 - particle.currentLife / particle.life.toFloat()
		val updateFlags = this.updateFlags

		if (updateFlags and UPDATE_SCALE != 0)
			particle.scale = (particle.scale + particle.scaleDiff * scale.getScale(percent))

		if (updateFlags and UPDATE_VELOCITY != 0) {
			val velocity = (particle.velocity + particle.velocityDiff * this.velocity.getScale(percent)) * delta

			var velocityX: Float
			var velocityY: Float
			if (updateFlags and UPDATE_ANGLE != 0) {
				val angle = particle.angle + particle.angleDiff * this.angle.getScale(percent)
				velocityX = velocity * MathUtils.cos(angle * MathUtils.degRad)
				velocityY = velocity * MathUtils.sin(angle * MathUtils.degRad)
				if (updateFlags and UPDATE_ROTATION != 0) {
					var rotation = particle.rotation + particle.rotationDiff * this.rotation.getScale(percent)
					if (isAligned) rotation += angle
					particle.rotation = rotation
				}
			} else {
				velocityX = velocity * particle.angleCos
				velocityY = velocity * particle.angleSin
				if (isAligned || updateFlags and UPDATE_ROTATION != 0) {
					var rotation = particle.rotation + particle.rotationDiff * this.rotation.getScale(percent)
					if (isAligned) rotation += particle.angle
					particle.rotation = rotation
				}
			}

			if (updateFlags and UPDATE_WIND != 0)
				velocityX += (particle.wind + particle.windDiff * wind.getScale(percent)) * delta

			if (updateFlags and UPDATE_GRAVITY != 0)
				velocityY += (particle.gravity + particle.gravityDiff * gravity.getScale(percent)) * delta

			particle.translate(velocityX, velocityY, 0f)
		} else {
			if (updateFlags and UPDATE_ROTATION != 0)
				particle.rotation = particle.rotation + particle.rotationDiff * rotation.getScale(percent)
		}

		val color: FloatArray
		if (updateFlags and UPDATE_TINT != 0)
			color = tint.getColor(percent)
		else
			color = particle.tint!!

		if (isPremultipliedAlpha) {
			val alphaMultiplier = if (isAdditive) 0f else 1f
			val a = particle.transparency + particle.transparencyDiff * transparency.getScale(percent)
			particle.setColor(color[0] * a, color[1] * a, color[2] * a, a * alphaMultiplier)
		} else {
			particle.setColor(color[0], color[1], color[2],
					particle.transparency + particle.transparencyDiff * transparency.getScale(percent))
		}
		return true
	}

	fun setPosition(x: Float, y: Float, z: Float = 0f) {
		val particles = particles!!
		if (isAttached) {
			val xD = x - this.x
			val yD = y - this.y
			val zD = z - this.z
			val active = this.active
			var i = 0
			val n = active!!.size
			while (i < n) {
				if (active[i]) particles[i]!!.translate(xD, yD, zD)
				i++
			}
		}
		this.x = x
		this.y = y
		this.z = z
	}

	/**
	 * Ignores the [isContinuous] setting until the emitter is started again. This allows the emitter
	 * to stop smoothly.
	 */
	fun allowCompletion() {
		allowCompletion = true
		durationTimer = duration
	}

	val isComplete: Boolean
		get() {
			if (delayTimer < delay) return false
			return durationTimer >= duration && activeCount == 0
		}

	val percentComplete: Float
		get() {
			if (delayTimer < delay) return 0f
			return minOf(1f, durationTimer / duration)
		}

	fun setFlip(flipX: Boolean, flipY: Boolean) {
		this.flipX = flipX
		this.flipY = flipY
		val particles = particles ?: return
		var i = 0
		val n = particles.size
		while (i < n) {
			val particle = particles[i]
			particle?.setFlip(flipX, flipY)
			i++
		}
	}

	fun flipY() {
		angle.setHigh(-angle.highMin, -angle.highMax)
		angle.setLow(-angle.lowMin, -angle.lowMax)

		gravity.setHigh(-gravity.highMin, -gravity.highMax)
		gravity.setLow(-gravity.lowMin, -gravity.lowMax)

		wind.setHigh(-wind.highMin, -wind.highMax)
		wind.setLow(-wind.lowMin, -wind.lowMax)

		rotation.setHigh(-rotation.highMin, -rotation.highMax)
		rotation.setLow(-rotation.lowMin, -rotation.lowMax)

		yOffsetValue.setLow(-yOffsetValue.lowMin, -yOffsetValue.lowMax)
	}

	/**
	 * Returns the bounding box for all active particles. z axis will always be zero.
	 */
	val boundingBox: Box
		get() {
			if (bounds == null) bounds = Box()

			val particles = this.particles!!
			val active = this.active!!
			val bounds = this.bounds!!

			bounds.inf()
			var i = 0
			val n = active.size
			while (i < n) {
				if (active[i]) {
					val r = particles[i]!!.bounds
					bounds.ext(r.x, r.y, 0f)
					bounds.ext(r.x + r.width, r.y + r.height, 0f)
				}
				i++
			}

			return bounds
		}


	override fun dispose() {
	}

	init {
		durationValue.isAlwaysActive = true
		emissionValue.isAlwaysActive = true
		lifeValue.isAlwaysActive = true
		scale.isAlwaysActive = true
		transparency.isAlwaysActive = true
		spawnShape.isAlwaysActive = true
		spawnWidthValue.isAlwaysActive = true
		spawnHeightValue.isAlwaysActive = true
	}

	companion object {
		private val UPDATE_SCALE = 1 shl 0
		private val UPDATE_ANGLE = 1 shl 1
		private val UPDATE_ROTATION = 1 shl 2
		private val UPDATE_VELOCITY = 1 shl 3
		private val UPDATE_WIND = 1 shl 4
		private val UPDATE_GRAVITY = 1 shl 5
		private val UPDATE_TINT = 1 shl 6
	}
}

open class Particle {

	var x: Float = 0f
	var y: Float = 0f
	var z: Float = 0f

	var life: Int = 0
	var currentLife: Int = 0
	var scale: Float = 0f
	var scaleDiff: Float = 0f
	var rotation: Float = 0f
	var rotationDiff: Float = 0f
	var velocity: Float = 0f
	var velocityDiff: Float = 0f
	var angle: Float = 0f
	var angleDiff: Float = 0f
	var angleCos: Float = 0f
	var angleSin: Float = 0f
	var transparency: Float = 0f
	var transparencyDiff: Float = 0f
	var wind: Float = 0f
	var windDiff: Float = 0f
	var gravity: Float = 0f
	var gravityDiff: Float = 0f
	var tint: FloatArray? = null

	val bounds: Rectangle = Rectangle()

	var flipX: Boolean = false
	var flipY: Boolean = false

	private val _colorTint = Color.WHITE.copy()
	val colorTint: Color
		get() = _colorTint

	fun setFlip(flipX: Boolean, flipY: Boolean) {
		this.flipX = flipX
		this.flipY = flipY
	}

	fun translate(xD: Float, yD: Float, zD: Float) {
		x += xD
		y += yD
		z += zD
	}

	fun setColor(r: Float, g: Float, b: Float, a: Float) {
		_colorTint.set(r, g, b, a)
	}
}

open class ParticleValue {

	private var _isActive: Boolean = false

	var isActive: Boolean
		get() = isAlwaysActive || _isActive
		set(value) {
			_isActive = value
		}

	var isAlwaysActive: Boolean = false


	open fun set(value: ParticleValue) {
		_isActive = value._isActive
		isAlwaysActive = value.isAlwaysActive
	}
}

class NumericValue : ParticleValue() {
	var value: Float = 0f


	fun set(value: NumericValue) {
		super.set(value)
		this.value = value.value
	}
}

open class RangedNumericValue : ParticleValue() {

	var lowMin: Float = 0f
	var lowMax: Float = 0f

	fun newLowValue(): Float {
		return lowMin + (lowMax - lowMin) * MathUtils.random()
	}

	fun setLow(min: Float, max: Float) {
		lowMin = min
		lowMax = max
	}

	fun set(value: RangedNumericValue) {
		super.set(value)
		lowMax = value.lowMax
		lowMin = value.lowMin
	}
}

class ScaledNumericValue : RangedNumericValue() {
	var scaling = floatArrayOf(1f)
	var timeline = floatArrayOf(0f)
	var highMin: Float = 0f
	var highMax: Float = 0f
	var isRelative: Boolean = false

	fun newHighValue(): Float {
		return highMin + (highMax - highMin) * MathUtils.random()
	}

	fun setHigh(min: Float, max: Float) {
		highMin = min
		highMax = max
	}

	fun getScale(percent: Float): Float {
		var endIndex = -1
		val timeline = this.timeline
		val n = timeline.size
		for (i in 1..n - 1) {
			val t = timeline[i]
			if (t > percent) {
				endIndex = i
				break
			}
		}
		if (endIndex == -1) return scaling[n - 1]
		val scaling = this.scaling
		val startIndex = endIndex - 1
		val startValue = scaling[startIndex]
		val startTime = timeline[startIndex]
		return startValue + (scaling[endIndex] - startValue) * ((percent - startTime) / (timeline[endIndex] - startTime))
	}

	fun set(value: ScaledNumericValue) {
		super.set(value)
		highMax = value.highMax
		highMin = value.highMin
		scaling = FloatArray(value.scaling.size)
		arrayCopy(value.scaling, 0, scaling, 0, scaling.size)
		timeline = FloatArray(value.timeline.size)
		arrayCopy(value.timeline, 0, timeline, 0, timeline.size)
		isRelative = value.isRelative
	}
}

class GradientColorValue : ParticleValue() {

	var colors = floatArrayOf(1f, 1f, 1f)
	var timeline = floatArrayOf(0f)

	init {
		isAlwaysActive = true
	}

	fun getColor(percent: Float): FloatArray {
		var startIndex = 0
		var endIndex = -1
		val timeline = this.timeline
		val n = timeline.size
		for (i in 1..n - 1) {
			val t = timeline[i]
			if (t > percent) {
				endIndex = i
				break
			}
			startIndex = i
		}
		val startTime = timeline[startIndex]
		startIndex *= 3
		val r1 = colors[startIndex]
		val g1 = colors[startIndex + 1]
		val b1 = colors[startIndex + 2]
		if (endIndex == -1) {
			temp[0] = r1
			temp[1] = g1
			temp[2] = b1
			return temp
		}
		val factor = (percent - startTime) / (timeline[endIndex] - startTime)
		endIndex *= 3
		temp[0] = r1 + (colors[endIndex] - r1) * factor
		temp[1] = g1 + (colors[endIndex + 1] - g1) * factor
		temp[2] = b1 + (colors[endIndex + 2] - b1) * factor
		return temp
	}

	fun set(value: GradientColorValue) {
		super.set(value)
		colors = FloatArray(value.colors.size)
		arrayCopy(value.colors, 0, colors, 0, colors.size)
		timeline = FloatArray(value.timeline.size)
		arrayCopy(value.timeline, 0, timeline, 0, timeline.size)
	}

	companion object {
		private val temp = FloatArray(4)
	}
}

class SpawnShapeValue : ParticleValue() {
	var shape = SpawnShape.point
	var isEdges: Boolean = false
	var side = SpawnEllipseSide.both

	fun set(value: SpawnShapeValue) {
		super.set(value)
		shape = value.shape
		isEdges = value.isEdges
		side = value.side
	}
}

enum class SpawnShape {
	point, line, square, ellipse
}

enum class SpawnEllipseSide {
	both, top, bottom
}
