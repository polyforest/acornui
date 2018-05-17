package com.acornui.gl.component

import com.acornui.component.BoxStyle
import com.acornui.component.ContainerImpl
import com.acornui.component.InteractivityMode
import com.acornui.component.Rect
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.gl.component.drawing.MeshData
import com.acornui.gl.component.drawing.QUAD_INDICES
import com.acornui.gl.component.drawing.dynamicMeshC
import com.acornui.gl.component.drawing.transform
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

open class GlRect(
		owner: Owned
) : ContainerImpl(owner), Rect {

	final override val style = bind(BoxStyle())

	var segments = 40

	private val fill = addChild(dynamicMeshC())
	private val gradient = addChild(dynamicMeshC())
	private val stroke = addChild(dynamicMeshC())

	init {
		fill.interactivityMode = InteractivityMode.NONE
		gradient.interactivityMode = InteractivityMode.NONE
		stroke.interactivityMode = InteractivityMode.NONE
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val margin = style.margin
		val w = (explicitWidth ?: 0f) - margin.right - margin.left
		val h = (explicitHeight ?: 0f) - margin.top - margin.bottom
		if (w <= 0f || h <= 0f) {
			fill.clear()
			stroke.clear()
			return
		}

		val corners = style.borderRadius
		val topLeftX = fitSize(corners.topLeft.x, corners.topRight.x, w)
		val topLeftY = fitSize(corners.topLeft.y, corners.bottomLeft.y, h)
		val topRightX = fitSize(corners.topRight.x, corners.topLeft.x, w)
		val topRightY = fitSize(corners.topRight.y, corners.bottomRight.y, h)
		val bottomRightX = fitSize(corners.bottomRight.x, corners.bottomLeft.x, w)
		val bottomRightY = fitSize(corners.bottomRight.y, corners.topRight.y, h)
		val bottomLeftX = fitSize(corners.bottomLeft.x, corners.bottomRight.x, w)
		val bottomLeftY = fitSize(corners.bottomLeft.y, corners.topLeft.y, h)

		fill.buildMesh {
			val colorTint = if (style.linearGradient == null) style.backgroundColor else Color.WHITE
			if (colorTint.a > 0f) {
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
					// Vertical slice to the right of top left corner
					val anchorX = topLeftX
					val anchorY = topLeftY
					pushVertex(anchorX, 0f, 0f, colorTint)
					pushVertex(maxOf(topLeftX, bottomLeftX), 0f, 0f, colorTint)
					pushVertex(maxOf(topLeftX, bottomLeftX), anchorY, 0f, colorTint)
					pushVertex(anchorX, anchorY, 0f, colorTint)
					pushIndices(QUAD_INDICES)
				} else if (topLeftX > bottomLeftX) {
					// Vertical slice to the right of bottom left corner
					val anchorX = bottomLeftX
					val anchorY = h - bottomLeftY
					pushVertex(anchorX, anchorY, 0f, colorTint)
					pushVertex(maxOf(topLeftX, bottomLeftX), anchorY, 0f, colorTint)
					pushVertex(maxOf(topLeftX, bottomLeftX), h, 0f, colorTint)
					pushVertex(anchorX, h, 0f, colorTint)
					pushIndices(QUAD_INDICES)
				}
				if (topRightX < bottomRightX) {
					// Vertical slice to the left of top right corner
					val anchorX = w - maxOf(topRightX, bottomRightX)
					val anchorY = topRightY
					pushVertex(anchorX, 0f, 0f, colorTint)
					pushVertex(w - topRightX, 0f, 0f, colorTint)
					pushVertex(w - topRightX, anchorY, 0f, colorTint)
					pushVertex(anchorX, anchorY, 0f, colorTint)
					pushIndices(QUAD_INDICES)
				} else if (topRightX > bottomRightX) {
					// Vertical slice to the left of bottom right corner
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
						pushVertex(anchorX - cos(theta) * topLeftX, anchorY - sin(theta) * topLeftY, 0f, colorTint)
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
						pushVertex(anchorX + cos(theta) * topRightX, anchorY - sin(theta) * topRightY, 0f, colorTint)
						if (i > 0) {
							pushIndex(n)
							pushIndex(n + i + 1)
							pushIndex(n + i)
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
						pushVertex(anchorX + cos(theta) * bottomRightX, anchorY + sin(theta) * bottomRightY, 0f, colorTint)
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
						pushVertex(anchorX - cos(theta) * bottomLeftX, anchorY + sin(theta) * bottomLeftY, 0f, colorTint)
						if (i > 0) {
							pushIndex(n)
							pushIndex(n + i + 1)
							pushIndex(n + i)
						}
					}
				}
				trn(margin.left, margin.top)
			}
		}

		stroke.buildMesh {
			val borderColor = style.borderColor
			val border = style.borderThickness
			val topBorder = fitSize(border.top, border.bottom, h)
			val leftBorder = fitSize(border.left, border.right, w)
			val rightBorder = fitSize(border.right, border.left, w)
			val bottomBorder = fitSize(border.bottom, border.top, h)
			val innerTopLeftX = maxOf(topLeftX, leftBorder)
			val innerTopLeftY = maxOf(topLeftY, topBorder)
			val innerTopRightX = maxOf(topRightX, rightBorder)
			val innerTopRightY = maxOf(topRightY, topBorder)
			val innerBottomRightX = maxOf(bottomRightX, rightBorder)
			val innerBottomRightY = maxOf(bottomRightY, bottomBorder)
			val innerBottomLeftX = maxOf(bottomLeftX, leftBorder)
			val innerBottomLeftY = maxOf(bottomLeftY, bottomBorder)

			if (topBorder > 0f && borderColor.top.a > 0f) {
				val left = topLeftX
				val right = w - topRightX
				if (right > left) {
					pushVertex(left, 0f, 0f, borderColor.top)
					pushVertex(right, 0f, 0f, borderColor.top)
					pushVertex(w - innerTopRightX, topBorder, 0f, borderColor.top)
					pushVertex(innerTopLeftX, topBorder, 0f, borderColor.top)
					pushIndices(QUAD_INDICES)
				}
			}

			if (rightBorder > 0f && borderColor.right.a > 0f) {
				val top = topRightY
				val bottom = h - bottomRightY
				val right = w
				if (bottom > top) {
					pushVertex(right, top, 0f, borderColor.right)
					pushVertex(right, bottom, 0f, borderColor.right)
					pushVertex(right - rightBorder, h - innerBottomRightY, 0f, borderColor.right)
					pushVertex(right - rightBorder, innerTopRightY, 0f, borderColor.right)
					pushIndices(QUAD_INDICES)
				}
			}

			if (bottomBorder > 0f && borderColor.bottom.a > 0f) {
				val left = bottomLeftX
				val right = w - bottomRightX
				val bottom = h
				if (right > left) {
					pushVertex(right, bottom, 0f, borderColor.bottom)
					pushVertex(left, bottom, 0f, borderColor.bottom)
					pushVertex(innerBottomLeftX, bottom - bottomBorder, 0f, borderColor.bottom)
					pushVertex(w - innerBottomRightX, bottom - bottomBorder, 0f, borderColor.bottom)
					pushIndices(QUAD_INDICES)
				}
			}

			if (leftBorder > 0f && borderColor.left.a > 0f) {
				val top = topLeftY
				val bottom = h - bottomLeftY
				if (bottom > top) {
					pushVertex(0f, bottom, 0f, borderColor.left)
					pushVertex(0f, top, 0f, borderColor.left)
					pushVertex(leftBorder, innerTopLeftY, 0f, borderColor.left)
					pushVertex(leftBorder, h - innerBottomLeftY, 0f, borderColor.left)
					pushIndices(QUAD_INDICES)
				}
			}

			borderCorner(0f, topLeftY, topLeftX, 0f, leftBorder, innerTopLeftY, innerTopLeftX, topBorder, borderColor.left, borderColor.top)
			borderCorner(w - rightBorder, innerTopRightY, w - innerTopRightX, topBorder, w, topRightY, w - topRightX, 0f, borderColor.right, borderColor.top)
			borderCorner(w, h - bottomRightY, w - bottomRightX, h, w - rightBorder, h - innerBottomRightY, w - innerBottomRightX, h - bottomBorder, borderColor.right, borderColor.bottom)
			borderCorner(leftBorder, h - innerBottomLeftY, innerBottomLeftX, h - bottomBorder, 0f, h - bottomLeftY, bottomLeftX, h, borderColor.left, borderColor.bottom)

			trn(margin.left, margin.top)
		}

		gradient.clear()
		if (style.linearGradient != null) {
			val linearGradient = style.linearGradient!!
			gradient.buildMesh {
				val angle = linearGradient.getAngle(w, h) - PI * 0.5f
				val a = cos(angle) * w
				val b = sin(angle) * h
				val len = abs(a) + abs(b)
				val thickness = MathUtils.sqrt(w * w + h * h)

				var pixel = 0f
				var n = 2
				pushVertex(0f, 0f, 0f, linearGradient.colorStops[0].color)
				pushVertex(0f, thickness, 0f, linearGradient.colorStops[0].color)
				val numColorStops = linearGradient.colorStops.size
				for (i in 0..numColorStops - 1) {
					val colorStop = linearGradient.colorStops[i]

					if (colorStop.percent != null) {
						pixel = maxOf(pixel, colorStop.percent!! * len)
					} else if (colorStop.pixels != null) {
						pixel = maxOf(pixel, colorStop.pixels!!)
					} else if (i == numColorStops - 1) {
						pixel = len
					} else if (i > 0) {
						var nextKnownPixel = len
						var nextKnownJ = numColorStops - 1
						for (j in (i + 1)..linearGradient.colorStops.lastIndex) {
							val jColorStop = linearGradient.colorStops[j]
							if (jColorStop.percent != null) {
								nextKnownJ = j
								nextKnownPixel = maxOf(pixel, jColorStop.percent!! * len)
								break
							} else if (jColorStop.pixels != null) {
								nextKnownJ = j
								nextKnownPixel = maxOf(pixel, jColorStop.pixels!!)
								break
							}
						}
						pixel += (nextKnownPixel - pixel) / (1f + nextKnownJ.toFloat() - i.toFloat())
					}
					if (pixel > 0f) {
						pushVertex(pixel, 0f, 0f, colorStop.color)
						pushVertex(pixel, thickness, 0f, colorStop.color)

						if (i > 0) {
							pushIndex(n)
							pushIndex(n + 1)
							pushIndex(n - 1)
							pushIndex(n - 1)
							pushIndex(n - 2)
							pushIndex(n)
						}
						n += 2
					}
				}

				if (pixel < len) {
					val lastColor = linearGradient.colorStops.last().color
					pushVertex(len, 0f, 0f, lastColor)
					pushVertex(len, thickness, 0f, lastColor)
					pushIndex(n)
					pushIndex(n + 1)
					pushIndex(n - 1)
					pushIndex(n - 1)
					pushIndex(n - 2)
					pushIndex(n)
				}

				transform(position = Vector3(margin.left + w * 0.5f, margin.top + h * 0.5f), rotation = Vector3(z = angle), origin = Vector3(len * 0.5f, thickness * 0.5f))
			}
		}
	}

	private fun MeshData.borderCorner(outerX1: Float, outerY1: Float, outerX2: Float, outerY2: Float, innerX1: Float, innerY1: Float, innerX2: Float, innerY2: Float, color1: ColorRo, color2: ColorRo) {
		if (color1.a <= 0f && color2.a <= 0f) return
		val outerW = outerX2 - outerX1
		val outerH = outerY2 - outerY1
		val innerW = innerX2 - innerX1
		val innerH = innerY2 - innerY1
		if (outerW != 0f || outerH != 0f || innerW != 0f || innerH != 0f) {
			var n = highestIndex + 1
			for (i in 0..segments) {
				val theta = PI * 0.5f * (i.toFloat() / segments)
				val color = if (i < segments shr 1) color1 else color2
				pushVertex(outerX2 - cos(theta) * outerW, outerY1 + sin(theta) * outerH, 0f, color)
				pushVertex(innerX2 - cos(theta) * innerW, innerY1 + sin(theta) * innerH, 0f, color)
				if (i > 0) {
					pushIndex(n)
					pushIndex(n + 1)
					pushIndex(n - 1)
					pushIndex(n - 1)
					pushIndex(n - 2)
					pushIndex(n)
				}
				n += 2
			}
		}
	}

	private val glState = inject(GlState)
	private val gl = inject(Gl20)

	override fun draw(viewport: MinMaxRo) {
		if (style.linearGradient != null) {
			StencilUtil.mask(glState.batch, gl, {
				if (fill.visible) fill.render(viewport)
			}) {
				if (gradient.visible)
					gradient.render(viewport)
				if (stroke.visible)
					stroke.render(viewport)
			}
		} else {
			super.draw(viewport)
		}
	}
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