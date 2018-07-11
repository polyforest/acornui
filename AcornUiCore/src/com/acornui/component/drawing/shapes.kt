package com.acornui.component.drawing

import com.acornui._assert
import com.acornui.collection.Clearable
import com.acornui.component.drawing.MeshBuilderStyle.fillColor
import com.acornui.component.drawing.MeshBuilderStyle.lineStyle
import com.acornui.gl.core.putIndex
import com.acornui.gl.core.putIndices
import com.acornui.gl.core.putVertex
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*
import kotlin.math.cos
import kotlin.math.sin

private val v1 = Vector2()
private val v2 = Vector2()
private val v3 = Vector2()
private val v4 = Vector2()

val QUAD_INDICES = intArrayOf(0, 1, 2, 2, 3, 0)
val TRIANGLE_INDICES = intArrayOf(0, 1, 2)

fun MeshRegion.fillTriangle(v1: Vector2, v2: Vector2, v3: Vector2) {
	putVertex(v1, colorTint = fillColor)
	putVertex(v2, colorTint = fillColor)
	putVertex(v3, colorTint = fillColor)
	putIndices(TRIANGLE_INDICES)
}

/**
 * Fills a quadrilateral with the specified fill style.
 */
fun MeshRegion.fillQuad(v1: Vector2, v2: Vector2, v3: Vector2, v4: Vector2) {
	putVertex(v1, colorTint = fillColor)
	putVertex(v2, colorTint = fillColor)
	putVertex(v3, colorTint = fillColor)
	putVertex(v4, colorTint = fillColor)
	putIndices(QUAD_INDICES)
}

fun MeshRegion.line(x1: Float, y1: Float, x2: Float, y2: Float, controlA: Vector2? = null, controlB: Vector2? = null, controlAThickness: Float = lineStyle.thickness, controlBThickness: Float = lineStyle.thickness, init: MeshRegion.() -> Unit = {}) {
	val p1 = Vector2.obtain().set(x1, y1)
	val p2 = Vector2.obtain().set(x2, y2)
	val ret = line(p1, p2, controlA, controlB, controlAThickness, controlBThickness, init)
	Vector2.free(p1)
	Vector2.free(p2)
	return ret
}

fun MeshRegion.line(p1: Vector2, p2: Vector2, controlA: Vector2? = null, controlB: Vector2? = null, controlAThickness: Float = lineStyle.thickness, controlBThickness: Float = lineStyle.thickness, init: MeshRegion.() -> Unit = {}) {
	meshData {
		val capBuilder = CapStyle.getCapBuilder(lineStyle.capStyle)
				?: throw Exception("No cap builder defined for: ${lineStyle.capStyle}")
		capBuilder.createCap(p1, p2, controlA, this, lineStyle, controlAThickness, clockwise = true)
		val vertexSize = vertexAttributes.vertexSize
		val indexA = batch.vertexComponents.position / vertexSize - 2
		_assert(indexA >= startVertexPosition / vertexSize, "Cap builder did not create at least two vertices")
		val indexB = indexA + 1
		capBuilder.createCap(p2, p1, controlB, this, lineStyle, controlBThickness, clockwise = false)
		val indexC = batch.vertexComponents.position / vertexSize - 2
		_assert(indexC >= indexA + 2, "Cap builder did not create at least two vertices")
		val indexD = indexC + 1

		// Span a rectangle from the ends the caps create.
		putIndex(indexA)
		putIndex(indexC)
		putIndex(indexB)
		putIndex(indexB)
		putIndex(indexC)
		putIndex(indexD)

		init()
	}
}

fun MeshRegion.triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, init: MeshRegion.() -> Unit = {}) {
	v1.set(x1, y1)
	v2.set(x2, y2)
	v3.set(x3, y3)
	triangle(v1, v2, v3, init)
}

fun MeshRegion.triangle(v1: Vector2, v2: Vector2, v3: Vector2, init: MeshRegion.() -> Unit = {}) {
	meshData {
		if (fillColor.a > 0f) fillTriangle(v1, v2, v3)
		if (lineStyle.thickness > 0f) {
			line(v1, v2, v3, v3)
			line(v2, v3, v1, v1)
			line(v3, v1, v2, v2)
		}
		init()
	}
}

fun MeshRegion.quad(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, init: MeshRegion.() -> Unit = {}) {
	v1.set(x1, y1)
	v2.set(x2, y2)
	v3.set(x3, y3)
	v4.set(x4, y4)
	quad(v1, v2, v3, v4, init)
}

fun MeshRegion.rect(x: Float, y: Float, w: Float, h: Float, init: MeshRegion.() -> Unit = {}) {
	v1.set(x, y)
	v2.set(x + w, y)
	v3.set(x + w, y + h)
	v4.set(x, y + h)
	quad(v1, v2, v3, v4, init)
}

