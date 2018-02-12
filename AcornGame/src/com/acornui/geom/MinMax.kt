package com.acornui.geom

interface MinMaxRo {

	val xMin: Float
	val xMax: Float
	val yMin: Float
	val yMax: Float
	val width: Float
	val height: Float
	fun isEmpty(): Boolean
	fun isNotEmpty(): Boolean
	fun intersects(other: MinMaxRo): Boolean

}

data class MinMax(
		override var xMin: Float = Float.POSITIVE_INFINITY,
		override var xMax: Float = Float.NEGATIVE_INFINITY,
		override var yMin: Float = Float.POSITIVE_INFINITY,
		override var yMax: Float = Float.NEGATIVE_INFINITY
) : MinMaxRo {

	fun inf() {
		xMin = Float.POSITIVE_INFINITY
		xMax = Float.NEGATIVE_INFINITY
		yMin = Float.POSITIVE_INFINITY
		yMax = Float.NEGATIVE_INFINITY
	}

	fun ext(x: Float, y: Float) {
		if (x < xMin) xMin = x
		if (x > xMax) xMax = x
		if (y < yMin) yMin = y
		if (y > yMax) yMax = y
	}

	override fun isEmpty(): Boolean {
		return xMax <= xMin || yMax <= yMin
	}

	override fun isNotEmpty(): Boolean = !isEmpty()

	fun scl(x: Float, y: Float) {
		xMin *= x
		xMax *= x
		yMin *= y
		yMax *= y
	}

	fun inflate(left: Float, top: Float, right: Float, bottom: Float) {
		xMin -= left
		xMax += right
		yMin -= top
		yMax += bottom
	}

	override val width: Float
		get() = xMax - xMin

	override val height: Float
		get() = yMax - yMin

	fun set(other: MinMaxRo) {
		xMin = other.xMin
		xMax = other.xMax
		yMin = other.yMin
		yMax = other.yMax
	}

	override fun intersects(other: MinMaxRo): Boolean {
		return (xMax >= other.xMin && yMax >= other.yMin && xMin <= other.xMax && yMin <= other.yMax)
	}
}