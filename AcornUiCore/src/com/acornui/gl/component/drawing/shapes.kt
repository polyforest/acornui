package com.acornui.gl.component.drawing

import com.acornui._assert
import com.acornui.math.*

private val v1 = Vector2()
private val v2 = Vector2()
private val v3 = Vector2()
private val v4 = Vector2()

val QUAD_INDICES = intArrayOf(0, 1, 2, 2, 3, 0)
val TRIANGLE_INDICES = intArrayOf(0, 1, 2)

fun fillTriangle(v1: Vector2, v2: Vector2, v3: Vector2): MeshData {
	val primitive = meshData()
	primitive.texture = fillStyle.texture
	primitive.pushVertex(v1, fillStyle)
	primitive.pushVertex(v2, fillStyle)
	primitive.pushVertex(v3, fillStyle)
	primitive.pushIndices(TRIANGLE_INDICES)
	return primitive
}

/**
 * Fills a quadrilateral with the specified fill style.
 */
fun fillQuad(v1: Vector2, v2: Vector2, v3: Vector2, v4: Vector2): MeshData {
	val primitive = meshData()
	primitive.texture = fillStyle.texture
	primitive.pushVertex(v1, fillStyle)
	primitive.pushVertex(v2, fillStyle)
	primitive.pushVertex(v3, fillStyle)
	primitive.pushVertex(v4, fillStyle)
	primitive.pushIndices(QUAD_INDICES)
	return primitive
}

fun line(x1: Float, y1: Float, x2: Float, y2: Float, controlA: Vector2? = null, controlB: Vector2? = null, controlAThickness: Float = lineStyle.thickness, controlBThickness: Float = lineStyle.thickness, init: MeshData.() -> Unit = {}): MeshData {
	val p1 = Vector2.obtain().set(x1, y1)
	val p2 = Vector2.obtain().set(x2, y2)
	val ret = line(p1, p2, controlA, controlB, controlAThickness, controlBThickness, init)
	Vector2.free(p1)
	Vector2.free(p2)
	return ret
}

fun line(p1: Vector2, p2: Vector2, controlA: Vector2? = null, controlB: Vector2? = null, controlAThickness: Float = lineStyle.thickness, controlBThickness: Float = lineStyle.thickness, init: MeshData.() -> Unit = {}): MeshData {
	val primitive = meshData()
	primitive.texture = lineStyle.fillStyle.texture
	val capBuilder = CapStyle.getCapBuilder(lineStyle.capStyle) ?: throw Exception("No cap builder defined for: ${lineStyle.capStyle}")

	capBuilder.createCap(p1, p2, controlA, primitive, lineStyle, controlAThickness, clockwise = true)
	val indexA = primitive.vertices.size - 2
	_assert(indexA >= 0, "Cap builder did not create at least two vertices")
	val indexB = indexA + 1
	capBuilder.createCap(p2, p1, controlB, primitive, lineStyle, controlBThickness, clockwise = false)
	val indexC = primitive.vertices.size - 2
	_assert(indexC >= indexA + 2, "Cap builder did not create at least two vertices")
	val indexD = indexC + 1

	// Span a rectangle from the ends the caps create.
	primitive.pushIndex(indexA)
	primitive.pushIndex(indexC)
	primitive.pushIndex(indexB)
	primitive.pushIndex(indexB)
	primitive.pushIndex(indexC)
	primitive.pushIndex(indexD)

	primitive.init()
	return primitive
}

fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, init: MeshData.() -> Unit = {}): MeshData {
	v1.set(x1, y1)
	v2.set(x2, y2)
	v3.set(x3, y3)
	return triangle(v1, v2, v3, init)
}

fun triangle(v1: Vector2, v2: Vector2, v3: Vector2, init: MeshData.() -> Unit = {}): MeshData {
	return meshData {
		if (fillStyle.isVisible) +fillTriangle(v1, v2, v3)
		if (lineStyle.isVisible) {
			+line(v1, v2, v3, v3)
			+line(v2, v3, v1, v1)
			+line(v3, v1, v2, v2)
		}
		init()
	}
}

fun quad(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, init: MeshData.() -> Unit = {}): MeshData {
	v1.set(x1, y1)
	v2.set(x2, y2)
	v3.set(x3, y3)
	v4.set(x4, y4)
	return quad(v1, v2, v3, v4, init)
}

fun rect(x: Float, y: Float, w: Float, h: Float, init: MeshData.() -> Unit = {}): MeshData {
	v1.set(x, y)
	v2.set(x + w, y)
	v3.set(x + w, y + h)
	v4.set(x, y + h)
	return quad(v1, v2, v3, v4, init)
}

