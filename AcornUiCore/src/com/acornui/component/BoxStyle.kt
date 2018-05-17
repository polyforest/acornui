package com.acornui.component

import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleProperty
import com.acornui.core.radToDeg
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.graphics.color
import com.acornui.math.*
import com.acornui.serialization.*
import kotlin.math.atan2

open class BoxStyle : StyleBase() {

	override val type: StyleType<BoxStyle> = Companion

	var linearGradient: LinearGradientRo? by prop<LinearGradientRo?>(null)
	var backgroundColor: ColorRo by prop(Color.CLEAR)
	var borderColor: BorderColorsRo by prop(BorderColors())
	var borderThickness: PadRo by prop(Pad())
	var borderRadius: CornersRo by prop(Corners())
	var margin: PadRo by prop(Pad())
	var padding: PadRo by prop(Pad())

	companion object : StyleType<BoxStyle>
}

object BoxStyleSerializer : To<BoxStyle>, From<BoxStyle> {

	override fun BoxStyle.write(writer: Writer) {
		writer.styleProperty(this, "linearGradient")?.obj(linearGradient, LinearGradientSerializer)
		writer.styleProperty(this, "backgroundColor")?.color(backgroundColor)
		writer.styleProperty(this, "borderColor")?.obj(borderColor, BorderColorsSerializer)
		writer.styleProperty(this, "borderThickness")?.obj(borderThickness, PadSerializer)
		writer.styleProperty(this, "borderRadius")?.obj(borderRadius, CornersSerializer)
		writer.styleProperty(this, "margin")?.obj(margin, PadSerializer)
	}

	override fun read(reader: Reader): BoxStyle {
		val boxStyle = BoxStyle()
		read(reader, boxStyle)
		return boxStyle
	}

	fun read(reader: Reader, boxStyle: BoxStyle) {
		reader.contains("linearGradient") { boxStyle.linearGradient = it.obj(LinearGradientSerializer) }
		reader.contains("backgroundColor") { boxStyle.backgroundColor = it.color()!! }
		reader.contains("borderColor") { boxStyle.borderColor = it.obj(BorderColorsSerializer)!! }
		reader.contains("borderThickness") { boxStyle.borderThickness = it.obj(PadSerializer)!! }
		reader.contains("borderRadius") { boxStyle.borderRadius = it.obj(CornersSerializer)!! }
		reader.contains("margin") { boxStyle.margin = it.obj(PadSerializer)!! }
	}
}

fun boxStyle(init: BoxStyle.() -> Unit): BoxStyle {
	val b = BoxStyle()
	b.init()
	return b
}

/**
 * Creates a linear gradient where the color stops are distributed evenly for the given colors.
 */
fun LinearGradient(direction: GradientDirection, vararg colors: ColorRo): LinearGradient {
	val colorStops: Array<ColorStopRo> = Array(colors.size, { ColorStop(colors[it]) })
	return LinearGradient(direction, 0f, colorStops.toMutableList())
}

fun LinearGradient(direction: GradientDirection, vararg colorStops: ColorStopRo): LinearGradient = LinearGradient(direction, 0f, colorStops.toMutableList())
fun LinearGradient(angle: Float, vararg colorStops: ColorStopRo): LinearGradient = LinearGradient(GradientDirection.ANGLE, angle, colorStops.toMutableList())

/**
 * A read-only interface to [LinearGradient]
 */
interface LinearGradientRo {

	/**
	 * The direction the gradient should go. If direction is [GradientDirection.ANGLE], then the [angle] property
	 * will be used.
	 */
	val direction: GradientDirection

	/**
	 * In radians, angles start pointing towards top, rotating clockwise.
	 * PI / 2 is pointing to the right, PI is pointing to bottom, 3 / 2f * PI is pointing to right.
	 */
	val angle: Float

	val colorStops: List<ColorStopRo>

	fun toCssString(): String {
		val angleStr = when (direction) {
			GradientDirection.TOP_LEFT -> "to left top"
			GradientDirection.TOP -> "to top"
			GradientDirection.TOP_RIGHT -> "to top right"
			GradientDirection.RIGHT -> "to right"
			GradientDirection.BOTTOM_RIGHT -> "to bottom right"
			GradientDirection.BOTTOM -> "to bottom"
			GradientDirection.BOTTOM_LEFT -> "to bottom left"
			GradientDirection.LEFT -> "to left"
			GradientDirection.ANGLE -> "${angle.radToDeg()}deg"
		}
		var colorStopsStr = ""
		for (colorStop in colorStops) {
			if (colorStopsStr != "") colorStopsStr += ", "
			colorStopsStr += colorStop.toCssString()
		}
		return "linear-gradient($angleStr, $colorStopsStr)"
	}


	fun getAngle(width: Float, height: Float): Float {
		if (direction == GradientDirection.ANGLE) return angle
		else return direction.getAngle(width, height)
	}
}

data class LinearGradient(

		override var direction: GradientDirection,

		override var angle: Float,

		override val colorStops: MutableList<ColorStopRo>
) : LinearGradientRo

