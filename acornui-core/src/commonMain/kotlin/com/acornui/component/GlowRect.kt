package com.acornui.component

import com.acornui.component.layout.LayoutContainer
import com.acornui.component.layout.NoopLayoutData
import com.acornui.component.style.NoopStyle
import com.acornui.component.style.StyleType
import com.acornui.component.style.set
import com.acornui.component.style.styleTag
import com.acornui.di.Context
import com.acornui.filter.BlurQuality
import com.acornui.filter.GlowFilter
import com.acornui.filter.filtered
import com.acornui.filter.glowFilter
import com.acornui.math.ColorTransformation
import com.acornui.math.ColorTransformationRo
import com.acornui.math.Vector3
import com.acornui.math.Vector3Ro
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A Rect with a shadow.
 */
class GlowRect(owner: Context) : LayoutContainer<NoopStyle, NoopLayoutData>(owner, FillLayout()) {
	
	private val glowFilter: GlowFilter
	private val rect: Rect = rect()
	
	val style: GlowBoxStyle = bind(GlowBoxStyle())
	
	init {
		+filtered {
			+rect
			glowFilter = +glowFilter()
		}
		
		watch(style) {
			rect.style.set(it)
			glowFilter.blurX = it.blurX
			glowFilter.blurY = it.blurY
			glowFilter.quality = it.quality
			glowFilter.offset = it.offset
			glowFilter.colorTransformation = it.colorTransform
		}
	}
}

class GlowBoxStyle : BoxStyle() {
	
	override val type = Companion

	var blurX by prop(3f)

	var blurY by prop(3f)

	var quality by prop(BlurQuality.NORMAL)

	var offset: Vector3Ro by prop(Vector3.ZERO)
	
	var colorTransform: ColorTransformationRo by prop(ColorTransformation())
	
	companion object : StyleType<GlowBoxStyle> {
		override val extends: StyleType<*>? = BoxStyle
	}
}

val shadowRectStyleTag = styleTag()

fun Context.shadowRect(init: ComponentInit<GlowRect>): GlowRect {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return GlowRect(this).apply {
		styleTags.add(shadowRectStyleTag)
		init()
	}
}