fun quad(v1: Vector2, v2: Vector2, v3: Vector2, v4: Vector2, init: MeshData.() -> Unit = {}): MeshData {
	return meshData {
		if (fillStyle.isVisible)
			+fillQuad(v1, v2, v3, v4)
		if (lineStyle.isVisible) {
			+line(v1, v2, v4, v3)
			+line(v2, v3, v1, v4)
			+line(v3, v4, v2, v1)
			+line(v4, v1, v3, v2)
		}
		init()
	}
}

/**
 * Draws a circle.
 * @param radius The radius of the circle.
 * @param segments The number of segments for the circle.
 */
fun circle(radius: Float, segments: Int, init: MeshData.() -> Unit = {}): MeshData {
	return oval(radius, radius, segments, init)
}

/**
 * Draws an oval.
 * @param width The width of the oval before rotation.
 * @param height The height of the oval before rotation.
 * @param segments The number of segments for the oval.
 */
fun oval(width: Float, height: Float, segments: Int = 180, init: MeshData.() -> Unit = {}): MeshData {
	val m = meshData {
		val fill = fillStyle.isVisible

		val fillPrimitive: MeshData?
		if (fill) {
			fillPrimitive = +meshData()
			fillPrimitive.texture = fillStyle.texture
			fillPrimitive.pushVertex(Vector2.obtain(), fillStyle)
		} else {
			fillPrimitive = null
		}

		val thetaInc = PI2 / segments

		// To ensure the tangent is correct at the wrap point.
		calculateOvalPoint(PI2 - thetaInc * 3f, width, height, v4)
		calculateOvalPoint(PI2 - thetaInc * 2f, width, height, v3)
		calculateOvalPoint(PI2 - thetaInc, width, height, v2)

		for (i in 0..segments) {
			val theta = i.toFloat() / segments * PI2
			calculateOvalPoint(theta, width, height, v1)

			if (fillPrimitive != null) {
				fillPrimitive.pushVertex(v1, fillStyle)
				if (i > 0) {
					fillPrimitive.pushIndex(0)
					fillPrimitive.pushIndex(i)
					fillPrimitive.pushIndex(i + 1)
				}
			}
			if (i > 0) {
				if (lineStyle.isVisible)
					+line(v3, v2, v4, v1)
			}
			v4.set(v3)
			v3.set(v2)
			v2.set(v1)
		}
	}
	m.init()
	return m
}

fun calculateOvalPoint(theta: Float, width: Float, height: Float, out: Vector2) {
	out.set((MathUtils.cos(theta) + 1f) * width * 0.5f, (MathUtils.sin(theta) + 1f) * height * 0.5f)
}

