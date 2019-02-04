package com.acornui.component

import com.acornui.core.di.Owned

interface Rect : UiComponent {

	val style: BoxStyle
}

fun Owned.rect(init: ComponentInit<GlRect> = {}): GlRect {
	val r = GlRect(this)
	r.init()
	return r
}