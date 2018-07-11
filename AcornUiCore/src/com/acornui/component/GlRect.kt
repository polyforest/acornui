/*
 * Copyright 2018 Poly Forest
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

package com.acornui.component

import com.acornui.component.drawing.*
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.gl.core.*
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

open class GlRect(
		owner: Owned
) : ContainerImpl(owner), Rect {

	final override val style = bind(BoxStyle())

	var segments = 40

	private val fill = staticMesh()
	private val gradient = staticMesh()
	private val stroke = staticMesh()
	private val fillC = addChild(staticMeshC {
		mesh = fill
		interactivityMode = InteractivityMode.NONE
	})
	private val gradientC = addChild(staticMeshC {
		mesh = gradient
		interactivityMode = InteractivityMode.NONE
	})
	private val strokeC = addChild(staticMeshC {
		mesh = stroke
		interactivityMode = InteractivityMode.NONE
	})

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val margin = style.margin
		val w = (explicitWidth ?: 0f) - margin.right - margin.left
		val h = (explicitHeight ?: 0f) - margin.top - margin.bottom
		fill.clear()
		stroke.clear()
		gradient.clear()
		if (w <= 0f || h <= 0f) return

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
			// If we have a linear gradient, fill with white; we will be using the fill as a mask inside draw.
			val tint = if (style.linearGradient == null) style.backgroundColor else Color.WHITE
			if (tint.a > 0f) {
				run {
					// Middle vertical strip
					val left = maxOf(topLeftX, bottomLeftX)
					val right = w - maxOf(topRightX, bottomRightX)
					if (right > left) {
						putVertex(left, 0f, 0f, colorTint = tint)
						putVertex(right, 0f, 0f, colorTint = tint)
						putVertex(right, h, 0f, colorTint = tint)
						putVertex(left, h, 0f, colorTint = tint)
						putIndices(QUAD_INDICES)
					}
				}
				if (topLeftX > 0f || bottomLeftX > 0f) {
					// Left vertical strip
					val leftW = maxOf(topLeftX, bottomLeftX)
					putVertex(0f, topLeftY, 0f, colorTint = tint)
					putVertex(leftW, topLeftY, 0f, colorTint = tint)
					putVertex(leftW, h - bottomLeftY, 0f, colorTint = tint)
					putVertex(0f, h - bottomLeftY, 0f, colorTint = tint)
					putIndices(QUAD_INDICES)
				}
				if (topRightX > 0f || bottomRightX > 0f) {
					// Right vertical strip
					val rightW = maxOf(topRightX, bottomRightX)
					putVertex(w - rightW, topRightY, 0f, colorTint = tint)
					putVertex(w, topRightY, 0f, colorTint = tint)
					putVertex(w, h - bottomRightY, 0f, colorTint = tint)
					putVertex(w - rightW, h - bottomRightY, 0f, colorTint = tint)
					putIndices(QUAD_INDICES)
				}
				if (topLeftX < bottomLeftX) {
					// Vertical slice to the right of top left corner
					val anchorX = topLeftX
					val anchorY = topLeftY
					putVertex(anchorX, 0f, 0f, colorTint = tint)
					putVertex(maxOf(topLeftX, bottomLeftX), 0f, 0f, colorTint = tint)
					putVertex(maxOf(topLeftX, bottomLeftX), anchorY, 0f, colorTint = tint)
					putVertex(anchorX, anchorY, 0f, colorTint = tint)
					putIndices(QUAD_INDICES)
				} else if (topLeftX > bottomLeftX) {
					// Vertical slice to the right of bottom left corner
					val anchorX = bottomLeftX
					val anchorY = h - bottomLeftY
					putVertex(anchorX, anchorY, 0f, colorTint = tint)
					putVertex(maxOf(topLeftX, bottomLeftX), anchorY, 0f, colorTint = tint)
					putVertex(maxOf(topLeftX, bottomLeftX), h, 0f, colorTint = tint)
					putVertex(anchorX, h, 0f, colorTint = tint)
					putIndices(QUAD_INDICES)
				}
				if (topRightX < bottomRightX) {
					// Vertical slice to the left of top right corner
					val anchorX = w - maxOf(topRightX, bottomRightX)
					val anchorY = topRightY
					putVertex(anchorX, 0f, 0f, colorTint = tint)
					putVertex(w - topRightX, 0f, 0f, colorTint = tint)
					putVertex(w - topRightX, anchorY, 0f, colorTint = tint)
					putVertex(anchorX, anchorY, 0f, colorTint = tint)
					putIndices(QUAD_INDICES)
				} else if (topRightX > bottomRightX) {
					// Vertical slice to the left of bottom right corner
					val anchorX = w - maxOf(topRightX, bottomRightX)
					val anchorY = h - bottomRightY
					putVertex(anchorX, anchorY, 0f, colorTint = tint)
					putVertex(w - bottomRightX, anchorY, 0f, colorTint = tint)
					putVertex(w - bottomRightX, h, 0f, colorTint = tint)
					putVertex(anchorX, h, 0f, colorTint = tint)
					putIndices(QUAD_INDICES)
				}

				if (topLeftX > 0f && topLeftY > 0f) {
					val n = highestIndex + 1
					val anchorX = topLeftX
					val anchorY = topLeftY
					putVertex(anchorX, anchorY, 0f, colorTint = tint) // Anchor

					for (i in 0..segments) {
						val theta = PI * 0.5f * (i.toFloat() / segments)
						putVertex(anchorX - cos(theta) * topLeftX, anchorY - sin(theta) * topLeftY, 0f, colorTint = tint)
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
					putVertex(anchorX, anchorY, 0f, colorTint = tint) // Anchor

					for (i in 0..segments) {
						val theta = PI * 0.5f * (i.toFloat() / segments)
						putVertex(anchorX + cos(theta) * topRightX, anchorY - sin(theta) * topRightY, 0f, colorTint = tint)
						if (i > 0) {
							putIndex(n)
							putIndex(n + i + 1)
							putIndex(n + i)
						}
					}
				}

				if (bottomRightX > 0f && bottomRightY > 0f) {
					val n = highestIndex + 1
					val anchorX = w - bottomRightX
					val anchorY = h - bottomRightY
					putVertex(anchorX, anchorY, 0f, colorTint = tint) // Anchor

					for (i in 0..segments) {
						val theta = PI * 0.5f * (i.toFloat() / segments)
						putVertex(anchorX + cos(theta) * bottomRightX, anchorY + sin(theta) * bottomRightY, 0f, colorTint = tint)
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
					putVertex(anchorX, anchorY, 0f, colorTint = tint) // Anchor

					for (i in 0..segments) {
						val theta = PI * 0.5f * (i.toFloat() / segments)
						putVertex(anchorX - cos(theta) * bottomLeftX, anchorY + sin(theta) * bottomLeftY, 0f, colorTint = tint)
						if (i > 0) {
							putIndex(n)
							putIndex(n + i + 1)
							putIndex(n + i)
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
					putVertex(left, 0f, 0f, colorTint = borderColor.top)
					putVertex(right, 0f, 0f, colorTint = borderColor.top)
					putVertex(w - innerTopRightX, topBorder, 0f, colorTint = borderColor.top)
					putVertex(innerTopLeftX, topBorder, 0f, colorTint = borderColor.top)
					putIndices(QUAD_INDICES)
				}
			}

			if (rightBorder > 0f && borderColor.right.a > 0f) {
				val top = topRightY
				val bottom = h - bottomRightY
				val right = w
				if (bottom > top) {
					putVertex(right, top, 0f, colorTint = borderColor.right)
					putVertex(right, bottom, 0f, colorTint = borderColor.right)
					putVertex(right - rightBorder, h - innerBottomRightY, 0f, colorTint = borderColor.right)
					putVertex(right - rightBorder, innerTopRightY, 0f, colorTint = borderColor.right)
					putIndices(QUAD_INDICES)
				}
			}

			if (bottomBorder > 0f && borderColor.bottom.a > 0f) {
				val left = bottomLeftX
				val right = w - bottomRightX
				val bottom = h
				if (right > left) {
					putVertex(right, bottom, 0f, colorTint = borderColor.bottom)
					putVertex(left, bottom, 0f, colorTint = borderColor.bottom)
					putVertex(innerBottomLeftX, bottom - bottomBorder, 0f, colorTint = borderColor.bottom)
					putVertex(w - innerBottomRightX, bottom - bottomBorder, 0f, colorTint = borderColor.bottom)
					putIndices(QUAD_INDICES)
				}
			}

			if (leftBorder > 0f && borderColor.left.a > 0f) {
				val top = topLeftY
				val bottom = h - bottomLeftY
				if (bottom > top) {
					putVertex(0f, bottom, 0f, colorTint = borderColor.left)
					putVertex(0f, top, 0f, colorTint = borderColor.left)
					putVertex(leftBorder, innerTopLeftY, 0f, colorTint = borderColor.left)
					putVertex(leftBorder, h - innerBottomLeftY, 0f, colorTint = borderColor.left)
					putIndices(QUAD_INDICES)
				}
			}

			borderCorner(0f, topLeftY, topLeftX, 0f, leftBorder, innerTopLeftY, innerTopLeftX, topBorder, borderColor.left, borderColor.top)
			borderCorner(w - rightBorder, innerTopRightY, w - innerTopRightX, topBorder, w, topRightY, w - topRightX, 0f, borderColor.right, borderColor.top)
			borderCorner(w, h - bottomRightY, w - bottomRightX, h, w - rightBorder, h - innerBottomRightY, w - innerBottomRightX, h - bottomBorder, borderColor.right, borderColor.bottom)
			borderCorner(leftBorder, h - innerBottomLeftY, innerBottomLeftX, h - bottomBorder, 0f, h - bottomLeftY, bottomLeftX, h, borderColor.left, borderColor.bottom)

			trn(margin.left, margin.top)
		}

		if (style.linearGradient != null) {
			val linearGradient = style.linearGradient!!
			gradient.buildMesh {
				val angle = linearGradient.getAngle(w, h) - PI * 0.5f
				val a = cos(angle) * w
				val b = sin(angle) * h
				val len = abs(a) + abs(b)
				val thickness = sqrt(w * w + h * h)

				var pixel = 0f
				var n = 2
				putVertex(0f, 0f, 0f, colorTint = linearGradient.colorStops[0].color)
				putVertex(0f, thickness, 0f, colorTint = linearGradient.colorStops[0].color)
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
						putVertex(pixel, 0f, 0f, colorTint = colorStop.color)
						putVertex(pixel, thickness, 0f, colorTint = colorStop.color)

						if (i > 0) {
							putIndex(n)
							putIndex(n + 1)
							putIndex(n - 1)
							putIndex(n - 1)
							putIndex(n - 2)
							putIndex(n)
						}
						n += 2
					}
				}

				if (pixel < len) {
					val lastColor = linearGradient.colorStops.last().color
					putVertex(len, 0f, 0f, colorTint = lastColor)
					putVertex(len, thickness, 0f, colorTint = lastColor)
					putIndex(n)
					putIndex(n + 1)
					putIndex(n - 1)
					putIndex(n - 1)
					putIndex(n - 2)
					putIndex(n)
				}

				transform(position = Vector3(margin.left + w * 0.5f, margin.top + h * 0.5f), rotation = Vector3(z = angle), origin = Vector3(len * 0.5f, thickness * 0.5f))
			}
		}
	}

	private fun MeshRegion.borderCorner(outerX1: Float, outerY1: Float, outerX2: Float, outerY2: Float, innerX1: Float, innerY1: Float, innerX2: Float, innerY2: Float, color1: ColorRo, color2: ColorRo) {
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
				putVertex(outerX2 - cos(theta) * outerW, outerY1 + sin(theta) * outerH, 0f, colorTint = color)
				putVertex(innerX2 - cos(theta) * innerW, innerY1 + sin(theta) * innerH, 0f, colorTint = color)
				if (i > 0) {
					putIndex(n)
					putIndex(n + 1)
					putIndex(n - 1)
					putIndex(n - 1)
					putIndex(n - 2)
					putIndex(n)
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
				if (fillC.visible)
					fillC.render(viewport)
			}) {
				if (gradientC.visible)
					gradientC.render(viewport)
				if (strokeC.visible)
					strokeC.render(viewport)
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
	return if (total > max) {
		v1 * max / total
	} else {
		v1
	}
}