object LinearGradientSerializer : To<LinearGradientRo>, From<LinearGradient> {

	override fun read(reader: Reader): LinearGradient {
		val b = LinearGradient(
				direction = GradientDirection.valueOf(reader.string("direction")!!),
				angle = reader.float("angle")!!,
				colorStops = reader.array2("colorStops", ColorStopSerializer)!!.toMutableList()
		)
		return b
	}

	override fun LinearGradientRo.write(writer: Writer) {
		writer.string("direction", direction.name)
		writer.float("angle", angle)
		writer.array("colorStops", colorStops, ColorStopSerializer)
	}
}

enum class GradientDirection {
	TOP_LEFT,
	TOP,
	TOP_RIGHT,
	RIGHT,
	BOTTOM_RIGHT,
	BOTTOM,
	BOTTOM_LEFT,
	LEFT,
	ANGLE;

	fun getAngle(width: Float, height: Float): Float {
		return when (this) {
			TOP_LEFT -> atan2(-height, width)
			TOP -> 0f
			TOP_RIGHT -> atan2(height, width)
			RIGHT -> 0.5f * PI
			BOTTOM_RIGHT -> atan2(height, -width)
			BOTTOM -> PI
			BOTTOM_LEFT -> atan2(-height, -width)
			LEFT -> 1.5f * PI
			ANGLE -> 0f
		}
	}
}

/**
 * A read only interface to [ColorStop]
 */
interface ColorStopRo {

	/**
	 * The color value at this stop.
	 */
	val color: ColorRo

	/**
	 * The percent towards the end position of the gradient. This number is absolute, not relative to
	 * the previous color stop.
	 * If neither [pixels] or [percent] is set, this stop will be halfway between the previous stop and the next.
	 */
	val percent: Float?

	/**
	 * The number of pixels towards the end position of the gradient. This number is absolute, not relative to
	 * the previous color stop.
	 * If neither [pixels] or [percent] is set, this stop will be halfway between the previous stop and the next.
	 */
	val pixels: Float?

	fun toCssString(): String {
		var str = color.toCssString()
		if (percent != null) str += " ${percent!! * 100f}%"
		else if (pixels != null) str += " ${pixels}px"
		return str
	}
}

data class ColorStop(

		/**
		 * The color value at this stop.
		 */
		override var color: ColorRo,

		/**
		 * The percent towards the end position of the gradient. This number is absolute, not relative to
		 * the previous color stop.
		 * If neither [pixels] or [percent] is set, this stop will be halfway between the previous stop and the next.
		 */
		override var percent: Float? = null,

		/**
		 * The number of pixels towards the end position of the gradient. This number is absolute, not relative to
		 * the previous color stop.
		 * If neither [pixels] or [percent] is set, this stop will be halfway between the previous stop and the next.
		 */
		override var pixels: Float? = null
) : ColorStopRo {

	fun set(other: ColorStopRo): ColorStop {
		color = other.color
		percent = other.percent
		pixels = other.pixels
		return this
	}

}

object ColorStopSerializer : To<ColorStopRo>, From<ColorStop> {

	override fun read(reader: Reader): ColorStop {
		val c = ColorStop(
				color = reader.color("color")!!,
				percent = reader.float("percent"),
				pixels = reader.float("pixels")
		)
		return c
	}

	override fun ColorStopRo.write(writer: Writer) {
		writer.color("color", color)
		if (percent != null) writer.float("percent", percent)
		if (pixels != null) writer.float("pixels", pixels)
	}
}

interface BorderColorsRo {
	val top: ColorRo
	val right: ColorRo
	val bottom: ColorRo
	val left: ColorRo

	fun copy(): BorderColors {
		return BorderColors(top.copy(), right.copy(), bottom.copy(), left.copy())
	}
}

data class BorderColors(
		override val top: Color,
		override val right: Color,
		override val bottom: Color,
		override val left: Color
) : BorderColorsRo {

	constructor(all: ColorRo) : this(all.copy(), all.copy(), all.copy(), all.copy()) {
		set(all)
	}

	constructor() : this(Color.CLEAR)

	fun set(all: ColorRo): BorderColors {
		top.set(all)
		right.set(all)
		bottom.set(all)
		left.set(all)
		return this
	}

	fun set(other: BorderColors): BorderColors {
		top.set(other.top)
		right.set(other.right)
		bottom.set(other.bottom)
		left.set(other.left)
		return this
	}
}

object BorderColorsSerializer : To<BorderColorsRo>, From<BorderColors> {
	override fun BorderColorsRo.write(writer: Writer) {
		writer.color("top", top)
		writer.color("right", right)
		writer.color("bottom", bottom)
		writer.color("left", left)
	}

	override fun read(reader: Reader): BorderColors {
		return BorderColors(
				reader.color("top")!!,
				reader.color("right")!!,
				reader.color("bottom")!!,
				reader.color("left")!!
		)
	}
}