fun MeshRegion.quad(v1: Vector2, v2: Vector2, v3: Vector2, v4: Vector2, init: MeshRegion.() -> Unit = {}) {
	meshData {
		if (style.fillColor.a > 0f)
			fillQuad(v1, v2, v3, v4)
		if (style.lineStyle.thickness > 0f) {
			line(v1, v2, v4, v3)
			line(v2, v3, v1, v4)
			line(v3, v4, v2, v1)
			line(v4, v1, v3, v2)
		}
		init()
	}
}

/**
 * Draws a circle.
 * @param radius The radius of the circle.
 * @param segments The number of segments for the circle.
 */
fun MeshRegion.circle(radius: Float, segments: Int, init: MeshRegion.() -> Unit = {}) {
	oval(radius, radius, segments, init)
}

/**
 * Draws an oval.
 * @param width The width of the oval before rotation.
 * @param height The height of the oval before rotation.
 * @param segments The number of segments for the oval.
 */
fun MeshRegion.oval(width: Float, height: Float, segments: Int = 180, init: MeshRegion.() -> Unit = {}) = meshData {
	val shouldFill = style.fillColor.a > 0f
	val n = highestIndex + 1
	if (shouldFill) {
		putVertex(0f, 0f, 0f, colorTint = fillColor)
	}

	val thetaInc = PI2 / segments

	// To ensure the tangent is correct at the wrap point.
	calculateOvalPoint(PI2 - thetaInc * 3f, width, height, v4)
	calculateOvalPoint(PI2 - thetaInc * 2f, width, height, v3)
	calculateOvalPoint(PI2 - thetaInc, width, height, v2)

	for (i in 0..segments) {
		val theta = i.toFloat() / segments * PI2
		calculateOvalPoint(theta, width, height, v1)

		if (shouldFill) {
			putVertex(v1, colorTint = style.fillColor)
			if (i > 0) {
				putIndex(n)
				putIndex(n + i)
				putIndex(n + i + 1)
			}
		}
		if (i > 0) {
			if (style.lineStyle.thickness > 0f)
				line(v3, v2, v4, v1)
		}
		v4.set(v3)
		v3.set(v2)
		v2.set(v1)
	}
	init()
}

private fun calculateOvalPoint(theta: Float, width: Float, height: Float, out: Vector2) {
	out.set((cos(theta) + 1f) * width * 0.5f, (sin(theta) + 1f) * height * 0.5f)
}