fun curvedRect(w: Float, h: Float, corners: Corners, segments: Int = 30, init: MeshData.() -> Unit = {}): MeshData {
	val m = meshData {
		val topLeftX = fitSize(corners.topLeft.x, corners.topRight.x, w)
		val topLeftY = fitSize(corners.topLeft.y, corners.bottomLeft.y, h)
		val topRightX = fitSize(corners.topRight.x, corners.topLeft.x, w)
		val topRightY = fitSize(corners.topRight.y, corners.bottomRight.y, h)
		val bottomRightX = fitSize(corners.bottomRight.x, corners.bottomLeft.x, w)
		val bottomRightY = fitSize(corners.bottomRight.y, corners.topRight.y, h)
		val bottomLeftX = fitSize(corners.bottomLeft.x, corners.bottomRight.x, w)
		val bottomLeftY = fitSize(corners.bottomLeft.y, corners.topLeft.y, h)

		val colorTint = fillStyle.colorTint
		run {
			// Middle vertical strip
			val left = maxOf(topLeftX, bottomLeftX)
			val right = w - maxOf(topRightX, bottomRightX)
			if (right > left) {
				pushVertex(left, 0f, 0f, colorTint)
				pushVertex(right, 0f, 0f, colorTint)
				pushVertex(right, h, 0f, colorTint)
				pushVertex(left, h, 0f, colorTint)
				pushIndices(QUAD_INDICES)
			}
		}
		if (topLeftX > 0f || bottomLeftX > 0f) {
			// Left vertical strip
			val leftW = maxOf(topLeftX, bottomLeftX)
			pushVertex(0f, topLeftY, 0f, colorTint)
			pushVertex(leftW, topLeftY, 0f, colorTint)
			pushVertex(leftW, h - bottomLeftY, 0f, colorTint)
			pushVertex(0f, h - bottomLeftY, 0f, colorTint)
			pushIndices(QUAD_INDICES)
		}
		if (topRightX > 0f || bottomRightX > 0f) {
			// Right vertical strip
			val rightW = maxOf(topRightX, bottomRightX)
			pushVertex(w - rightW, topRightY, 0f, colorTint)
			pushVertex(w, topRightY, 0f, colorTint)
			pushVertex(w, h - bottomRightY, 0f, colorTint)
			pushVertex(w - rightW, h - bottomRightY, 0f, colorTint)
			pushIndices(QUAD_INDICES)
		}
		if (topLeftX < bottomLeftX) {
			val anchorX = topLeftX
			val anchorY = topLeftY
			pushVertex(anchorX, 0f, 0f, colorTint)
			pushVertex(maxOf(topLeftX, bottomLeftX), 0f, 0f, colorTint)
			pushVertex(maxOf(topLeftX, bottomLeftX), anchorY, 0f, colorTint)
			pushVertex(anchorX, anchorY, 0f, colorTint)
			pushIndices(QUAD_INDICES)
		} else if (topLeftX > bottomLeftX) {
			val anchorX = bottomLeftX
			val anchorY = h - bottomLeftY
			pushVertex(anchorX, anchorY, 0f, colorTint)
			pushVertex(maxOf(topLeftX, bottomLeftX), anchorY, 0f, colorTint)
			pushVertex(maxOf(topLeftX, bottomLeftX), h, 0f, colorTint)
			pushVertex(anchorX, h, 0f, colorTint)
			pushIndices(QUAD_INDICES)
		}
		if (topRightX < bottomRightX) {
			val anchorX = w - maxOf(topRightX, bottomRightX)
			val anchorY = topRightY
			pushVertex(anchorX, 0f, 0f, colorTint)
			pushVertex(w - topRightX, 0f, 0f, colorTint)
			pushVertex(w - topRightX, anchorY, 0f, colorTint)
			pushVertex(anchorX, anchorY, 0f, colorTint)
			pushIndices(QUAD_INDICES)
		} else if (topRightX > bottomRightX) {
			val anchorX = w - maxOf(topRightX, bottomRightX)
			val anchorY = h - bottomRightY
			pushVertex(anchorX, anchorY, 0f, colorTint)
			pushVertex(w - bottomRightX, anchorY, 0f, colorTint)
			pushVertex(w - bottomRightX, h, 0f, colorTint)
			pushVertex(anchorX, h, 0f, colorTint)
			pushIndices(QUAD_INDICES)
		}

		if (topLeftX > 0f && topLeftY > 0f) {
			val n = highestIndex + 1
			val anchorX = topLeftX
			val anchorY = topLeftY
			pushVertex(anchorX, anchorY, 0f, colorTint) // Anchor

			for (i in 0..segments) {
				val theta = PI * 0.5f * (i.toFloat() / segments)
				pushVertex(anchorX - MathUtils.cos(theta) * topLeftX, anchorY - MathUtils.sin(theta) * topLeftY, 0f, colorTint)
				if (i > 0) {
					pushIndex(n)
					pushIndex(n + i)
					pushIndex(n + i + 1)
				}
			}
		}

		if (topRightX > 0f && topRightY > 0f) {
			val n = highestIndex + 1
			val anchorX = w - topRightX
			val anchorY = topRightY
			pushVertex(anchorX, anchorY, 0f, colorTint) // Anchor

			for (i in 0..segments) {
				val theta = PI * 0.5f * (i.toFloat() / segments)
				pushVertex(anchorX + MathUtils.cos(theta) * topRightX, anchorY - MathUtils.sin(theta) * topRightY, 0f, colorTint)
				if (i > 0) {
					pushIndex(n)
					pushIndex(n + i)
					pushIndex(n + i + 1)
				}
			}
		}

		if (bottomRightX > 0f && bottomRightY > 0f) {
			val n = highestIndex + 1
			val anchorX = w - bottomRightX
			val anchorY = h - bottomRightY
			pushVertex(anchorX, anchorY, 0f, colorTint) // Anchor

			for (i in 0..segments) {
				val theta = PI * 0.5f * (i.toFloat() / segments)
				pushVertex(anchorX + MathUtils.cos(theta) * bottomRightX, anchorY + MathUtils.sin(theta) * bottomRightY, 0f, colorTint)
				if (i > 0) {
					pushIndex(n)
					pushIndex(n + i)
					pushIndex(n + i + 1)
				}
			}
		}

		if (bottomLeftX > 0f && bottomLeftY > 0f) {
			val n = highestIndex + 1
			val anchorX = bottomLeftX
			val anchorY = h - bottomLeftY
			pushVertex(anchorX, anchorY, 0f, colorTint) // Anchor

			for (i in 0..segments) {
				val theta = PI * 0.5f * (i.toFloat() / segments)
				pushVertex(anchorX - MathUtils.cos(theta) * bottomLeftX, anchorY + MathUtils.sin(theta) * bottomLeftY, 0f, colorTint)
				if (i > 0) {
					pushIndex(n)
					pushIndex(n + i)
					pushIndex(n + i + 1)
				}
			}
		}
	}
	m.init()
	return m
}

/**
 * Proportionally scales value to fit in max if `value + other > max`
 */
private fun fitSize(value: Float, other: Float, max: Float): Float {
	val v1 = if (value < 0f) 0f else value
	val v2 = if (other < 0f) 0f else other
	val total = v1 + v2
	if (total > max) {
		return v1 * max / total
	} else {
		return v1
	}
}