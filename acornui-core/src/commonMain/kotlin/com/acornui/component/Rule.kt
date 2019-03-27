package com.acornui.component

import com.acornui.component.style.*
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.serialization.*

class Rule(owned: Owned, private val isVertical: Boolean) : ElementContainerImpl<UiComponent>(owned) {

	private val rect: Rect = +rect()

	val style = bind(RuleStyle())

	init {
		styleTags.add(Companion)
		if (isVertical) styleTags.add(VERTICAL_STYLE)
		else styleTags.add(HORIZONTAL_STYLE)

		watch(style) {
			rect.style.set(it)
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (isVertical) {
			rect.setSize(style.thickness, explicitHeight)
		} else {
			rect.setSize(explicitWidth, style.thickness)
		}
		out.set(rect.bounds)
	}

	companion object : StyleTag {
		val VERTICAL_STYLE = styleTag()
		val HORIZONTAL_STYLE = styleTag()
	}
}

class RuleStyle : BoxStyle() {

	override val type: StyleType<RuleStyle> = Companion

	var thickness by prop(2f)

	companion object : StyleType<RuleStyle> {
		override val extends: StyleType<*>? = BoxStyle
	}
}

object RuleStyleSerializer : To<RuleStyle>, From<RuleStyle> {

	override fun RuleStyle.write(writer: Writer) {
		BoxStyleSerializer.apply {
			write(writer)
		}
		writer.styleProperty(this, ::thickness)?.float(thickness)
	}

	override fun read(reader: Reader): RuleStyle {
		val ruleStyle = RuleStyle()
		BoxStyleSerializer.read(reader, ruleStyle)
		reader.contains(ruleStyle::thickness.name) { ruleStyle.thickness = it.float()!! }
		return ruleStyle
	}
}

fun Owned.hr(init: (@ComponentDslMarker Rule).()->Unit = {}): Rule {
	val c = Rule(this, isVertical = false)
	c.init()
	return c
}

fun Owned.vr(init: (@ComponentDslMarker Rule).()->Unit = {}): Rule {
	val c = Rule(this, isVertical = true)
	c.init()
	return c
}