fun MeshRegion.curvedRect(w: Float, h: Float, corners: CornersRo, segments: Int = 30, init: MeshRegion.() -> Unit = {}) = meshData {
	val topLeftX = fitSize(corners.topLeft.x, corners.topRight.x, w)
	val topLeftY = fitSize(corners.topLeft.y, corners.bottomLeft.y, h)
	val topRightX = fitSize(corners.topRight.x, corners.topLeft.x, w)
	val topRightY = fitSize(corners.topRight.y, corners.bottomRight.y, h)
	val bottomRightX = fitSize(corners.bottomRight.x, corners.bottomLeft.x, w)
	val bottomRightY = fitSize(corners.bottomRight.y, corners.topRight.y, h)
	val bottomLeftX = fitSize(corners.bottomLeft.x, corners.bottomRight.x, w)
	val bottomLeftY = fitSize(corners.bottomLeft.y, corners.topLeft.y, h)

	val colorTint = style.fillColor
	run {
		// Middle vertical strip
		val left = maxOf(topLeftX, bottomLeftX)
		val right = w - maxOf(topRightX, bottomRightX)
		if (right > left) {
			putVertex(left, 0f, 0f, colorTint = colorTint)
			putVertex(right, 0f, 0f, colorTint = colorTint)
			putVertex(right, h, 0f, colorTint = colorTint)
			putVertex(left, h, 0f, colorTint = colorTint)
			putIndices(QUAD_INDICES)
		}
	}
	if (topLeftX > 0f || bottomLeftX > 0f) {
		// Left vertical strip
		val leftW = maxOf(topLeftX, bottomLeftX)
		putVertex(0f, topLeftY, 0f, colorTint = colorTint)
		putVertex(leftW, topLeftY, 0f, colorTint = colorTint)
		putVertex(leftW, h - bottomLeftY, 0f, colorTint = colorTint)
		putVertex(0f, h - bottomLeftY, 0f, colorTint = colorTint)
		putIndices(QUAD_INDICES)
	}
	if (topRightX > 0f || bottomRightX > 0f) {
		// Right vertical strip
		val rightW = maxOf(topRightX, bottomRightX)
		putVertex(w - rightW, topRightY, 0f, colorTint = colorTint)
		putVertex(w, topRightY, 0f, colorTint = colorTint)
		putVertex(w, h - bottomRightY, 0f, colorTint = colorTint)
		putVertex(w - rightW, h - bottomRightY, 0f, colorTint = colorTint)
		putIndices(QUAD_INDICES)
	}
	if (topLeftX < bottomLeftX) {
		val anchorX = topLeftX
		val anchorY = topLeftY
		putVertex(anchorX, 0f, 0f, colorTint = colorTint)
		putVertex(maxOf(topLeftX, bottomLeftX), 0f, 0f, colorTint = colorTint)
		putVertex(maxOf(topLeftX, bottomLeftX), anchorY, 0f, colorTint = colorTint)
		putVertex(anchorX, anchorY, 0f, colorTint = colorTint)
		putIndices(QUAD_INDICES)
	} else if (topLeftX > bottomLeftX) {
		val anchorX = bottomLeftX
		val anchorY = h - bottomLeftY
		putVertex(anchorX, anchorY, 0f, colorTint = colorTint)
		putVertex(maxOf(topLeftX, bottomLeftX), anchorY, 0f, colorTint = colorTint)
		putVertex(maxOf(topLeftX, bottomLeftX), h, 0f, colorTint = colorTint)
		putVertex(anchorX, h, 0f, colorTint = colorTint)
		putIndices(QUAD_INDICES)
	}
	if (topRightX < bottomRightX) {
		val anchorX = w - maxOf(topRightX, bottomRightX)
		val anchorY = topRightY
		putVertex(anchorX, 0f, 0f, colorTint = colorTint)
		putVertex(w - topRightX, 0f, 0f, colorTint = colorTint)
		putVertex(w - topRightX, anchorY, 0f, colorTint = colorTint)
		putVertex(anchorX, anchorY, 0f, colorTint = colorTint)
		putIndices(QUAD_INDICES)
	} else if (topRightX > bottomRightX) {
		val anchorX = w - maxOf(topRightX, bottomRightX)
		val anchorY = h - bottomRightY
		putVertex(anchorX, anchorY, 0f, colorTint = colorTint)
		putVertex(w - bottomRightX, anchorY, 0f, colorTint = colorTint)
		putVertex(w - bottomRightX, h, 0f, colorTint = colorTint)
		putVertex(anchorX, h, 0f, colorTint = colorTint)
		putIndices(QUAD_INDICES)
	}

	if (topLeftX > 0f && topLeftY > 0f) {
		val n = highestIndex + 1
		val anchorX = topLeftX
		val anchorY = topLeftY
		putVertex(anchorX, anchorY, 0f, colorTint = colorTint) // Anchor

		for (i in 0..segments) {
			val theta = PI * 0.5f * (i.toFloat() / segments)
			putVertex(anchorX - cos(theta) * topLeftX, anchorY - sin(theta) * topLeftY, 0f, colorTint = colorTint)
			if (i > 0) {
				putIndex(n)
				putIndex(n + i)
				putIndex(n + i + 1)
			}
		}
	}

	if (topRightX > 0f && topRightY > 0f) {
		val n = highestIndex + 1
		val anchorX = w - topRightX
		val anchorY = topRightY
		putVertex(anchorX, anchorY, 0f, colorTint = colorTint) // Anchor

		for (i in 0..segments) {
			val theta = PI * 0.5f * (i.toFloat() / segments)
			putVertex(anchorX + cos(theta) * topRightX, anchorY - sin(theta) * topRightY, 0f, colorTint = colorTint)
			if (i > 0) {
				putIndex(n)
				putIndex(n + i)
				putIndex(n + i + 1)
			}
		}
	}

	if (bottomRightX > 0f && bottomRightY > 0f) {
		val n = highestIndex + 1
		val anchorX = w - bottomRightX
		val anchorY = h - bottomRightY
		putVertex(anchorX, anchorY, 0f, colorTint = colorTint) // Anchor

		for (i in 0..segments) {
			val theta = PI * 0.5f * (i.toFloat() / segments)
			putVertex(anchorX + cos(theta) * bottomRightX, anchorY + sin(theta) * bottomRightY, 0f, colorTint = colorTint)
			if (i > 0) {
				putIndex(n)
				putIndex(n + i)
				putIndex(n + i + 1)
			}
		}
	}

	if (bottomLeftX > 0f && bottomLeftY > 0f) {
		val n = highestIndex + 1
		val anchorX = bottomLeftX
		val anchorY = h - bottomLeftY
		putVertex(anchorX, anchorY, 0f, colorTint = colorTint) // Anchor

		for (i in 0..segments) {
			val theta = PI * 0.5f * (i.toFloat() / segments)
			putVertex(anchorX - cos(theta) * bottomLeftX, anchorY + sin(theta) * bottomLeftY, 0f, colorTint = colorTint)
			if (i > 0) {
				putIndex(n)
				putIndex(n + i)
				putIndex(n + i + 1)
			}
		}
	}
	init()
}

/**
 * Proportionally scales value to fit in max if `value + other > max`
 */
private fun fitSize(value: Float, other: Float, max: Float): Float {
	val v1 = if (value < 0f) 0f else value
	val v2 = if (other < 0f) 0f else other
	val total = v1 + v2
	return if (total > max) {
		v1 * max / total
	} else {
		v1
	}
}

private val style = MeshBuilderStyle

/**
 * An object representing the default styling attributes for shape drawing.
 */
object MeshBuilderStyle : Clearable {
	val lineStyle: LineStyle = LineStyle()
	var fillColor: ColorRo = Color.WHITE

	override fun clear() {
		lineStyle.clear()
		fillColor = Color.WHITE
	}
}